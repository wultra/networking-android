/*
 * Copyright 2022 Wultra s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.wultra.android.powerauth.networking

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.wultra.android.powerauth.networking.data.BaseRequest
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.error.ApiError
import com.wultra.android.powerauth.networking.error.ApiHttpException
import com.wultra.android.powerauth.networking.error.ErrorResponse
import com.wultra.android.powerauth.networking.log.WPNLogger
import com.wultra.android.powerauth.networking.processing.GsonRequestBodyBytes
import com.wultra.android.powerauth.networking.processing.GsonResponseBodyConverter
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import com.wultra.android.powerauth.networking.tokens.TokenManager
import com.wultra.android.powerauth.networking.utils.AppUtils
import com.wultra.android.powerauth.networking.utils.ConnectionMonitor
import com.wultra.android.powerauth.networking.utils.getCurrentLocale
import com.wultra.android.powerauth.BuildConfig
import io.getlime.security.powerauth.core.CoreEncryptedResponse
import io.getlime.security.powerauth.core.CoreEncryptor
import io.getlime.security.powerauth.networking.response.IGetTokenListener
import io.getlime.security.powerauth.networking.response.IGenerateTokenHeaderListener
import io.getlime.security.powerauth.networking.response.IGetEncryptorListener
import io.getlime.security.powerauth.networking.response.ITimeSynchronizationListener
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthHttpHeader
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import io.getlime.security.powerauth.sdk.PowerAuthToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

interface IApiCallResponseListener<T> {
    fun onSuccess(result: T)
    fun onFailure(error: ApiError)
}

/**
 * Common API methods for making request via OkHttp and (de)serializing data via Gson.
 *
 * @param baseUrl Base url for all requests
 * @param okHttpClient Configured Http Client
 * @param powerAuthSDK Power Auth instance for request authentication
 * @param gsonBuilder Builder that will be used for request/response (de)serialization.
 * @param appContext Application context. The library internally uses [Context.applicationContext]
 * to avoid holding a reference to an Activity or other short-lived context.
 * @param userAgent Default user agent for each request. Note that such value might be "overridden"
 * on per-request basis. Default value is `libraryDefault`.
 */
abstract class Api(
    private val baseUrl: String,
    okHttpClient: OkHttpClient,
    private val powerAuthSDK: PowerAuthSDK,
    private val gsonBuilder: GsonBuilder,
    appContext: Context,
    private val userAgent: UserAgent = UserAgent.libraryDefault(appContext)
) {

    private val appContext: Context = appContext.applicationContext

    /**
     * Language sent in request header. Default value is "en".
     */
    @Volatile
    var acceptLanguage = "en"

    /**
     * Strategy used to dispatch [EndpointAuthenticated] requests.
     * Default value is [RequestConcurrencyStrategy.SERIAL_AUTHENTICATED].
     */
    @Volatile
    var concurrencyStrategy = RequestConcurrencyStrategy.SERIAL_AUTHENTICATED

    private val okHttpClient: OkHttpClient

    // DEPRECATED: retained for binary compatibility with previously inlined token posts.
    @PublishedApi
    @Deprecated(
        "Token providers are ignored and will be removed in the next major version.",
        level = DeprecationLevel.WARNING
    )
    @Suppress("DEPRECATION")
    internal val tokenProvider: IPowerAuthTokenProvider = TokenManager(appContext, powerAuthSDK.tokenStore)

    init {
        val builder = okHttpClient.newBuilder()
        WPNLogger.configure(builder)
        this.okHttpClient = builder.build()
    }

    // PUBLIC API

    fun <TRequestData: BaseRequest, TResponseData: StatusResponse> post(
        data: TRequestData,
        endpoint: EndpointBasic<TRequestData, TResponseData>,
        headers: HashMap<String, String>? = null,
        okHttpInterceptor: OkHttpBuilderInterceptor? = null,
        listener: IApiCallResponseListener<TResponseData>
    ) {
        makeCall(getBodyBytes(data), endpoint, HashMap(headers.orEmpty()), okHttpInterceptor, listener)
    }

    fun <TRequestData: BaseRequest, TResponseData: StatusResponse> post(
        data: TRequestData,
        endpoint: EndpointAuthenticated<TRequestData, TResponseData>,
        authentication: PowerAuthAuthentication,
        headers: HashMap<String, String>? = null,
        okHttpInterceptor: OkHttpBuilderInterceptor? = null,
        listener: IApiCallResponseListener<TResponseData>
    ) {
        when (concurrencyStrategy) {
            RequestConcurrencyStrategy.CONCURRENT_ALL ->
                buildAuthenticatedHeaders(data, endpoint, authentication, headers)
                    .onFailure {
                        listener.onFailure(ApiError(it))
                    }.onSuccess { (bodyBytes, newHeaders) ->
                        makeCall(bodyBytes, endpoint, newHeaders, okHttpInterceptor, listener)
                    }

            RequestConcurrencyStrategy.SERIAL_AUTHENTICATED -> {
                val runnable = Runnable {
                    buildAuthenticatedHeaders(data, endpoint, authentication, headers)
                        .onFailure {
                            listener.onFailure(ApiError(it))
                        }.onSuccess { (bodyBytes, newHeaders) ->
                            makeBlockingCall(bodyBytes, endpoint, newHeaders, okHttpInterceptor, listener)
                        }
                }
                try {
                    powerAuthSDK.getSerialExecutor().execute(runnable)
                } catch (e: Exception) {
                    // e.g. missing activation, or the executor rejected the task
                    listener.onFailure(ApiError(e))
                }
            }
        }
    }

    fun <TRequestData: BaseRequest, TResponseData: StatusResponse> post(
        data: TRequestData,
        endpoint: EndpointAuthenticatedWithToken<TRequestData, TResponseData>,
        headers: HashMap<String, String>? = null,
        okHttpInterceptor: OkHttpBuilderInterceptor? = null,
        listener: IApiCallResponseListener<TResponseData>
    ) {
        obtainTokenAuthenticationHeader(endpoint.tokenName) { result ->
            result.onFailure {
                listener.onFailure(ApiError(it))
            }.onSuccess { tokenHeader ->
                val bodyBytes = getBodyBytes(data)
                val newHeaders = HashMap(headers.orEmpty())
                newHeaders[tokenHeader.key] = tokenHeader.value
                makeCall(bodyBytes, endpoint, newHeaders, okHttpInterceptor, listener)
            }
        }
    }

    // PRIVATE API

    @PublishedApi
    internal fun obtainTokenAuthenticationHeader(
        tokenName: String,
        completion: (Result<PowerAuthHttpHeader>) -> Unit
    ) {
        try {
            powerAuthSDK.tokenStore.requestAccessToken(
                appContext,
                tokenName,
                PowerAuthAuthentication.possession(),
                object : IGetTokenListener {
                    override fun onGetTokenSucceeded(token: PowerAuthToken) {
                        try {
                            powerAuthSDK.tokenStore.generateAuthenticationHeader(
                                appContext,
                                tokenName,
                                object : IGenerateTokenHeaderListener {
                                    override fun onGenerateTokenHeaderSucceeded(header: PowerAuthHttpHeader) {
                                        completion(Result.success(header))
                                    }

                                    override fun onGenerateTokenHeaderFailed(t: Throwable) {
                                        completion(Result.failure(t))
                                    }
                                }
                            )
                        } catch (t: Throwable) {
                            completion(Result.failure(t))
                        }
                    }

                    override fun onGetTokenFailed(t: Throwable) {
                        completion(Result.failure(t))
                    }
                }
            )
        } catch (t: Throwable) {
            completion(Result.failure(t))
        }
    }

    private fun getBodyBytes(data: BaseRequest): ByteArray {
        val requestGson = gsonBuilder.create()
        val requestTypeAdapter = getTypeAdapter(requestGson, data.javaClass)
        return GsonRequestBodyBytes(requestGson, requestTypeAdapter).convert(data)
    }

    private fun <TRequestData: BaseRequest> buildAuthenticatedHeaders(
        data: TRequestData,
        endpoint: EndpointAuthenticated<TRequestData, *>,
        authentication: PowerAuthAuthentication,
        headers: HashMap<String, String>?
    ): Result<Pair<ByteArray, HashMap<String, String>>> {
        val bodyBytes = getBodyBytes(data)
        val newHeaders = HashMap(headers.orEmpty())
        return try {
            val authorizationHeader = powerAuthSDK.authenticationHeaderForRequestWithBody(
                authentication,
                "POST",
                endpoint.uriId,
                bodyBytes
            )
            newHeaders[authorizationHeader.key] = authorizationHeader.value
            Result.success(bodyBytes to newHeaders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class PreparedCall(val request: Request, val client: OkHttpClient)

    private fun buildRequest(
        bodyBytes: ByteArray,
        endpoint: Endpoint<*, *>,
        headers: HashMap<String, String>,
        okHttpInterceptor: OkHttpBuilderInterceptor?,
        encryptor: CoreEncryptor?
    ): PreparedCall {
        var bytes = bodyBytes

        if (encryptor != null) {
            val encryptedRequest = encryptor.encryptRequest(bytes)
            bytes = encryptedRequest.requestBody
            if (endpoint is EndpointBasic || endpoint is EndpointAuthenticatedWithToken) {
                encryptedRequest.requestHeaders.forEach { header ->
                    headers[header.key] = header.value
                }
            }
        }

        val body = bytes.toRequestBody(
            "application/json; charset=UTF-8".toMediaTypeOrNull(),
            0,
            bytes.size
        )

        val requestBuilder = Request.Builder()
            .url("${baseUrl.removeSuffix("/")}/${endpoint.endpointUrlPath.removePrefix("/")}")
            .post(body)
            .header("Accept-Language", acceptLanguage)

        userAgent.value?.let { requestBuilder.header("User-Agent", it) }

        headers.forEach { requestBuilder.header(it.key, it.value) }

        val request = requestBuilder.build()
        val client = if (okHttpInterceptor != null) {
            val builder = okHttpClient.newBuilder()
            okHttpInterceptor.intercept(builder)
            builder.build()
        } else {
            okHttpClient
        }
        return PreparedCall(request, client)
    }

    private fun <TResponseData: StatusResponse> handleResponse(
        response: Response,
        request: Request,
        encryptor: CoreEncryptor?,
        endpoint: Endpoint<*, TResponseData>,
        listener: IApiCallResponseListener<TResponseData>
    ) {
        response.use {
            try {
                if (response.isSuccessful) {
                    val responseBody = response.body
                        ?: throw IOException("Response body is null")

                    val resData = if (encryptor != null) {
                        val decryptedData = encryptor.decryptResponse(CoreEncryptedResponse(responseBody.bytes()))
                        okHttpClient.interceptors.mapNotNull { it as? ECIESInterceptor }.forEach {
                            it.encryptedResponseReceived(request.url.toUrl(), decryptedData)
                        }
                        decryptedData
                    } else {
                        responseBody.bytes()
                    }

                    val gson = gsonBuilder.create()
                    val typeAdapter = getTypeAdapter(gson, endpoint.responseType)
                    val converter = GsonResponseBodyConverter(gson, typeAdapter)
                    listener.onSuccess(converter.convert(resData))
                } else {
                    val bodyBytes = response.body?.bytes()
                    val errorResponse = bodyBytes?.let {
                        val gson = gsonBuilder.create()
                        val typeAdapter = getTypeAdapter(gson, ErrorResponse::class.java)
                        GsonResponseBodyConverter(gson, typeAdapter).convert(it)
                    }
                    listener.onFailure(ApiError(ApiHttpException(response, errorResponse)))
                }
            } catch (e: Throwable) {
                // do not allow the app to crash when unexpected body is returned
                listener.onFailure(ApiError(ApiHttpException(response, errorResponse = null, cause = e)))
            }
        }
    }

    private fun <TResponseData: StatusResponse> makeCall(
        bodyBytes: ByteArray,
        endpoint: Endpoint<*, TResponseData>,
        headers: HashMap<String, String>,
        okHttpInterceptor: OkHttpBuilderInterceptor? = null,
        listener: IApiCallResponseListener<TResponseData>
    ) {
        getEncryptor(endpoint) { result ->
            result.onFailure {
                listener.onFailure(ApiError(it))
            }.onSuccess { encryptor ->
                try {
                    val prepared = buildRequest(bodyBytes, endpoint, headers, okHttpInterceptor, encryptor)
                    prepared.client.newCall(prepared.request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            listener.onFailure(ApiError(e))
                        }

                        override fun onResponse(call: Call, response: Response) {
                            handleResponse(response, prepared.request, encryptor, endpoint, listener)
                        }
                    })
                } catch (e: Exception) {
                    listener.onFailure(ApiError(e))
                }
            }
        }
    }

    /**
     * Same as [makeCall], but blocks until the request completes, as required by
     * [PowerAuthSDK.getSerialExecutor]'s contract.
     */
    private fun <TResponseData: StatusResponse> makeBlockingCall(
        bodyBytes: ByteArray,
        endpoint: Endpoint<*, TResponseData>,
        headers: HashMap<String, String>,
        okHttpInterceptor: OkHttpBuilderInterceptor?,
        listener: IApiCallResponseListener<TResponseData>
    ) {
        val latch = CountDownLatch(1)
        var encryptorResult: Result<CoreEncryptor?>? = null
        getEncryptor(endpoint) { result ->
            encryptorResult = result
            latch.countDown()
        }
        // Bounded by PowerAuthSDK's own client timeouts, so a stuck callback can't hang the
        // shared serial executor forever.
        val clientConfig = powerAuthSDK.clientConfiguration
        val timeoutMillis = clientConfig.connectionTimeout.toLong() + clientConfig.readTimeout.toLong()
        if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
            listener.onFailure(ApiError(TimeoutException("Timed out waiting for the E2EE encryptor")))
            return
        }

        encryptorResult!!.onFailure {
            listener.onFailure(ApiError(it))
        }.onSuccess { encryptor ->
            try {
                val prepared = buildRequest(bodyBytes, endpoint, headers, okHttpInterceptor, encryptor)
                val response = prepared.client.newCall(prepared.request).execute()
                handleResponse(response, prepared.request, encryptor, endpoint, listener)
            } catch (e: Exception) {
                listener.onFailure(ApiError(e))
            }
        }
    }

    private fun <T> getTypeAdapter(gson: Gson, type: Class<T>): TypeAdapter<T> {
        return gson.getAdapter(TypeToken.get(type))
    }

    private fun getEncryptor(
        endpoint: Endpoint<*, *>,
        callback: (Result<CoreEncryptor?>) -> Unit
    ) {

        val listener = object : IGetEncryptorListener {
            override fun onGetEncryptorSuccess(encryptor: CoreEncryptor) {
                callback(Result.success(encryptor))
            }

            override fun onGetEncryptorFailed(t: Throwable) {
                callback(Result.failure(t))
            }
        }

        when (endpoint.e2eeConfiguration) {
            E2EEConfiguration.APPLICATION_SCOPE -> powerAuthSDK.getEncryptorForApplicationScope(listener)
            E2EEConfiguration.ACTIVATION_SCOPE -> powerAuthSDK.getEncryptorForActivationScope(listener)
            E2EEConfiguration.NOT_ENCRYPTED -> callback(Result.success(null))
        }
    }

    // DEPRECATED: retained for source and binary compatibility until the next major version.

    @Deprecated(
        "This method is no longer needed and will be removed in the next major version.",
        level = DeprecationLevel.WARNING
    )
    @PublishedApi
    internal fun synchronizeTime(completion: (Result<Unit>) -> Unit) {
        val ts = powerAuthSDK.timeSynchronizationService
        if (ts.isTimeSynchronized) {
            completion(Result.success(Unit))
        } else {
            WPNLogger.i("Time is not synchronized, requesting synchronization first.")
            ts.synchronizeTime(object: ITimeSynchronizationListener {
                override fun onTimeSynchronizationSucceeded() {
                    completion(Result.success(Unit))
                }

                override fun onTimeSynchronizationFailed(t: Throwable) {
                    WPNLogger.e("Time failed to synchronize, stopping whole request: $t")
                    completion(Result.failure(t))
                }
            })
        }
    }

    /**
     * Compatibility constructor for the removed token provider integration.
     *
     * The token provider is ignored. Token-authenticated requests always use the SDK token store.
     */
    @Deprecated(
        "The token provider is ignored and will be removed in the next major version.",
        level = DeprecationLevel.WARNING
    )
    @Suppress("DEPRECATION")
    constructor(
        baseUrl: String,
        okHttpClient: OkHttpClient,
        powerAuthSDK: PowerAuthSDK,
        gsonBuilder: GsonBuilder,
        appContext: Context,
        @Suppress("UNUSED_PARAMETER", "DEPRECATION")
        tokenProvider: IPowerAuthTokenProvider?,
        userAgent: UserAgent = UserAgent.libraryDefault(appContext)
    ) : this(
        baseUrl,
        okHttpClient,
        powerAuthSDK,
        gsonBuilder,
        appContext,
        userAgent
    )
}

interface OkHttpBuilderInterceptor {
    fun intercept(builder: OkHttpClient.Builder)
}

/**
 * Strategy used to dispatch [EndpointAuthenticated] requests. [EndpointBasic] and
 * [EndpointAuthenticatedWithToken] are unaffected and always dispatched concurrently.
 *
 * PowerAuth authentication codes use a counter, so signed requests must be validated on the
 * server in order - if more than one is issued at the same time, one may fail.
 * See [PowerAuthSDK.getSerialExecutor] for details.
 */
enum class RequestConcurrencyStrategy {
    /** All requests are dispatched concurrently (previous default behavior). */
    CONCURRENT_ALL,
    /** Serialized via [PowerAuthSDK.getSerialExecutor], one in flight at a time. */
    SERIAL_AUTHENTICATED
}

class UserAgent internal constructor(internal val value: String? = null) {
    companion object {
        fun libraryDefault(appContext: Context): UserAgent {
            return try {
                val appInfo = AppUtils.getMyPackageBasicInfo(appContext)
                val product = "PowerAuthNetworking"
                val sdkVer = BuildConfig.VERSION_NAME
                val appVer = appInfo.versionName
                val appId = appInfo.packageName
                val lang = appContext.getCurrentLocale().language // we use here only language to fit iOS implementation
                val maker = Build.BRAND
                val os = "Android"
                val osVer = Build.VERSION.RELEASE
                val model = Build.MODEL
                val network = ConnectionMonitor.getConnectivityStatus(appContext)
                UserAgent("$product/$sdkVer ($lang; $network) $appId/$appVer ($maker; $os/$osVer; $model)")
            } catch (t: Throwable) {
                WPNLogger.e("Failed to construct library default User-Agent: $t")
                UserAgent("PowerAuthNetworking/${BuildConfig.VERSION_NAME}")
            }
        }

        fun systemDefault() = UserAgent()

        fun customValue(value: String) = UserAgent(value)
    }
}
