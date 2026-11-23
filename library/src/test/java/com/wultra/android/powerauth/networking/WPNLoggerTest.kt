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

package com.wultra.android.powerauth.networking

import com.wultra.android.powerauth.networking.log.HeaderBlockList
import com.wultra.android.powerauth.networking.log.WPNLogListener
import com.wultra.android.powerauth.networking.log.WPNLogger
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.BufferedSink
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Tests for WPNLogger configuration and log listener.
 * Uses `unitTests.isReturnDefaultValues = true` so android.util.Log calls succeed.
 */
class WPNLoggerTest {

    private var originalVerboseLevel: WPNLogger.VerboseLevel = WPNLogger.VerboseLevel.WARNING
    private var originalListener: WPNLogListener? = null
    private var originalLogHttpTraffic = false
    private var originalHeadersToSkip = WPNLogger.httpHeadersToSkip

    @Before
    fun setUp() {
        originalVerboseLevel = WPNLogger.verboseLevel
        originalListener = WPNLogger.logListener
        originalLogHttpTraffic = WPNLogger.logHttpTraffic
        originalHeadersToSkip = WPNLogger.httpHeadersToSkip
    }

    @After
    fun tearDown() {
        WPNLogger.verboseLevel = originalVerboseLevel
        WPNLogger.logListener = originalListener
        WPNLogger.logHttpTraffic = originalLogHttpTraffic
        WPNLogger.httpHeadersToSkip = originalHeadersToSkip
    }

    @Test
    fun `default verbose level is WARNING`() {
        assertEquals(WPNLogger.VerboseLevel.WARNING, originalVerboseLevel)
    }

    @Test
    fun `verbose level can be changed`() {
        WPNLogger.verboseLevel = WPNLogger.VerboseLevel.DEBUG
        assertEquals(WPNLogger.VerboseLevel.DEBUG, WPNLogger.verboseLevel)
    }

    @Test
    fun `default log listener is null`() {
        assertNull(originalListener)
    }

    @Test
    fun `log listener can be set`() {
        val listener = TestLogListener()
        WPNLogger.logListener = listener
        assertEquals(listener, WPNLogger.logListener)
    }

    @Test
    fun `HTTP logging requires opt-in even at DEBUG`() {
        assertFalse(originalLogHttpTraffic)
        val listener = TestLogListener()
        WPNLogger.logListener = listener
        WPNLogger.verboseLevel = WPNLogger.VerboseLevel.DEBUG

        val request = Request.Builder().url("https://example.test/").build()
        clientReturning { response(it) }.newCall(request).execute().close()

        assertTrue(listener.messages.isEmpty())
    }

    @Test
    fun `opt-in diagnostics omit payloads and protect credential headers`() {
        val listener = TestLogListener()
        WPNLogger.logListener = listener
        WPNLogger.verboseLevel = WPNLogger.VerboseLevel.DEBUG
        WPNLogger.logHttpTraffic = true
        WPNLogger.httpHeadersToSkip = HeaderBlockList().apply {
            add("Cookie")
            add("Authorization")
            remove("Authorization")
        }

        var bodyWrites = 0
        val request = Request.Builder()
            .url("https://demo-user:demo-pass@example.test:8443/private-path?value=private-query")
            .post(object : RequestBody() {
                override fun contentType() = null
                override fun writeTo(sink: BufferedSink) {
                    bodyWrites++
                    sink.writeUtf8("dummy-request-body")
                }
            })
            .header("aUtHoRiZaTiOn", "dummy-auth")
            .header("X-pOwErAuTh-Token", "dummy-token")
            .header("Cookie", "dummy-cookie")
            .header("X-Diagnostic", "visible")
            .build()
        val client = clientReturning {
            assertSame(request, it)
            response(it, "dummy-response-body").newBuilder()
                .header("sEt-CoOkIe", "dummy-session")
                .header("X-PowerAuth-Encryption", "dummy-encryption")
                .build()
        }

        client.newCall(request).execute().use {
            assertEquals("dummy-response-body", it.body?.string())
        }

        assertEquals(0, bodyWrites)
        val messages = listener.messages.joinToString("\n") { it.second }
        assertTrue("POST - https://example.test:8443" in messages)
        assertTrue("Status code: 200" in messages)
        assertTrue("X-Diagnostic: visible" in messages)
        assertTrue("aUtHoRiZaTiOn: <redacted>" in messages)
        assertTrue("X-pOwErAuTh-Token: <redacted>" in messages)
        assertTrue("sEt-CoOkIe: <redacted>" in messages)
        assertTrue("X-PowerAuth-Encryption: <redacted>" in messages)
        assertFalse("\n  - Cookie:" in messages)
        listOf("dummy-", "demo-user", "demo-pass", "private-path", "private-query").forEach {
            assertFalse("Unexpected log content: $it", it in messages)
        }
    }

    @Test
    fun `listener cannot bypass verbosity and failure log omits exception message`() {
        val listener = TestLogListener(followVerboseLevel = false)
        WPNLogger.logListener = listener
        WPNLogger.logHttpTraffic = true
        val failure = IOException("dummy-failure-detail")
        val client = clientReturning { throw failure }
        val request = Request.Builder().url("https://example.test/").build()

        WPNLogger.verboseLevel = WPNLogger.VerboseLevel.OFF
        assertSame(failure, assertThrows(IOException::class.java) { client.newCall(request).execute() })
        assertTrue(listener.messages.isEmpty())

        WPNLogger.verboseLevel = WPNLogger.VerboseLevel.ERROR
        assertSame(failure, assertThrows(IOException::class.java) { client.newCall(request).execute() })
        val (level, message) = listener.messages.single()
        assertEquals("error", level)
        assertTrue("java.io.IOException" in message)
        assertFalse("dummy-failure-detail" in message)
    }

    @Test
    fun `VerboseLevel enum ordering`() {
        val levels = WPNLogger.VerboseLevel.entries
        assertEquals(5, levels.size)
        assertEquals(WPNLogger.VerboseLevel.OFF, levels[0])
        assertEquals(WPNLogger.VerboseLevel.ERROR, levels[1])
        assertEquals(WPNLogger.VerboseLevel.WARNING, levels[2])
        assertEquals(WPNLogger.VerboseLevel.INFO, levels[3])
        assertEquals(WPNLogger.VerboseLevel.DEBUG, levels[4])
    }

    @Test
    fun `VerboseLevel ordinal increases with verbosity`() {
        assertTrue(WPNLogger.VerboseLevel.OFF.ordinal < WPNLogger.VerboseLevel.ERROR.ordinal)
        assertTrue(WPNLogger.VerboseLevel.ERROR.ordinal < WPNLogger.VerboseLevel.WARNING.ordinal)
        assertTrue(WPNLogger.VerboseLevel.WARNING.ordinal < WPNLogger.VerboseLevel.INFO.ordinal)
        assertTrue(WPNLogger.VerboseLevel.INFO.ordinal < WPNLogger.VerboseLevel.DEBUG.ordinal)
    }

    private fun clientReturning(block: (Request) -> Response): OkHttpClient {
        return OkHttpClient.Builder().apply {
            WPNLogger.configure(this)
            addInterceptor(Interceptor { block(it.request()) })
        }.build()
    }

    private fun response(request: Request, body: String = "") = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(body.toResponseBody())
        .build()

    private class TestLogListener(
        @Deprecated("Listeners always follow WPNLogger.verboseLevel.")
        override val followVerboseLevel: Boolean = true
    ) : WPNLogListener {
        val messages = mutableListOf<Pair<String, String>>()

        override fun error(message: String) { messages.add("error" to message) }
        override fun warning(message: String) { messages.add("warning" to message) }
        override fun info(message: String) { messages.add("info" to message) }
        override fun debug(message: String) { messages.add("debug" to message) }
    }
}
