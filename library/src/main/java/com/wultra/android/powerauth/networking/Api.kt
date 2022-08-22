/*
 * Copyright (c) 2020, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

@file:Suppress("unused")

package com.wultra.android.powerauth.networking

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.wultra.android.powerauth.BuildConfig
import com.wultra.android.powerauth.networking.data.BaseRequest
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.error.ApiError
import com.wultra.android.powerauth.networking.error.ApiHttpException
import com.wultra.android.powerauth.networking.error.ErrorResponse
import com.wultra.android.powerauth.networking.processing.GsonRequestBodyBytes
import com.wultra.android.powerauth.networking.processing.GsonResponseBodyConverter
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenListener
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import com.wultra.android.powerauth.networking.tokens.TokenManager
import com.wultra.android.powerauth.networking.utils.AppUtils
import com.wultra.android.powerauth.networking.utils.getCurrentLocale
import com.wultra.android.powerauth.networking.utils.toBcp47LanguageTag
import io.getlime.security.powerauth.core.EciesCryptogram
import io.getlime.security.powerauth.core.EciesEncryptor
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import io.getlime.security.powerauth.sdk.PowerAuthToken
import okhttp3.*
import java.io.IOException
import java.util.*

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
    @PublishedApi internal val okHttpClient: OkHttpClient,
    @PublishedApi internal val powerAuthSDK: PowerAuthSDK,
    @PublishedApi internal val gsonBuilder: GsonBuilder,
    @PublishedApi internal val appContext: Context,
    @PublishedApi internal val tokenProvider: IPowerAuthTokenProvider = TokenManager(appContext, powerAuthSDK.tokenStore),
    @PublishedApi internal val userAgent: UserAgent = UserAgent.libraryDefault(appContext)) {

    /**
     * Language sent in request header. Default value is "en".
     */
    var acceptLanguage = "en"

     // PUBLIC API

    inline fun <reified TRequestData: BaseRequest, reified TResponseData: StatusResponse> post(
        data: TRequestData,
        endpoint: EndpointBasic<TRequestData, TResponseData>,
        headers: HashMap<String, String>? = null,
        encryptor: EciesEncryptor? = null,
        listener: IApiCallResponseListener<TResponseData>) {

        val requestGson = gsonBuilder.create()
        val requestTypeAdapter = getTypeAdapter<TRequestData>(requestGson)
        val bodyBytes = GsonRequestBodyBytes(requestGson, requestTypeAdapter).convert(data)

        makeCall(bodyBytes, endpoint, headers ?: hashMapOf(), encryptor, listener)
    }

    inline fun <reified TRequestData: BaseRequest, reified TResponseData: StatusResponse> post(
        data: TRequestData,
        endpoint: EndpointSigned<TRequestData, TResponseData>,
        authentication: PowerAuthAuthentication,
        headers: HashMap<String, String>? = null,
        encryptor: EciesEncryptor? = null,
        listener: IApiCallResponseListener<TResponseData>) {

        val requestGson = gsonBuilder.create()
        val requestTypeAdapter = getTypeAdapter<TRequestData>(requestGson)
        val bodyBytes = GsonRequestBodyBytes(requestGson, requestTypeAdapter).convert(data)

        val authorizationHeader = powerAuthSDK.requestSignatureWithAuthentication(appContext, authentication, "POST", endpoint.uriId, bodyBytes)

        val newHeaders = headers ?: hashMapOf()
        newHeaders[authorizationHeader.key] = authorizationHeader.value

        makeCall(bodyBytes, endpoint, newHeaders, encryptor, listener)
    }

    inline fun <reified TRequestData: BaseRequest, reified TResponseData: StatusResponse> post(
        data: TRequestData,
        endpoint: EndpointSignedWithToken<TRequestData, TResponseData>,
        headers: HashMap<String, String>? = null,
        encryptor: EciesEncryptor? = null,
        listener: IApiCallResponseListener<TResponseData>) {

        tokenProvider.getTokenAsync(endpoint.tokenName, object : IPowerAuthTokenListener {
            override fun onReceived(token: PowerAuthToken) {

                val tokenHeader = token.generateHeader()
                val requestGson = gsonBuilder.create()
                val requestTypeAdapter = getTypeAdapter<TRequestData>(requestGson)
                val bodyBytes = GsonRequestBodyBytes(requestGson, requestTypeAdapter).convert(data)
                val newHeaders = headers ?: hashMapOf()
                newHeaders[tokenHeader.key] = tokenHeader.value

                makeCall(bodyBytes, endpoint, newHeaders, encryptor, listener)
            }

            override fun onFailed(e: Throwable) {
                listener.onFailure(ApiError(e))
            }
        })
    }

    // PRIVATE API

    @PublishedApi
    internal inline fun <reified TRequestData: BaseRequest, reified TResponseData: StatusResponse> makeCall(
        bodyBytes: ByteArray,
        endpoint: Endpoint<TRequestData, TResponseData>,
        headers: HashMap<String, String>,
        encryptor: EciesEncryptor? = null,
        listener: IApiCallResponseListener<TResponseData>) {

        var bytes = bodyBytes

        if (encryptor != null) {
            val cryptogram = encryptor.encryptRequest(bodyBytes)
            if (cryptogram != null) {
                val e2eePayload = E2EERequest(cryptogram.keyBase64, cryptogram.bodyBase64, cryptogram.macBase64, cryptogram.nonceBase64)
                bytes = Gson().toJson(e2eePayload).encodeToByteArray()
                if (endpoint is EndpointBasic || endpoint is EndpointSignedWithToken) {
                    headers[encryptor.metadata.httpHeaderKey] = encryptor.metadata.httpHeaderValue
                }
            }
        }

        val body = RequestBody.create(MediaType.parse("application/json; charset=UTF-8")!!, bytes)

        val requestBuilder = Request.Builder()
            .url("${baseUrl.removeSuffix("/")}/${endpoint.endpointUrlPath.removePrefix("/")}")
            .post(body)
            .header("Accept-Language", acceptLanguage)

        userAgent.value?.let { requestBuilder.header("User-Agent", it) }

        headers.forEach { requestBuilder.header(it.key, it.value) }

        val call = okHttpClient.newCall(requestBuilder.build())
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                listener.onFailure(ApiError(e))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {

                        val resData = if (encryptor != null) {
                            val envelope = Gson().fromJson(response.body()!!.string(), E2EEResponse::class.java)
                            encryptor.decryptResponse(EciesCryptogram(envelope.encryptedData, envelope.mac))
                        } else {
                            response.body()!!.bytes()
                        }

                        val gson = gsonBuilder.create()
                        val typeAdapter = getTypeAdapter<TResponseData>(gson)
                        val converter = GsonResponseBodyConverter(gson, typeAdapter)
                        listener.onSuccess(converter.convert(resData))
                    } else {
                        val gson = gsonBuilder.create()
                        val typeAdapter = getTypeAdapter<ErrorResponse>(gson)
                        val converter = GsonResponseBodyConverter(gson, typeAdapter)
                        val errorResponse = converter.convert(response.body()!!)
                        listener.onFailure(ApiError(ApiHttpException(response, errorResponse)))
                    }
                } catch (e: Throwable) {
                    // do not allow the app to crash when unexpected body is returned
                    listener.onFailure(ApiError(ApiHttpException(response, errorResponse = null, cause = e)))
                }
            }
        })
    }

    @PublishedApi
    internal inline fun <reified T> getTypeAdapter(gson: Gson): TypeAdapter<T> {
        return gson.getAdapter(TypeToken.get(T::class.java))
    }
}

class UserAgent internal constructor(@PublishedApi internal val value: String? = null) {
    companion object {
        fun libraryDefault(appContext: Context): UserAgent {
            val appInfo = AppUtils.getMyPackageBasicInfo(appContext)
            return UserAgent(
                "PowerAuthNetworking/${BuildConfig.VERSION_NAME} " +
                "(${Build.BRAND}; ${appContext.getCurrentLocale().toBcp47LanguageTag()}) " +
                "${appInfo.packageName}/${appInfo.versionName}"
            )
        }

        fun systemDefault() = UserAgent()

        fun customValue(value: String) = UserAgent(value)
    }
}

/** Envelope for E2EE requests. */
@PublishedApi internal class E2EERequest(
    @SerializedName("ephemeralPublicKey") val ephemeralPublicKey: String?,
    @SerializedName("encryptedData") val encryptedData: String?,
    @SerializedName("mac") val mac: String?,
    @SerializedName("nonce") val nonce: String?
    )

/** Envelope for E2EE responses. */
@PublishedApi internal class E2EEResponse(
    @SerializedName("encryptedData") val encryptedData: String?,
    @SerializedName("mac") val mac: String?
    )