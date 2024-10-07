package com.wultra.android.powerauth.networking.log

import android.util.Log
import com.wultra.android.powerauth.networking.ECIESInterceptor
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.Buffer
import java.lang.StringBuilder
import java.net.URL

/**
 * Logger provides simple logging facility.
 *
 * Logs are written with "WPN" tag to standard [android.util.Log] logger.
 * You can set [logListener] to start listening on the
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
        @JvmStatic var verboseLevel = VerboseLevel.WARNING

        /** Listener that can tap into the log stream and process it on it's own. */
        @JvmStatic var logListener: WPNLogListener? = null

        /** If HTTP traffic should be logged. */
        @JvmStatic var logHttpTraffic = true

        /**
         * Headers to skip when logging HTTP traffic.
         *
         * Note that all headers are transformed to lowercase variant when added.
         *
         * Default headers to skip are:
         * ```
         * "accept-language", "content-type", "content-length", "accept-language", "transfer-encoding", "date", "server", "user-agent",
         * "connection", "x-content-type-options", "x-xss-protection", "cache-control", "pragma", "expires", "x-frame-options", "vary"
         * ```
         */
        @JvmStatic var httpHeadersToSkip = HeaderBlockList()

        private val tag = "WPN"

        private fun log(valueFn: () -> String, allowedLevel: VerboseLevel, logFn: (String?, String) -> Unit, listenerFn: ((String) -> Unit)?) {
            val shouldProcess = verboseLevel.ordinal >= allowedLevel.ordinal
            val log = if (shouldProcess || logListener?.followVerboseLevel == false) valueFn() else return
            if (shouldProcess) {
                logFn(tag, log)
            }
            listenerFn?.invoke(log)
        }

        internal fun d(message: String) {
            d { message }
        }

        internal fun d(fn: () -> String) {
            log(fn, VerboseLevel.DEBUG, Log::d, logListener?.let { it::debug })
        }

        internal fun w(message: String) {
            w { message }
        }

        internal fun w(fn: () -> String) {
            log(fn, VerboseLevel.WARNING, Log::w, logListener?.let { it::warning })
        }

        internal fun i(message: String) {
            i { message }
        }

        internal fun i(fn: () -> String) {
            log(fn, VerboseLevel.INFO, Log::i, logListener?.let { it::info })
        }

        internal fun e(message: String) {
            e { message }
        }

        internal fun e(fn: () -> String) {
            log(fn, VerboseLevel.ERROR, Log::e, logListener?.let { it::error })
        }

        internal fun configure(builder: OkHttpClient.Builder) {
            builder.addInterceptor(
                object: ECIESInterceptor {
                    override fun encryptedResponseReceived(url: URL, decrypted: ByteArray) {
                        if (logHttpTraffic) {
                            d {
                                "- Decrypted response ($url) - ${decrypted.decodeToString()}"
                            }
                        }
                    }

                    override fun intercept(chain: Interceptor.Chain): Response {

                        val request = chain.request()

                        if (!logHttpTraffic) {
                            return chain.proceed(request)
                        }

                        i {
                            "\n<--- WPN REQUEST ---" +
                                "\n- URL: ${request.method} - ${request.url}" +
                                "\n- Headers: ${request.headers.forLog(httpHeadersToSkip.toList())}"
                        }

                        try {
                            d {
                                val buffer = Buffer()
                                request.newBuilder().build().body?.writeTo(buffer)
                                "- Body: ${buffer.readUtf8()}"
                            }
                        } catch (e: Throwable) {
                            e("- Failed to parse request body: ${e.message}")
                        }

                        val response: Response

                        try {
                            response = chain.proceed(request)
                        } catch (e: Throwable) {
                            e {
                                "\n--- WPN REQUEST FAILED --->" +
                                    "\n- URL: ${request.method} - ${request.url}" +
                                    "\n- Error: $e"
                            }
                            throw e
                        }

                        i {
                            "\n--- WPN RESPONSE --->" +
                                "\n- URL: ${response.request.method} - ${
                                    response.request.url
                                }" +
                                "\n- Status code: ${response.code}" +
                                "\n- Headers: ${response.headers.forLog(httpHeadersToSkip.toList())}"
                        }

                        try {
                            d {
                                "Body: ${
                                    response.peekBody(10_000).string()
                                }" // allow max 10 KB of text
                            }
                        } catch (e: Throwable) {
                            e("- Failed to parse response body: ${e.message}")
                        }

                        return response
                    }
                }
            )
        }
    }
}

/**
 * Headers to skip when logging.
 *
 * Note that all headers are transformed to lowercase variant when added.
 *
 * Default headers to skip are:
 * ```
 * "accept-language", "content-type", "content-length", "accept-language", "transfer-encoding", "date", "server", "user-agent",
 * "connection", "x-content-type-options", "x-xss-protection", "cache-control", "pragma", "expires", "x-frame-options", "vary"
 * ```
 */
class HeaderBlockList {

    private val headersToSkp = mutableListOf(
        "accept-language", "content-type", "content-length", "accept-language", "transfer-encoding", "date", "server", "user-agent",
        "connection", "x-content-type-options", "x-xss-protection", "cache-control", "pragma", "expires", "x-frame-options", "vary"
    )

    fun add(element: String): Boolean {
        return headersToSkp.add(element.lowercase())
    }

    fun addAll(elements: Collection<String>): Boolean {
        return headersToSkp.addAll(elements.map { it.lowercase() })
    }

    fun remove(element: String): Boolean {
        return headersToSkp.remove(element.lowercase())
    }

    fun removeAll(elements: Collection<String>): Boolean {
        return headersToSkp.removeAll(elements.map { it.lowercase() }.toSet())
    }

    fun toList() = headersToSkp.toList()
}

private fun Headers.forLog(skip: List<String>): String {
    val result = StringBuilder()
    var skipped = 0
    for (i in 0 until size) {
        val name = name(i)
        if (!skip.contains(name.lowercase())) {
            result.append("\n  - ${name}: ${value(i)}")
        } else {
            skipped += 1
        }
    }
    result.insert(0, "$skipped filtered out")
    return result.toString()
}
