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

package com.wultra.android.powerauth.networking.tokens

import android.content.Context
import io.getlime.security.powerauth.networking.response.IGetTokenListener
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthToken
import io.getlime.security.powerauth.sdk.PowerAuthTokenStore

/**
 * Manager for PowerAuth token header handling.
 * Default internal implementation of [IPowerAuthTokenProvider]
 */
internal class TokenManager(
    private val appContext: Context,
    private val powerAuthTokenStore: PowerAuthTokenStore) : IPowerAuthTokenProvider {

    override fun getTokenAsync(tokenName: String, listener: IPowerAuthTokenListener) {
        val localPowerAuthToken = powerAuthTokenStore.getLocalToken(appContext, tokenName)
        if (localPowerAuthToken != null) {
            listener.onReceived(localPowerAuthToken)
        } else {
            val authentication = PowerAuthAuthentication.possession()
            try {
                powerAuthTokenStore.requestAccessToken(appContext, tokenName, authentication, object : IGetTokenListener {
                    override fun onGetTokenSucceeded(token: PowerAuthToken) {
                        listener.onReceived(token)
                    }

                    override fun onGetTokenFailed(t: Throwable) {
                        listener.onFailed(t)
                    }
                })
            } catch (t: Throwable) {
                listener.onFailed(t)
            }
        }
    }
}