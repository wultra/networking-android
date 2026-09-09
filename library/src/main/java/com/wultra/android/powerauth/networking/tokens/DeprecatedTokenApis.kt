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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Deprecated token provider APIs retained only for source compatibility. These APIs will be
 * removed in the next major version.
 *
 * These APIs are never invoked by the library. Token-authenticated requests always use
 * [io.getlime.security.powerauth.sdk.PowerAuthSDK.tokenStore].
 */
package com.wultra.android.powerauth.networking.tokens

import io.getlime.security.powerauth.sdk.PowerAuthToken

@Deprecated(
    "Token providers are ignored and will be removed in the next major version. " +
        "Token-authenticated requests use PowerAuthSDK.tokenStore.",
    level = DeprecationLevel.WARNING
)
interface IPowerAuthTokenProvider {
    fun getTokenAsync(tokenName: String, listener: IPowerAuthTokenListener)
}

@Deprecated(
    "Token listeners are retained only for compatibility, are never invoked by the library, " +
        "and will be removed in the next major version.",
    level = DeprecationLevel.WARNING
)
interface IPowerAuthTokenListener {
    fun onReceived(token: PowerAuthToken)
    fun onFailed(e: Throwable)
}
