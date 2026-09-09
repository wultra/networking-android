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

package com.wultra.android.powerauth.networking.tokens

import android.content.Context
import io.getlime.security.powerauth.networking.response.IGetTokenListener
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthToken
import io.getlime.security.powerauth.sdk.PowerAuthTokenStore

/**
 * Deprecated compatibility implementation for the former [IPowerAuthTokenProvider] default.
 *
 * New library code uses [PowerAuthTokenStore] directly. This class remains available only for
 * previously compiled inline token requests and will be removed in the next major version.
 */
@Deprecated(
    "TokenManager is retained only for binary compatibility and will be removed in the next major version.",
    level = DeprecationLevel.WARNING
)
internal class TokenManager(
    appContext: Context,
    private val powerAuthTokenStore: PowerAuthTokenStore
) : IPowerAuthTokenProvider {

    private val appContext: Context = appContext.applicationContext

    override fun getTokenAsync(tokenName: String, listener: IPowerAuthTokenListener) {
        val localPowerAuthToken = powerAuthTokenStore.getLocalToken(appContext, tokenName)
        if (localPowerAuthToken != null) {
            listener.onReceived(localPowerAuthToken)
        } else {
            try {
                powerAuthTokenStore.requestAccessToken(
                    appContext,
                    tokenName,
                    PowerAuthAuthentication.possession(),
                    object : IGetTokenListener {
                        override fun onGetTokenSucceeded(token: PowerAuthToken) {
                            listener.onReceived(token)
                        }

                        override fun onGetTokenFailed(t: Throwable) {
                            listener.onFailed(t)
                        }
                    }
                )
            } catch (t: Throwable) {
                listener.onFailed(t)
            }
        }
    }
}
