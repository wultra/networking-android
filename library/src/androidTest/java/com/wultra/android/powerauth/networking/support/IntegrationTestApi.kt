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
