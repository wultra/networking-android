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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.wultra.android.powerauth.networking.data.BaseRequest
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.error.ApiError
import com.wultra.android.powerauth.networking.error.ApiHttpException
import com.wultra.android.powerauth.networking.log.WPNLogger
import com.wultra.android.powerauth.networking.support.IntegrationTestApi
import com.wultra.android.powerauth.networking.support.createDummyPowerAuth
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * MockWebServer-based tests for the [Api.post] pipeline.
 *
 * Tests the full request/response flow including Gson serialization,
 * OkHttp transport, header propagation, and error handling — all offline.
 */
@RunWith(AndroidJUnit4::class)
class PostMockWebServerTest {

    private lateinit var server: MockWebServer
    private lateinit var api: IntegrationTestApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        WPNLogger.verboseLevel = WPNLogger.VerboseLevel.DEBUG

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val powerAuth = createDummyPowerAuth(context, server.url("/").toString())

        api = IntegrationTestApi(
            baseUrl = server.url("/").toString(),
            okHttpClient = OkHttpClient(),
            powerAuthSDK = powerAuth,
            appContext = context,
            userAgent = UserAgent.customValue("TestAgent/1.0")
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // --- Success tests ---

    @Test
    fun basicPostSuccessResponse() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"status":"OK"}""")
        )

        val latch = CountDownLatch(1)
        var receivedResponse: StatusResponse? = null
        var receivedError: ApiError? = null

        api.post(
            data = BaseRequest(),
            endpoint = EndpointBasic<BaseRequest, StatusResponse>("/api/test"),
            listener = object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) {
                    receivedResponse = result
                    latch.countDown()
                }

                override fun onFailure(error: ApiError) {
                    receivedError = error
                    latch.countDown()
                }
            }
        )

        assertTrue("Request should complete within 10s", latch.await(10, TimeUnit.SECONDS))
        assertNotNull("Should receive success response", receivedResponse)
        assertEquals(StatusResponse.Status.OK, receivedResponse!!.status)

        // Verify the request was sent correctly
        val recorded = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull("Server should receive a request", recorded)
        assertEquals("POST", recorded!!.method)
        assertEquals("/api/test", recorded.path)
        assertEquals("application/json; charset=UTF-8", recorded.getHeader("Content-Type"))
    }

    @Test
    fun basicPostWithCustomHeaders() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"status":"OK"}""")
        )

        val latch = CountDownLatch(1)
        val headers = hashMapOf(
            "X-Custom-Header" to "custom-value",
            "X-Request-ID" to "test-123"
        )

        api.post(
            data = BaseRequest(),
            endpoint = EndpointBasic<BaseRequest, StatusResponse>("/api/test"),
            headers = headers,
            listener = object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) {
                    latch.countDown()
                }

                override fun onFailure(error: ApiError) {
                    latch.countDown()
                }
            }
        )

        assertTrue(latch.await(10, TimeUnit.SECONDS))

        val recorded = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertEquals("custom-value", recorded!!.getHeader("X-Custom-Header"))
        assertEquals("test-123", recorded.getHeader("X-Request-ID"))
    }

    @Test
    fun basicPostSendsAcceptLanguageHeader() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":"OK"}""")
        )

        api.acceptLanguage = "cs"

        val latch = CountDownLatch(1)

        api.post(
            data = BaseRequest(),
            endpoint = EndpointBasic<BaseRequest, StatusResponse>("/api/test"),
            listener = object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) { latch.countDown() }
                override fun onFailure(error: ApiError) { latch.countDown() }
            }
        )

        assertTrue(latch.await(10, TimeUnit.SECONDS))

        val recorded = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertEquals("cs", recorded!!.getHeader("Accept-Language"))
    }

    @Test
    fun basicPostSendsUserAgentHeader() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":"OK"}""")
        )

        val latch = CountDownLatch(1)

        api.post(
            data = BaseRequest(),
            endpoint = EndpointBasic<BaseRequest, StatusResponse>("/api/test"),
            listener = object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) { latch.countDown() }
                override fun onFailure(error: ApiError) { latch.countDown() }
            }
        )

        assertTrue(latch.await(10, TimeUnit.SECONDS))

        val recorded = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertEquals("TestAgent/1.0", recorded!!.getHeader("User-Agent"))
    }

    @Test
    fun basicPostSendsRequestBody() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":"OK"}""")
        )

        val latch = CountDownLatch(1)

        api.post(
            data = BaseRequest(),
            endpoint = EndpointBasic<BaseRequest, StatusResponse>("/api/test"),
            listener = object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) { latch.countDown() }
                override fun onFailure(error: ApiError) { latch.countDown() }
            }
        )

        assertTrue(latch.await(10, TimeUnit.SECONDS))

        val recorded = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(recorded)
        val body = recorded!!.body.readUtf8()
        assertEquals("{}", body) // BaseRequest serializes to empty JSON object
    }

    // --- Failure tests ---

    @Test
    fun basicPostHttpErrorResponse() {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{
                        "status": "ERROR",
                        "responseObject": {
                            "code": "INVALID_REQUEST",
                            "message": "Bad request"
                        }
                    }"""
                )
        )

        val latch = CountDownLatch(1)
        var receivedError: ApiError? = null

        api.post(
            data = BaseRequest(),
            endpoint = EndpointBasic<BaseRequest, StatusResponse>("/api/test"),
            listener = object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) {
                    fail("Should not succeed for HTTP 400")
                    latch.countDown()
                }

                override fun onFailure(error: ApiError) {
                    receivedError = error
                    latch.countDown()
                }
            }
        )

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        assertNotNull("Should receive error", receivedError)

        val httpException = receivedError!!.e as? ApiHttpException
        assertNotNull("Error should be ApiHttpException", httpException)
        assertEquals(400, httpException!!.code)
        assertNotNull("Should have error response", httpException.errorResponse)
        assertEquals(
            "INVALID_REQUEST",
            httpException.errorResponse!!.responseObject.code
        )
    }

    @Test
    fun basicPostServerErrorResponse() {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"status":"ERROR","responseObject":{"code":"ERROR_GENERIC","message":"Internal error"}}""")
        )

        val latch = CountDownLatch(1)
        var receivedError: ApiError? = null

        api.post(
            data = BaseRequest(),
            endpoint = EndpointBasic<BaseRequest, StatusResponse>("/api/test"),
            listener = object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) {
                    fail("Should not succeed for HTTP 500")
                    latch.countDown()
                }

                override fun onFailure(error: ApiError) {
                    receivedError = error
                    latch.countDown()
                }
            }
        )

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        assertNotNull(receivedError)

        val httpException = receivedError!!.e as? ApiHttpException
        assertNotNull(httpException)
        assertEquals(500, httpException!!.code)
    }

    @Test
    fun basicPostMalformedJsonResponse() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("not valid json{{{")
        )

        val latch = CountDownLatch(1)
        var receivedError: ApiError? = null

        api.post(
            data = BaseRequest(),
            endpoint = EndpointBasic<BaseRequest, StatusResponse>("/api/test"),
            listener = object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) {
                    // Depending on Gson behavior, malformed JSON might result in
                    // a partially parsed object or trigger an error.
                    latch.countDown()
                }

                override fun onFailure(error: ApiError) {
                    receivedError = error
                    latch.countDown()
                }
            }
        )

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        // Malformed JSON on a 200 response should result in a parse error
        // wrapped in ApiHttpException
        assertNotNull("Malformed JSON should cause an error", receivedError)
    }

    @Test
    fun basicPostEmptyBody() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("")
        )

        val latch = CountDownLatch(1)
        var receivedError: ApiError? = null

        api.post(
            data = BaseRequest(),
            endpoint = EndpointBasic<BaseRequest, StatusResponse>("/api/test"),
            listener = object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) {
                    latch.countDown()
                }

                override fun onFailure(error: ApiError) {
                    receivedError = error
                    latch.countDown()
                }
            }
        )

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        // Empty body should cause a parse error
        assertNotNull("Empty body should cause an error", receivedError)
    }

    @Test
    fun basicPostAuthenticationFailureErrorCode() {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{
                        "status": "ERROR",
                        "responseObject": {
                            "code": "POWERAUTH_AUTH_FAIL",
                            "message": "Authentication failed"
                        }
                    }"""
                )
        )

        val latch = CountDownLatch(1)
        var receivedError: ApiError? = null

        api.post(
            data = BaseRequest(),
            endpoint = EndpointBasic<BaseRequest, StatusResponse>("/api/test"),
            listener = object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) {
                    fail("Should not succeed for HTTP 401")
                    latch.countDown()
                }

                override fun onFailure(error: ApiError) {
                    receivedError = error
                    latch.countDown()
                }
            }
        )

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        assertNotNull(receivedError)

        val httpException = receivedError!!.e as? ApiHttpException
        assertNotNull(httpException)
        assertEquals(401, httpException!!.code)
        assertNotNull(httpException.errorResponse)
        assertEquals(
            "POWERAUTH_AUTH_FAIL",
            httpException.errorResponse!!.responseObject.code
        )
    }

    @Test
    fun basicPostWithErrorStatus() {
        // Test when server returns 200 but with ERROR status in body
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"status":"ERROR","responseObject":{"code":"INVALID_REQUEST","message":"Missing field"}}""")
        )

        val latch = CountDownLatch(1)
        var receivedResponse: StatusResponse? = null

        api.post(
            data = BaseRequest(),
            endpoint = EndpointBasic<BaseRequest, StatusResponse>("/api/test"),
            listener = object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) {
                    receivedResponse = result
                    latch.countDown()
                }

                override fun onFailure(error: ApiError) {
                    // HTTP 200 with ERROR status in body still counts as "success" at HTTP level
                    latch.countDown()
                }
            }
        )

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        // With HTTP 200, the response is parsed as success
        // The ERROR status is in the response object itself
        assertNotNull("Should receive parsed response", receivedResponse)
        assertEquals(StatusResponse.Status.ERROR, receivedResponse!!.status)
    }
}
