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

package com.wultra.android.powerauth.networking

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal object SameOriginHttpsRedirectInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code != 307 && response.code != 308) return response
        if (response.request.url != request.url || response.request.method != request.method) return response
        if (request.hasCredentialHeaders() || response.request.hasCredentialHeaders()) return response
        if (request.body?.isOneShot() == true || request.body?.isDuplex() == true) return response

        val from = request.url
        val location = response.headers("Location").singleOrNull() ?: return response
        val to = from.resolve(location)?.newBuilder()?.fragment(null)?.build() ?: return response
        if (!from.isHttps || !to.isHttps || from.host != to.host || from.port != to.port) return response
        if (to.username.isNotEmpty() || to.password.isNotEmpty() || to == from.newBuilder().fragment(null).build()) {
            return response
        }

        response.close()
        return chain.proceed(request.newBuilder().url(to).build())
    }

    private fun Request.hasCredentialHeaders(): Boolean = headers.names().any {
        it.startsWith("X-PowerAuth-", ignoreCase = true) ||
            it.equals("Authorization", ignoreCase = true) ||
            it.equals("Cookie", ignoreCase = true)
    }
}
