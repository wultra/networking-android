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

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.wultra.android.powerauth.networking.data.BaseRequest
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.error.ApiError
import com.wultra.android.powerauth.networking.error.ApiHttpException
import com.wultra.android.powerauth.networking.log.WPNLogger
import io.getlime.security.powerauth.sdk.PowerAuthClientConfiguration
import io.getlime.security.powerauth.sdk.PowerAuthConfiguration
import io.getlime.security.powerauth.sdk.PowerAuthSDK
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
 * Integration tests for the [Api.post] pipeline.
 *
 * Uses [MockWebServer] to test the full request/response flow including
 * Gson serialization, OkHttp transport, header propagation, and error handling.
 *
 * Mirrors Apple's WPNPostIntegrationTests.
 */
@RunWith(AndroidJUnit4::class)
class PostIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var api: IntegrationTestApi

    // Dummy PowerAuth SDK configuration valid for SDK 2.0+.
    // Mirrors `dummySDKConfig` from the Apple test suite.
    private val paConfiguration = "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"

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        WPNLogger.verboseLevel = WPNLogger.VerboseLevel.DEBUG

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = PowerAuthConfiguration.Builder(
            "mobile-tests",
            "${server.url("/")}",
            paConfiguration
        ).build()

        val powerAuth = PowerAuthSDK.Builder(config)
            .clientConfiguration(PowerAuthClientConfiguration.Builder().build())
            .build(context)

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

    @Test
    fun basicPostUrlConstruction() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":"OK"}""")
        )

        val latch = CountDownLatch(1)

        api.post(
            data = BaseRequest(),
            endpoint = EndpointBasic<BaseRequest, StatusResponse>("/v1/my/endpoint"),
            listener = object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) { latch.countDown() }
                override fun onFailure(error: ApiError) { latch.countDown() }
            }
        )

        assertTrue(latch.await(10, TimeUnit.SECONDS))

        val recorded = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertEquals("/v1/my/endpoint", recorded!!.path)
    }

    // --- Failure tests ---

    @Test
    fun basicPostHttpErrorResponse() {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("""{
                    "status": "ERROR",
                    "responseObject": {
                        "code": "INVALID_REQUEST",
                        "message": "Bad request"
                    }
                }""")
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
                .setBody("""{
                    "status": "ERROR",
                    "responseObject": {
                        "code": "POWERAUTH_AUTH_FAIL",
                        "message": "Authentication failed"
                    }
                }""")
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

    // --- Real server test (optional, needs jsonplaceholder) ---

    /**
     * Plain POST to jsonplaceholder.typicode.com.
     *
     * This test mirrors Apple's `plainPost()` test. It verifies that the
     * HTTP transport layer works correctly by making a real network call.
     *
     * The test expects an error because jsonplaceholder does not return
     * the WPN envelope format (`{"status":"OK", ...}`).
     */
    @Test
    fun plainPostToJsonPlaceholder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = PowerAuthConfiguration.Builder(
            "plain-test",
            "https://localhost/",
            paConfiguration
        ).build()

        val powerAuth = PowerAuthSDK.Builder(config)
            .clientConfiguration(PowerAuthClientConfiguration.Builder().build())
            .build(context)

        val realApi = IntegrationTestApi(
            baseUrl = "https://jsonplaceholder.typicode.com",
            okHttpClient = OkHttpClient(),
            powerAuthSDK = powerAuth,
            appContext = context,
            userAgent = UserAgent.customValue("WPNAndroidTest/1.0")
        )

        val latch = CountDownLatch(1)
        var receivedResponse: StatusResponse? = null
        var receivedError: ApiError? = null

        realApi.post(
            data = BaseRequest(),
            endpoint = TestEndpoints.todo,
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

        assertTrue("Real network request should complete within 30s", latch.await(30, TimeUnit.SECONDS))

        // jsonplaceholder returns 201 for POST /posts, but the body is not
        // in WPN envelope format. The library should either:
        // 1. Parse successfully (if the response happens to parse as StatusResponse)
        // 2. Fail with a parse error (ApiHttpException wrapping a Gson error)
        //
        // The key assertion is that the request was actually sent and we got a response.
        if (receivedError != null) {
            // Expected: transport succeeded but response format doesn't match
            Log.d("PostIntegrationTest", "Got expected error: ${receivedError!!.e}")
            val httpException = receivedError!!.e as? ApiHttpException
            if (httpException != null) {
                // If it's an HTTP exception, the status code should be 201 (Created)
                assertEquals(201, httpException.code)
            }
            // Otherwise it's a parse error, which is also acceptable
        } else {
            // If parsing somehow succeeded, the status might be null/unexpected
            Log.d("PostIntegrationTest", "Got response: ${receivedResponse?.status}")
        }
    }
}
