/*
 * Copyright 2022 Wultra s.r.o.
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