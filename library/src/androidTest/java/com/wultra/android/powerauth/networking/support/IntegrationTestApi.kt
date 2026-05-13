package com.wultra.android.powerauth.networking.support

import android.content.Context
import com.google.gson.GsonBuilder
import com.wultra.android.powerauth.networking.Api
import com.wultra.android.powerauth.networking.UserAgent
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient

// ============================================================================
// Concrete Api subclass for tests
// ============================================================================

/** Concrete [Api] subclass — needed because [Api] is abstract. */
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