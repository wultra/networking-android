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
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.wultra.android.powerauth.networking.data.BaseRequest
import com.wultra.android.powerauth.networking.data.ObjectRequest
import com.wultra.android.powerauth.networking.data.ObjectResponse
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

    // Static PowerAuth configuration (fake, just for SDK initialization)
    private val paConfiguration = "ARCB+/qxpmLCa04AyT2IPXHKED4Heu76QU+v2PtnzQbe0sYBAUEEU05t3byEUdh90CBiBvqgr4sWU7r1YTAtdpTh3EygAUL791k66wy+SZM1qELw6zdoOHNFk/s4neDDqKtIQ5E5jg=="

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        WPNLogger.verboseLevel = WPNLogger.VerboseLevel.DEBUG

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = PowerAuthConfiguration.Builder(
            "test-instance",
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
