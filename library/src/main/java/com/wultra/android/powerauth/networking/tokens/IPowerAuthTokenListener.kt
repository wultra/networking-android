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

import io.getlime.security.powerauth.sdk.PowerAuthToken

/**
 * Consumer of the PowerAuth token.
 */
interface IPowerAuthTokenListener {
    fun onReceived(token: PowerAuthToken)
    fun onFailed(e: Throwable)
}