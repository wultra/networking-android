/*
 * Copyright (c) 2022, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.powerauth.networking

import okhttp3.Interceptor
import java.net.URL

/**
 * Interceptor that can additionally handle encrypted response.
 */
interface ECIESInterceptor: Interceptor {
    /**
     * When ECIES encrypted response is received, this function is called when
     * the response is successfully decrypted.
     *
     * @param url URL of the response
     * @param decrypted Decrypted payload
     */
    fun encryptedResponseReceived(url: URL, decrypted: ByteArray)
}