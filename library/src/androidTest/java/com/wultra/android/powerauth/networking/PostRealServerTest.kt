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
import com.wultra.android.powerauth.networking.error.ApiErrorCode
import com.wultra.android.powerauth.networking.error.ApiHttpException
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Real-server integration tests for the [Api.post] pipeline.
 *
 * These tests require a valid `config.json` in the androidTest assets
 * and a running PowerAuth server. Tests are skipped when the config is absent.
 *
 * Mirrors Apple's WPNPostIntegrationTests (success + failure suites).
 */
@RunWith(AndroidJUnit4::class)
class PostRealServerTest {

    private fun loadConfigOrSkip(): TestConfiguration {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = TestConfiguration.load(context)
        assumeNotNull("Skipping: config.json not found in test assets", config)
        return config!!
    }

    // --- Transport test (jsonplaceholder) ---

    /**
     * Plain POST to jsonplaceholder.typicode.com.
     *
     * Mirrors Apple's `plainPost()` test. Verifies that the HTTP transport
     * layer works correctly by making a real network call.
     *
     * The test expects an error because jsonplaceholder does not return
     * the WPN envelope format (`{"status":"OK", ...}`).
     */
    @Test
    fun plainPostToJsonPlaceholder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val powerAuth = createDummyPowerAuth(context, "https://localhost/")

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
            Log.d("PostRealServerTest", "Got expected error: ${receivedError!!.e}")
            val httpException = receivedError!!.e as? ApiHttpException
            if (httpException != null) {
                // If it's an HTTP exception, the status code should be 201 (Created)
                assertEquals(201, httpException.code)
            }
            // Otherwise it's a parse error, which is also acceptable
        } else {
            // If parsing somehow succeeded, the status might be null/unexpected
            Log.d("PostRealServerTest", "Got response: ${receivedResponse?.status}")
        }
    }

    // --- Success tests (require config.json) ---

    /**
     * E2EE POST with application scope encryption.
     * Mirrors Apple's `e2eePost()` test.
     */
    @Test
    fun e2eePost() {
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val proxy = IntegrationProxy(config, context)
        proxy.initializePowerAuth()
        proxy.prepareActivation()

        try {
            val testApi = proxy.createApi(config.enrollmentServerOnboardingUrl)

            val latch = CountDownLatch(1)
            var receivedResponse: StatusResponse? = null
            var receivedError: ApiError? = null

            testApi.post(
                data = TestEndpoints.StartObjectRequest(
                    TestEndpoints.StartRequest(
                        identification = mapOf("clientNumber" to UUID.randomUUID().toString())
                    )
                ),
                endpoint = TestEndpoints.start,
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

            assertTrue("E2EE request should complete within 30s", latch.await(30, TimeUnit.SECONDS))
            assertNotNull("Should receive success response", receivedResponse)
            assertEquals(StatusResponse.Status.OK, receivedResponse!!.status)
        } finally {
            proxy.cleanup()
        }
    }

    /**
     * Signed POST with PowerAuth signature.
     * Mirrors Apple's `signedPost()` test.
     */
    @Test
    fun signedPost() {
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val proxy = IntegrationProxy(config, context)
        proxy.initializePowerAuth()
        proxy.prepareActivation()

        try {
            val testApi = proxy.createApi(config.operationsServerUrl)

            val latch = CountDownLatch(1)
            var receivedResponse: StatusResponse? = null
            var receivedError: ApiError? = null

            testApi.post(
                data = BaseRequest(),
                endpoint = TestEndpoints.history,
                authentication = PowerAuthAuthentication.possessionWithPassword(proxy.pin),
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

            assertTrue("Signed request should complete within 30s", latch.await(30, TimeUnit.SECONDS))
            assertNotNull("Should receive success response", receivedResponse)
            assertEquals(StatusResponse.Status.OK, receivedResponse!!.status)
        } finally {
            proxy.cleanup()
        }
    }

    // --- Failure tests (require config.json) ---

    /**
     * E2EE POST with activation scope without activation.
     * Mirrors Apple's `e2eePostUnactivated()` test.
     */
    @Test
    fun e2eePostUnactivated() {
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val proxy = IntegrationProxy(config, context)
        proxy.initializePowerAuth()
        // Intentionally NOT calling prepareActivation()

        try {
            val testApi = proxy.createApi(config.enrollmentServerOnboardingUrl)

            val latch = CountDownLatch(1)
            var receivedError: ApiError? = null

            testApi.post(
                data = BaseRequest(),
                endpoint = TestEndpoints.failingStart,
                listener = object : IApiCallResponseListener<StatusResponse> {
                    override fun onSuccess(result: StatusResponse) {
                        fail("Should not succeed for E2EE without activation")
                        latch.countDown()
                    }

                    override fun onFailure(error: ApiError) {
                        receivedError = error
                        latch.countDown()
                    }
                }
            )

            assertTrue("Request should complete within 30s", latch.await(30, TimeUnit.SECONDS))
            assertNotNull("Should receive error", receivedError)
        } finally {
            proxy.cleanup()
        }
    }

    /**
     * Signed POST with wrong PIN.
     * Mirrors Apple's `signedPostWrongPin()` test.
     */
    @Test
    fun signedPostWrongPin() {
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val proxy = IntegrationProxy(config, context)
        proxy.initializePowerAuth()
        proxy.prepareActivation()

        try {
            val testApi = proxy.createApi(config.operationsServerUrl)

            val latch = CountDownLatch(1)
            var receivedError: ApiError? = null

            testApi.post(
                data = BaseRequest(),
                endpoint = TestEndpoints.history,
                authentication = PowerAuthAuthentication.possessionWithPassword("0000"),
                listener = object : IApiCallResponseListener<StatusResponse> {
                    override fun onSuccess(result: StatusResponse) {
                        fail("Request should have failed with wrong PIN but succeeded")
                        latch.countDown()
                    }

                    override fun onFailure(error: ApiError) {
                        receivedError = error
                        latch.countDown()
                    }
                }
            )

            assertTrue("Request should complete within 30s", latch.await(30, TimeUnit.SECONDS))
            assertNotNull("Should receive error", receivedError)

            val httpException = receivedError!!.e as? ApiHttpException
            assertNotNull("Error should be ApiHttpException", httpException)
            assertNotNull("Should have error response", httpException!!.errorResponse)
            assertEquals(
                ApiErrorCode.POWERAUTH_AUTH_FAIL,
                httpException.errorResponse!!.responseObject.errorCode
            )
        } finally {
            proxy.cleanup()
        }
    }
}
