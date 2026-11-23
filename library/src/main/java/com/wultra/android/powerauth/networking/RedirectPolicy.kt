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

import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Decides whether to replay a POST once with the same body after a 307 or 308.
 * The callback runs synchronously on the request thread and may be called concurrently.
 * Return `false` to report the original redirect as an HTTP error.
 *
 * HTTPS-to-HTTP redirects are not replayed. A cross-origin replay of a basic,
 * unencrypted request does not copy the original request headers.
 * PowerAuth-authenticated and encrypted requests, and requests carrying PowerAuth headers,
 * Authorization, Proxy-Authorization, or an explicit Cookie header, can only be replayed
 * on the same HTTPS origin.
 */
fun interface RedirectPolicy {
    fun shouldReplay(source: HttpUrl, destination: HttpUrl, statusCode: Int): Boolean
}

internal class RedirectInterceptor(
    private val policy: RedirectPolicy,
    private val protectedRequest: Boolean
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code != 307 && response.code != 308) return response
        if (response.request.url != request.url || response.request.method != request.method) return response
        if (request.body?.isOneShot() == true || request.body?.isDuplex() == true) return response

        val from = request.url
        val location = response.headers("Location").singleOrNull() ?: return response
        val to = from.resolve(location)?.newBuilder()?.fragment(null)?.build() ?: return response
        if (to.username.isNotEmpty() || to.password.isNotEmpty() || to == from.newBuilder().fragment(null).build()) {
            return response
        }
        val replay = try {
            policy.shouldReplay(from, to, response.code)
        } catch (e: Exception) {
            response.close()
            throw IOException("Redirect policy failed", e)
        }
        if (!replay || (from.isHttps && !to.isHttps)) return response

        val sameOrigin = from.scheme == to.scheme && from.host == to.host && from.port == to.port
        val hasCredentials = request.hasCredentialHeaders() || response.request.hasCredentialHeaders()
        if ((!sameOrigin || !to.isHttps) && (protectedRequest || hasCredentials)) return response

        val followUp = request.newBuilder().url(to)
        if (!sameOrigin) {
            followUp.headers(Headers.Builder().build())
        }
        response.close()
        return chain.proceed(followUp.build())
    }

    private fun Request.hasCredentialHeaders(): Boolean = headers.names().any {
        it.startsWith("X-PowerAuth-", ignoreCase = true) ||
            it.equals("Authorization", ignoreCase = true) ||
            it.equals("Proxy-Authorization", ignoreCase = true) ||
            it.equals("Cookie", ignoreCase = true)
    }
}
