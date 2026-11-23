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

package com.wultra.android.powerauth.networking.log

import android.util.Log
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Logger provides simple logging facility.
 *
 * Logs are written with "WPN" tag to standard [android.util.Log] logger.
 * Set [logListener] to receive messages independently of Android logging.
 */
class WPNLogger {

    /** Level of the log which should be effectively logged. */
    enum class VerboseLevel {
        /** Silences all messages. */
        OFF,
        /** Only errors will be printed into the log. */
        ERROR,
        /** Errors and warnings will be printed into the log. */
        WARNING,
        /** Info logs, errors and warnings will be printed into the log. */
        INFO,
        /** All messages will be printed into the log. */
        DEBUG
    }

    companion object {

        /** Current verbose level. */
        @Volatile
        @JvmStatic var verboseLevel = VerboseLevel.WARNING

        /** Listener that can tap into the log stream and process it on its own. */
        @Volatile
        @JvmStatic var logListener: WPNLogListener? = null

        /** Whether to log HTTP metadata. Disabled by default; bodies are never logged by the HTTP interceptor. */
        @Volatile
        @JvmStatic var logHttpTraffic = false

        /**
         * Headers to skip when logging HTTP traffic.
         *
         * Authorization, Proxy-Authorization, Cookie, Set-Cookie and X-PowerAuth-* values
         * are always redacted if the header is not skipped.
         *
         * Note that all headers are transformed to lowercase variant when added.
         *
         * Default headers to skip are:
         * ```
         * "accept-language", "content-type", "content-length", "transfer-encoding", "date", "server", "user-agent",
         * "connection", "x-content-type-options", "x-xss-protection", "cache-control", "pragma", "expires", "x-frame-options", "vary"
         * ```
         */
        @JvmStatic var httpHeadersToSkip = HeaderBlockList()

        private val tag = "WPN"

        private fun log(valueFn: () -> String, allowedLevel: VerboseLevel, logFn: (String?, String) -> Unit, listenerFn: (WPNLogListener, String) -> Unit) {
            val logToAndroid = verboseLevel.ordinal >= allowedLevel.ordinal
            val listener = logListener
            if (!logToAndroid && listener?.followVerboseLevel != false) return
            val log = valueFn()
            if (logToAndroid) logFn(tag, log)
            if (listener != null) listenerFn(listener, log)
        }

        internal fun d(message: String) {
            d { message }
        }

        internal fun d(fn: () -> String) {
            log(fn, VerboseLevel.DEBUG, Log::d, WPNLogListener::debug)
        }

        internal fun w(message: String) {
            w { message }
        }

        internal fun w(fn: () -> String) {
            log(fn, VerboseLevel.WARNING, Log::w, WPNLogListener::warning)
        }

        internal fun i(message: String) {
            i { message }
        }

        internal fun i(fn: () -> String) {
            log(fn, VerboseLevel.INFO, Log::i, WPNLogListener::info)
        }

        internal fun e(message: String) {
            e { message }
        }

        internal fun e(fn: () -> String) {
            log(fn, VerboseLevel.ERROR, Log::e, WPNLogListener::error)
        }

        internal fun configure(builder: OkHttpClient.Builder) {
            val interceptor = Interceptor { chain ->
                val request = chain.request()
                if (!logHttpTraffic) return@Interceptor chain.proceed(request)

                i {
                    "\n<--- WPN REQUEST ---" +
                        "\n- Origin: ${request.method} - ${request.url.originForLog()}" +
                        "\n- Headers: ${request.headers.forLog(httpHeadersToSkip.toList())}"
                }

                val response: Response
                try {
                    response = chain.proceed(request)
                } catch (e: IOException) {
                    e {
                        "\n--- WPN REQUEST FAILED --->" +
                            "\n- Origin: ${request.method} - ${request.url.originForLog()}" +
                            "\n- Error: ${e.javaClass.name}"
                    }
                    throw e
                }

                i {
                    "\n--- WPN RESPONSE --->" +
                        "\n- Origin: ${response.request.method} - ${response.request.url.originForLog()}" +
                        "\n- Status code: ${response.code}" +
                        "\n- Headers: ${response.headers.forLog(httpHeadersToSkip.toList())}"
                }
                response
            }
            builder.addInterceptor(interceptor)
        }
    }
}

/**
 * Headers to skip when logging.
 *
 * Note that all headers are transformed to lowercase variant when added.
 * This class is thread safe.
 *
 * Default headers to skip are:
 * ```
 * "accept-language", "content-type", "content-length", "transfer-encoding", "date", "server", "user-agent",
 * "connection", "x-content-type-options", "x-xss-protection", "cache-control", "pragma", "expires", "x-frame-options", "vary"
 * ```
 */
class HeaderBlockList {

    private val headersToSkip = CopyOnWriteArrayList(
        listOf(
            "accept-language", "content-type", "content-length", "transfer-encoding", "date", "server", "user-agent",
            "connection", "x-content-type-options", "x-xss-protection", "cache-control", "pragma", "expires", "x-frame-options", "vary"
        )
    )

    fun add(element: String): Boolean {
        return headersToSkip.add(element.lowercase())
    }

    fun addAll(elements: Collection<String>): Boolean {
        return headersToSkip.addAll(elements.map { it.lowercase() })
    }

    fun remove(element: String): Boolean {
        return headersToSkip.remove(element.lowercase())
    }

    fun removeAll(elements: Collection<String>): Boolean {
        return headersToSkip.removeAll(elements.map { it.lowercase() }.toSet())
    }

    fun toList(): List<String> = headersToSkip.toList()
}

private fun HttpUrl.originForLog(): String {
    val originHost = if (':' in host) "[$host]" else host
    return "$scheme://$originHost:$port"
}

private val sensitiveHeaders = setOf("authorization", "proxy-authorization", "cookie", "set-cookie")

private fun Headers.forLog(skip: List<String>): String {
    val result = StringBuilder()
    var skipped = 0
    for (i in 0 until size) {
        val name = name(i)
        val lowerName = name.lowercase()
        if (lowerName in skip) {
            skipped += 1
            continue
        }
        val displayValue = if (lowerName in sensitiveHeaders || lowerName.startsWith("x-powerauth-")) {
            "<redacted>"
        } else {
            value(i)
        }
        result.append("\n  - $name: $displayValue")
    }

    result.insert(0, "$skipped filtered out")
    return result.toString()
}
