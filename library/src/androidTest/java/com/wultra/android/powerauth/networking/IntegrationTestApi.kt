/*
 * Copyright 2026 Wultra s.r.o.
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
import com.google.gson.GsonBuilder
import com.wultra.android.powerauth.networking.data.BaseRequest
import com.wultra.android.powerauth.networking.data.StatusResponse
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient

/**
 * Concrete [Api] subclass for testing. The [Api] class is abstract to prevent
 * direct instantiation in production code, but tests need a concrete instance
 * to exercise the HTTP pipeline.
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

// --- Test endpoints ---

/**
 * Endpoints used in integration tests. Inspired by real endpoints
 * from digital-onboarding and mtoken SDKs to ensure the library is
 * validated against the same server contracts used in production.
 */
object TestEndpoints {

    /** Plain endpoint for jsonplaceholder (no auth, no E2EE). */
    val todo = EndpointBasic<BaseRequest, StatusResponse>("/posts")

    /** E2EE endpoint with application scope, inspired by digital-onboarding Start. */
    val start = EndpointBasic<BaseRequest, StatusResponse>(
        "/api/onboarding/start",
        E2EEConfiguration.APPLICATION_SCOPE
    )

    /** Signed endpoint inspired by mtoken History. */
    val history = EndpointSigned<BaseRequest, StatusResponse>(
        "/api/auth/token/app/operation/history",
        "/operation/history"
    )

    /** Token-signed endpoint inspired by mtoken List. */
    val operationList = EndpointSignedWithToken<BaseRequest, StatusResponse>(
        "/api/auth/token/app/operation/list",
        "possession_universal"
    )

    /** E2EE endpoint with activation scope (will fail without activation). */
    val failingStart = EndpointBasic<BaseRequest, StatusResponse>(
        "/api/onboarding/start",
        E2EEConfiguration.ACTIVATION_SCOPE
    )
}
