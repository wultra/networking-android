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

@file:Suppress("unused")

package com.wultra.android.powerauth.networking

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.wultra.android.powerauth.BuildConfig
import com.wultra.android.powerauth.networking.data.BaseRequest
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.error.ApiError
import com.wultra.android.powerauth.networking.error.ApiHttpException
import com.wultra.android.powerauth.networking.error.ErrorResponse
import com.wultra.android.powerauth.networking.log.WPNLogger
import com.wultra.android.powerauth.networking.processing.GsonRequestBodyBytes
import com.wultra.android.powerauth.networking.processing.GsonResponseBodyConverter
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenListener
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import com.wultra.android.powerauth.networking.tokens.TokenManager
import com.wultra.android.powerauth.networking.utils.AppUtils
import com.wultra.android.powerauth.networking.utils.ConnectionMonitor
import com.wultra.android.powerauth.networking.utils.getCurrentLocale
import io.getlime.security.powerauth.core.CoreEncryptedResponse
import io.getlime.security.powerauth.core.CoreEncryptor
import io.getlime.security.powerauth.networking.response.IGetEncryptorListener
import io.getlime.security.powerauth.networking.response.ITimeSynchronizationListener
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import io.getlime.security.powerauth.sdk.PowerAuthToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient.Builder
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

interface IApiCallResponseListener<T> {
    fun onSuccess(result: T)
    fun onFailure(error: ApiError)
}

/**
 * Common API methods for making request via OkHttp and (de)serializing data via Gson.
 *
 * @param baseUrl Base url for all requests
 * @param okHttpClient Configured Http Client
 * @param powerAuthSDK Power Auth instance for request signing
 * @param gsonBuilder Builder that will be used for request/response (de)serialization.
 * @param tokenProvider Token provided for token signing.
 * @param userAgent Default user agent for each request. Note that such value might be "overridden"
 * on per-request basis. Default value is `libraryDefault`.
 */
abstract class Api(
    @PublishedApi internal val baseUrl: String,
    okHttpClient: OkHttpClient,
    @PublishedApi internal val powerAuthSDK: PowerAuthSDK,
    @PublishedApi internal val gsonBuilder: GsonBuilder,
    @PublishedApi internal val appContext: Context,
    tokenProvider: IPowerAuthTokenProvider? = null,
    @PublishedApi internal val userAgent: UserAgent = UserAgent.libraryDefault(appContext)
) {
    /**
     * Language sent in request header. Default value is "en".
     */
    var acceptLanguage = "en"

    @PublishedApi internal val okHttpClient: OkHttpClient

    @PublishedApi internal val tokenProvider: IPowerAuthTokenProvider = tokenProvider ?: TokenManager(appContext, powerAuthSDK.tokenStore)

    init {
        val builder = okHttpClient.newBuilder()
        WPNLogger.configure(builder)
        this.okHttpClient = builder.build()
    }

    // PUBLIC API

    inline fun <reified TRequestData: BaseRequest, reified TResponseData: StatusResponse> post(
        data: TRequestData,
        endpoint: EndpointBasic<TRequestData, TResponseData>,
        headers: HashMap<String, String>? = null,
        okHttpInterceptor: OkHttpBuilderInterceptor? = null,
        listener: IApiCallResponseListener<TResponseData>
    ) {
        makeCall(getBodyBytes(data), endpoint, headers ?: hashMapOf(), okHttpInterceptor, listener)
    }

    inline fun <reified TRequestData: BaseRequest, reified TResponseData: StatusResponse> post(
        data: TRequestData,
        endpoint: EndpointSigned<TRequestData, TResponseData>,
        authentication: PowerAuthAuthentication,
        headers: HashMap<String, String>? = null,
        okHttpInterceptor: OkHttpBuilderInterceptor? = null,
        listener: IApiCallResponseListener<TResponseData>
    ) {

        val bodyBytes = getBodyBytes(data)
        val newHeaders = headers ?: hashMapOf()

        try {
            val authorizationHeader = powerAuthSDK.authenticationHeaderForRequestWithBody(
                authentication,
                "POST",
                endpoint.uriId,
                bodyBytes
            )
            newHeaders[authorizationHeader.key] = authorizationHeader.value
        } catch (e: Exception) {
            listener.onFailure(ApiError(e))
            return
        }

        makeCall(bodyBytes, endpoint, newHeaders, okHttpInterceptor, listener)
    }

    inline fun <reified TRequestData: BaseRequest, reified TResponseData: StatusResponse> post(
        data: TRequestData,
        endpoint: EndpointSignedWithToken<TRequestData, TResponseData>,
        headers: HashMap<String, String>? = null,
        okHttpInterceptor: OkHttpBuilderInterceptor? = null,
        listener: IApiCallResponseListener<TResponseData>
    ) {
        // note: remove time synchronization from here after the https://github.com/wultra/networking-android/issues/67 is implemented
        // then, use powerAuthTokenStore.generateAuthenticationHeader.
        synchronizeTime {
            it.onSuccess {
                tokenProvider.getTokenAsync(
                    endpoint.tokenName,
                    object : IPowerAuthTokenListener {
                        override fun onReceived(token: PowerAuthToken) {
                            val bodyBytes = getBodyBytes(data)
                            val newHeaders = headers ?: hashMapOf()
                            try {
                                val tokenHeader = token.generateTokenHeader()
                                newHeaders[tokenHeader.key] = tokenHeader.value
                            } catch (e: Exception) {
                                listener.onFailure(ApiError(e))
                                return
                            }
                            makeCall(bodyBytes, endpoint, newHeaders, okHttpInterceptor, listener)
                        }

                        override fun onFailed(e: Throwable) {
                            listener.onFailure(ApiError(e))
                        }
                    }
                )
            }.onFailure { e ->
                listener.onFailure(ApiError(e))
            }
        }
    }

    // PRIVATE API

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

    @PublishedApi
    internal inline fun <reified TRequestData: BaseRequest> getBodyBytes(data: TRequestData): ByteArray {
        val requestGson = gsonBuilder.create()
        val requestTypeAdapter = getTypeAdapter<TRequestData>(requestGson)
        return GsonRequestBodyBytes(requestGson, requestTypeAdapter).convert(data)
    }

    @PublishedApi
    internal inline fun <reified TRequestData: BaseRequest, reified TResponseData: StatusResponse> makeCall(
        bodyBytes: ByteArray,
        endpoint: Endpoint<TRequestData, TResponseData>,
        headers: HashMap<String, String>,
        okHttpInterceptor: OkHttpBuilderInterceptor? = null,
        listener: IApiCallResponseListener<TResponseData>
    ) {

        var bytes = bodyBytes

        getEncryptor(endpoint) { result ->
            result.onFailure {
                listener.onFailure(ApiError(it))
            }.onSuccess { encryptor ->
                if (encryptor != null) {
                    try {
                        val encryptedRequest = encryptor.encryptRequest(bytes)
                        bytes = encryptedRequest.requestBody
                        if (endpoint is EndpointBasic || endpoint is EndpointSignedWithToken) {
                            encryptedRequest.requestHeaders.forEach { header ->
                                headers[header.key] = header.value
                            }
                        }
                    } catch (e: Exception) {
                        listener.onFailure(ApiError(e))
                        return@onSuccess
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
                val call = client.newCall(request)
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        listener.onFailure(ApiError(e))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            if (response.isSuccessful) {
                                val resData = if (encryptor != null) {
                                    val decryptedData = encryptor.decryptResponse(CoreEncryptedResponse(response.body!!.bytes()))
                                    okHttpClient.interceptors.mapNotNull { it as? ECIESInterceptor }.forEach {
                                        // only cryptogram is passed down the interceptor
                                        it.encryptedResponseReceived(request.url.toUrl(), decryptedData)
                                    }
                                    decryptedData
                                } else {
                                    response.body!!.bytes()
                                }

                                val gson = gsonBuilder.create()
                                val typeAdapter = getTypeAdapter<TResponseData>(gson)
                                val converter = GsonResponseBodyConverter(gson, typeAdapter)
                                listener.onSuccess(converter.convert(resData))
                            } else {
                                val gson = gsonBuilder.create()
                                val typeAdapter = getTypeAdapter<ErrorResponse>(gson)
                                val converter = GsonResponseBodyConverter(gson, typeAdapter)
                                val errorResponse = converter.convert(response.body!!)
                                listener.onFailure(ApiError(ApiHttpException(response, errorResponse)))
                            }
                        } catch (e: Throwable) {
                            // do not allow the app to crash when unexpected body is returned
                            listener.onFailure(ApiError(ApiHttpException(response, errorResponse = null, cause = e)))
                        }
                    }
                })
            }
        }
    }

    @PublishedApi
    internal inline fun <reified T> getTypeAdapter(gson: Gson): TypeAdapter<T> {
        return gson.getAdapter(TypeToken.get(T::class.java))
    }

    @PublishedApi
    internal inline fun <reified TRequestData: BaseRequest, reified TResponseData: StatusResponse> getEncryptor(
        endpoint: Endpoint<TRequestData, TResponseData>,
        crossinline callback: (Result<CoreEncryptor?>) -> Unit
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
}

interface OkHttpBuilderInterceptor {
    fun intercept(builder: Builder)
}

class UserAgent internal constructor(@PublishedApi internal val value: String? = null) {
    companion object {
        fun libraryDefault(appContext: Context): UserAgent {
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
            return UserAgent("$product/$sdkVer ($lang; $network) $appId/$appVer ($maker; $os/$osVer; $model)")
        }

        fun systemDefault() = UserAgent()

        fun customValue(value: String) = UserAgent(value)
    }
}
