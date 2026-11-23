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

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SameOriginHttpsRedirectInterceptorTest {
    @Test
    fun followsOneSameOriginRedirectWithoutRewritingPost() {
        val request = post()
        val (status, attempts) = exchange(request, 308, "/second")

        assertEquals(307, status)
        assertEquals(listOf("/first", "/second"), attempts.map { it.url.encodedPath })
        assertEquals("POST", attempts.last().method)
        assertSame(request.body, attempts.last().body)
    }

    @Test
    fun rejectsOriginChangesAndMethodChangingRedirects() {
        val cases = listOf(
            307 to "https://other.invalid/second",
            307 to "https://service.invalid:8443/second",
            307 to "http://service.invalid/second",
            302 to "/second"
        )
        for ((status, location) in cases) {
            val request = post()
            val (receivedStatus, attempts) = exchange(request, status, location)
            assertEquals(location, status, receivedStatus)
            assertEquals(location, listOf(request), attempts)
        }
    }

    @Test
    fun doesNotRedirectRequestsWithExplicitCredentialHeaders() {
        for (header in listOf("x-powerauth-token", "Authorization", "Cookie")) {
            val request = post().newBuilder().header(header, "dummy").build()
            val (status, attempts) = exchange(request, 307, "/second")

            assertEquals(307, status)
            assertEquals(listOf(request), attempts)
        }
    }

    private fun post(): Request = Request.Builder()
        .url("https://service.invalid/first")
        .post("{}".toRequestBody())
        .build()

    private fun exchange(request: Request, status: Int, location: String): Pair<Int, List<Request>> {
        val attempts = mutableListOf<Request>()
        val client = OkHttpClient.Builder()
            .followRedirects(false)
            .addInterceptor(SameOriginHttpsRedirectInterceptor)
            .addInterceptor { chain ->
                val attempted = chain.request()
                attempts.add(attempted)
                val firstAttempt = attempts.size == 1
                Response.Builder()
                    .request(attempted)
                    .protocol(Protocol.HTTP_1_1)
                    .code(if (firstAttempt) status else 307)
                    .message("Redirect")
                    .header("Location", if (firstAttempt) location else "/third")
                    .body("".toResponseBody())
                    .build()
            }
            .build()
        return client.newCall(request).execute().use { it.code to attempts }
    }
}
