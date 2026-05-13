package com.wultra.android.powerauth.networking.support

import android.content.Context
import com.google.gson.GsonBuilder
import com.wultra.android.powerauth.networking.Api
import com.wultra.android.powerauth.networking.UserAgent
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient

/**
 * Minimal [Api] subclass used by integration tests.
 *
 * It delegates all HTTP/signing behavior to the parent [Api] class and does not
 * add any endpoints of its own — test endpoints are defined in [TestEndpoints].
 * Instances are typically created via [PowerAuthIntegrationProxy.createApi].
 *
 * @param baseUrl       Root URL of the server under test (e.g. enrollment or operations server).
 * @param okHttpClient  OkHttp client instance; tests may supply a custom one for SSL pinning etc.
 * @param powerAuthSDK  Initialized PowerAuth SDK used for request signing.
 * @param appContext    Android application context.
 * @param userAgent     User-Agent value sent with every request; defaults to the system value.
 */
class IntegrationTestApi(
    baseUrl: String,
    okHttpClient: OkHttpClient,
    powerAuthSDK: PowerAuthSDK,
    appContext: Context,
    userAgent: UserAgent = UserAgent.systemDefault()
) : Api(
    baseUrl = baseUrl,
    okHttpClient = okHttpClient,
    powerAuthSDK = powerAuthSDK,
    gsonBuilder = GsonBuilder(),
    appContext = appContext,
    userAgent = userAgent
)
