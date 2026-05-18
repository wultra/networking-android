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
import com.wultra.android.powerauth.networking.error.ApiErrorCode
import com.wultra.android.powerauth.networking.error.ApiHttpException
import com.wultra.android.powerauth.networking.support.IntegrationTestApi
import com.wultra.android.powerauth.networking.support.PowerAuthIntegrationProxy
import com.wultra.android.powerauth.networking.support.TestConfiguration
import com.wultra.android.powerauth.networking.support.TestEndpoints
import com.wultra.android.powerauth.networking.support.createDummyPowerAuth
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
 */
@RunWith(AndroidJUnit4::class)
class PostRealServerTest {

    private fun loadConfigOrSkip(): TestConfiguration {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = TestConfiguration.load(context)
        assumeNotNull("⚠\uFE0F Skipping: config.json not found in test assets", config)
        return config!!
    }

    // --- Transport test (jsonplaceholder) ---

    /**
     * Plain POST to jsonplaceholder.typicode.com.
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
            endpoint = TestEndpoints.posts,
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

        assertNull("Response should be null because JSONPlaceholder response does not match StatusResponse", receivedResponse?.status)

        assertNull("Request should not fail on transport level", receivedError)
    }

    // --- Success tests (require config.json) ---

    /**
     * E2EE POST with application scope encryption.
     */
    @Test
    fun e2eePost() {
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val proxy = PowerAuthIntegrationProxy(config, context)
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
     * Signed POST with PowerAuth authentication code.
     */
    @Test
    fun signedPost() {
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val proxy = PowerAuthIntegrationProxy(config, context)
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

    /**
     * Token-signed POST to operation/list endpoint.
     * Verifies [EndpointSignedWithToken] flow with automatic token management.
     */
    @Test
    fun tokenSignedPostOperationList() {
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val proxy = PowerAuthIntegrationProxy(config, context)
        proxy.initializePowerAuth()
        proxy.prepareActivation()

        try {
            val testApi = proxy.createApi(config.operationsServerUrl)

            val latch = CountDownLatch(1)
            var receivedResponse: StatusResponse? = null
            var receivedError: ApiError? = null

            testApi.post(
                data = BaseRequest(),
                endpoint = TestEndpoints.operationList,
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

            assertTrue("Token-signed request should complete within 30s", latch.await(30, TimeUnit.SECONDS))
            assertNotNull("Should receive success response", receivedResponse)
            assertEquals(StatusResponse.Status.OK, receivedResponse!!.status)
        } finally {
            proxy.cleanup()
        }
    }

    // --- Failure tests (require config.json) ---

    /**
     * E2EE POST with activation scope without activation.
     */
    @Test
    fun e2eePostUnactivated() {
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val proxy = PowerAuthIntegrationProxy(config, context)
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
                        latch.countDown()
                        fail("Should not succeed for E2EE without activation")
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
     * Signed POST with the wrong PIN.
     */
    @Test
    fun signedPostWrongPin() {
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val proxy = PowerAuthIntegrationProxy(config, context)
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
                        latch.countDown()
                        fail("Request should have failed with wrong PIN but succeeded")
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
        } catch (e: Exception) {
            fail("Unexpected exception: $e with message ${e.message} and cause ${e.cause}")
        } finally {
            proxy.cleanup()
        }
    }
}
