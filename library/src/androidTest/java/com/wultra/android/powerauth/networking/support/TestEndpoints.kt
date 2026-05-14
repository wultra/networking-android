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

import com.wultra.android.powerauth.networking.E2EEConfiguration
import com.wultra.android.powerauth.networking.EndpointBasic
import com.wultra.android.powerauth.networking.EndpointSigned
import com.wultra.android.powerauth.networking.EndpointSignedWithToken
import com.wultra.android.powerauth.networking.data.BaseRequest
import com.wultra.android.powerauth.networking.data.ObjectRequest
import com.wultra.android.powerauth.networking.data.StatusResponse

/**
 * Pre-defined endpoint descriptors used across integration tests.
 *
 * Each property represents a different endpoint type supported by the library:
 * - [posts] — a basic (unsigned) endpoint aimed at a public REST API.
 * - [start] — a basic endpoint with application-scope end-to-end encryption (E2EE).
 * - [history] — a signed endpoint (PowerAuth authentication code, no token).
 * - [operationList] — a token-signed endpoint using the `possession_universal` token.
 * - [failingStart] — intentionally configured with activation-scope E2EE on an
 *   endpoint that expects application-scope, so tests can verify error handling.
 */
object TestEndpoints {

    /** Simple unsigned POST to `/posts` (e.g. JSONPlaceholder). */
    val posts = EndpointBasic<BaseRequest, StatusResponse>("/posts")

    /** Onboarding start with application-scope E2EE. */
    val start = EndpointBasic<StartObjectRequest, StatusResponse>(
        "/api/onboarding/start",
        E2EEConfiguration.APPLICATION_SCOPE
    )

    /** Signed endpoint for fetching operation history (uriId = `/operation/history`). */
    val history = EndpointSigned<BaseRequest, StatusResponse>(
        "/api/auth/token/app/operation/history",
        "/operation/history"
    )

    /** Token-signed endpoint for listing pending operations. */
    val operationList = EndpointSignedWithToken<BaseRequest, StatusResponse>(
        "/api/auth/token/app/operation/list",
        "possession_universal"
    )

    /**
     * Deliberately misconfigured endpoint — uses activation-scope E2EE against
     * a server path that requires application-scope. Used to test error paths.
     */
    val failingStart = EndpointBasic<BaseRequest, StatusResponse>(
        "/api/onboarding/start",
        E2EEConfiguration.ACTIVATION_SCOPE
    )

    /** Request body for the onboarding start endpoint. */
    data class StartRequest(val identification: Map<String, String>)

    /** Typed wrapper that serialises [StartRequest] inside `requestObject`. */
    class StartObjectRequest(requestObject: StartRequest) : ObjectRequest<StartRequest>(requestObject)
}
