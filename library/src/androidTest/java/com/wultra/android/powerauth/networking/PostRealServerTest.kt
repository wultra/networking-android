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
import android.util.Log
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

    companion object {
        private const val TAG = "PostRealServerTest"
    }

    private fun loadConfigOrSkip(): TestConfiguration {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = TestConfiguration.load(context)
        assumeNotNull("⚠\uFE0F Skipping: config.json not found in test assets", config)
        return config!!
    }

    private fun logErrorDetails(testName: String, error: ApiError?) {
        if (error == null) {
            Log.d(TAG, "[$testName] error is null")
            return
        }
        Log.e(TAG, "[$testName] ApiError: $error")
        Log.e(TAG, "[$testName]   exception class: ${error.e?.javaClass?.name}")
        Log.e(TAG, "[$testName]   exception message: ${error.e?.message}")
        Log.e(TAG, "[$testName]   exception cause: ${error.e?.cause}")
        Log.e(TAG, "[$testName]   exception cause message: ${error.e?.cause?.message}")
        Log.e(TAG, "[$testName]   exception cause cause: ${error.e?.cause?.cause}")
        Log.e(TAG, "[$testName]   exception cause cause message: ${error.e?.cause?.cause?.message}")

        // If it's an ApiHttpException, log HTTP details
        val httpException = error.e as? ApiHttpException
        if (httpException != null) {
            Log.e(TAG, "[$testName]   HTTP code: (ApiHttpException found)")
            Log.e(TAG, "[$testName]   errorResponse: ${httpException.errorResponse}")
        }

        // Log full stack trace
        val sw = java.io.StringWriter()
        error.e?.printStackTrace(java.io.PrintWriter(sw))
        Log.e(TAG, "[$testName]   Full stack trace:\n$sw")
    }

    private fun logPowerAuthState(testName: String, proxy: PowerAuthIntegrationProxy) {
        val pa = proxy.powerAuth
        if (pa == null) {
            Log.w(TAG, "[$testName] PowerAuth SDK is null!")
            return
        }
        Log.d(TAG, "[$testName] PowerAuth state:")
        Log.d(TAG, "[$testName]   hasValidActivation: ${pa.hasValidActivation()}")
        Log.d(TAG, "[$testName]   canStartActivation: ${pa.canStartActivation()}")
        Log.d(TAG, "[$testName]   hasPendingActivation: ${pa.hasPendingActivation()}")
        Log.d(TAG, "[$testName]   activationIdentifier: ${pa.activationIdentifier}")
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
        val testName = "e2eePost"
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Log.d(TAG, "[$testName] Starting test")
        Log.d(TAG, "[$testName] enrollmentServerOnboardingUrl: ${config.enrollmentServerOnboardingUrl}")
        Log.d(TAG, "[$testName] enrollmentServerUrl: ${config.enrollmentServerUrl}")
        Log.d(TAG, "[$testName] cloudServerUrl: ${config.cloudServerUrl}")

        val proxy = PowerAuthIntegrationProxy(config, context)
        Log.d(TAG, "[$testName] Calling initializePowerAuth()...")
        proxy.initializePowerAuth()
        Log.d(TAG, "[$testName] initializePowerAuth() done")

        Log.d(TAG, "[$testName] Calling prepareActivation()...")
        proxy.prepareActivation()
        Log.d(TAG, "[$testName] prepareActivation() done")
        logPowerAuthState(testName, proxy)

        try {
            val testApi = proxy.createApi(config.enrollmentServerOnboardingUrl)
            Log.d(TAG, "[$testName] API created with baseUrl: ${config.enrollmentServerOnboardingUrl}")

            val latch = CountDownLatch(1)
            var receivedResponse: StatusResponse? = null
            var receivedError: ApiError? = null

            val requestId = UUID.randomUUID().toString()
            Log.d(TAG, "[$testName] Sending E2EE POST with requestId=$requestId")

            testApi.post(
                data = TestEndpoints.StartObjectRequest(
                    TestEndpoints.StartRequest(
                        identification = mapOf("clientNumber" to requestId)
                    )
                ),
                endpoint = TestEndpoints.start,
                listener = object : IApiCallResponseListener<StatusResponse> {
                    override fun onSuccess(result: StatusResponse) {
                        Log.d(TAG, "[$testName] onSuccess: status=${result.status}")
                        receivedResponse = result
                        latch.countDown()
                    }

                    override fun onFailure(error: ApiError) {
                        Log.e(TAG, "[$testName] onFailure called")
                        logErrorDetails(testName, error)
                        receivedError = error
                        latch.countDown()
                    }
                }
            )

            val completed = latch.await(30, TimeUnit.SECONDS)
            Log.d(TAG, "[$testName] Latch completed=$completed, response=$receivedResponse, error=$receivedError")
            assertTrue("E2EE request should complete within 30s", completed)
            if (receivedError != null) {
                logErrorDetails(testName, receivedError)
                fail("Expected success but got error: ${receivedError!!.e?.message} cause: ${receivedError!!.e?.cause?.message}")
            }
            assertNotNull("Should receive success response", receivedResponse)
            assertEquals(StatusResponse.Status.OK, receivedResponse!!.status)
        } finally {
            proxy.cleanup()
        }
    }

    /**
     * Authenticated POST with PowerAuth authentication code.
     */
    @Test
    fun authenticatedPost() {
        val testName = "authenticatedPost"
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Log.d(TAG, "[$testName] Starting test")
        Log.d(TAG, "[$testName] operationsServerUrl: ${config.operationsServerUrl}")
        Log.d(TAG, "[$testName] enrollmentServerUrl: ${config.enrollmentServerUrl}")
        Log.d(TAG, "[$testName] cloudServerUrl: ${config.cloudServerUrl}")

        val proxy = PowerAuthIntegrationProxy(config, context)
        Log.d(TAG, "[$testName] Calling initializePowerAuth()...")
        proxy.initializePowerAuth()
        Log.d(TAG, "[$testName] initializePowerAuth() done")

        Log.d(TAG, "[$testName] Calling prepareActivation()...")
        proxy.prepareActivation()
        Log.d(TAG, "[$testName] prepareActivation() done, activationId=${proxy.activationId}")
        logPowerAuthState(testName, proxy)

        try {
            val testApi = proxy.createApi(config.operationsServerUrl)
            Log.d(TAG, "[$testName] API created with baseUrl: ${config.operationsServerUrl}")
            Log.d(TAG, "[$testName] Using endpoint: ${TestEndpoints.history}")
            Log.d(TAG, "[$testName] PIN length: ${proxy.pin.length}")

            val latch = CountDownLatch(1)
            var receivedResponse: StatusResponse? = null
            var receivedError: ApiError? = null

            Log.d(TAG, "[$testName] Sending authenticated POST...")
            testApi.post(
                data = BaseRequest(),
                endpoint = TestEndpoints.history,
                authentication = PowerAuthAuthentication.possessionWithPassword(proxy.pin),
                listener = object : IApiCallResponseListener<StatusResponse> {
                    override fun onSuccess(result: StatusResponse) {
                        Log.d(TAG, "[$testName] onSuccess: status=${result.status}")
                        receivedResponse = result
                        latch.countDown()
                    }

                    override fun onFailure(error: ApiError) {
                        Log.e(TAG, "[$testName] onFailure called")
                        logErrorDetails(testName, error)
                        receivedError = error
                        latch.countDown()
                    }
                }
            )

            val completed = latch.await(30, TimeUnit.SECONDS)
            Log.d(TAG, "[$testName] Latch completed=$completed, response=$receivedResponse, error=$receivedError")
            assertTrue("Authenticated request should complete within 30s", completed)
            if (receivedError != null) {
                logErrorDetails(testName, receivedError)
                fail("Expected success but got error: ${receivedError!!.e?.message} cause: ${receivedError!!.e?.cause?.message}")
            }
            assertNotNull("Should receive success response", receivedResponse)
            assertEquals(StatusResponse.Status.OK, receivedResponse!!.status)
        } finally {
            proxy.cleanup()
        }
    }

    /**
     * Token-authenticated POST to operation/list endpoint.
     * Verifies [EndpointAuthenticatedWithToken] flow with automatic token management.
     */
    @Test
    fun tokenAuthenticatedPostOperationList() {
        val testName = "tokenAuthenticatedPostOperationList"
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Log.d(TAG, "[$testName] Starting test")
        Log.d(TAG, "[$testName] operationsServerUrl: ${config.operationsServerUrl}")
        Log.d(TAG, "[$testName] enrollmentServerUrl: ${config.enrollmentServerUrl}")
        Log.d(TAG, "[$testName] cloudServerUrl: ${config.cloudServerUrl}")

        val proxy = PowerAuthIntegrationProxy(config, context)
        Log.d(TAG, "[$testName] Calling initializePowerAuth()...")
        proxy.initializePowerAuth()
        Log.d(TAG, "[$testName] initializePowerAuth() done")

        Log.d(TAG, "[$testName] Calling prepareActivation()...")
        proxy.prepareActivation()
        Log.d(TAG, "[$testName] prepareActivation() done, activationId=${proxy.activationId}")
        logPowerAuthState(testName, proxy)

        try {
            val testApi = proxy.createApi(config.operationsServerUrl)
            Log.d(TAG, "[$testName] API created with baseUrl: ${config.operationsServerUrl}")
            Log.d(TAG, "[$testName] Using endpoint: operationList (token: possession_universal)")

            val latch = CountDownLatch(1)
            var receivedResponse: StatusResponse? = null
            var receivedError: ApiError? = null

            Log.d(TAG, "[$testName] Sending token-authenticated POST...")
            testApi.post(
                data = BaseRequest(),
                endpoint = TestEndpoints.operationList,
                listener = object : IApiCallResponseListener<StatusResponse> {
                    override fun onSuccess(result: StatusResponse) {
                        Log.d(TAG, "[$testName] onSuccess: status=${result.status}")
                        receivedResponse = result
                        latch.countDown()
                    }

                    override fun onFailure(error: ApiError) {
                        Log.e(TAG, "[$testName] onFailure called")
                        logErrorDetails(testName, error)
                        receivedError = error
                        latch.countDown()
                    }
                }
            )

            val completed = latch.await(30, TimeUnit.SECONDS)
            Log.d(TAG, "[$testName] Latch completed=$completed, response=$receivedResponse, error=$receivedError")
            assertTrue("Token-authenticated request should complete within 30s", completed)
            if (receivedError != null) {
                logErrorDetails(testName, receivedError)
                fail("Expected success but got error: ${receivedError!!.e?.message} cause: ${receivedError!!.e?.cause?.message}")
            }
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
     * Authenticated POST with the wrong PIN.
     */
    @Test
    fun authenticatedPostWrongPin() {
        val testName = "authenticatedPostWrongPin"
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Log.d(TAG, "[$testName] Starting test")
        Log.d(TAG, "[$testName] operationsServerUrl: ${config.operationsServerUrl}")
        Log.d(TAG, "[$testName] enrollmentServerUrl: ${config.enrollmentServerUrl}")

        val proxy = PowerAuthIntegrationProxy(config, context)
        Log.d(TAG, "[$testName] Calling initializePowerAuth()...")
        proxy.initializePowerAuth()
        Log.d(TAG, "[$testName] initializePowerAuth() done")

        Log.d(TAG, "[$testName] Calling prepareActivation()...")
        proxy.prepareActivation()
        Log.d(TAG, "[$testName] prepareActivation() done, activationId=${proxy.activationId}")
        logPowerAuthState(testName, proxy)

        try {
            val testApi = proxy.createApi(config.operationsServerUrl)
            Log.d(TAG, "[$testName] API created with baseUrl: ${config.operationsServerUrl}")
            Log.d(TAG, "[$testName] Using wrong PIN '0000' (correct pin length: ${proxy.pin.length})")

            val latch = CountDownLatch(1)
            var receivedError: ApiError? = null

            Log.d(TAG, "[$testName] Sending authenticated POST with wrong PIN...")
            testApi.post(
                data = BaseRequest(),
                endpoint = TestEndpoints.history,
                authentication = PowerAuthAuthentication.possessionWithPassword("0000"),
                listener = object : IApiCallResponseListener<StatusResponse> {
                    override fun onSuccess(result: StatusResponse) {
                        Log.e(TAG, "[$testName] onSuccess called unexpectedly: status=${result.status}")
                        latch.countDown()
                        fail("Request should have failed with wrong PIN but succeeded")
                    }

                    override fun onFailure(error: ApiError) {
                        Log.d(TAG, "[$testName] onFailure called (expected)")
                        logErrorDetails(testName, error)
                        receivedError = error
                        latch.countDown()
                    }
                }
            )

            val completed = latch.await(30, TimeUnit.SECONDS)
            Log.d(TAG, "[$testName] Latch completed=$completed, error=$receivedError")
            assertTrue("Request should complete within 30s", completed)
            assertNotNull("Should receive error", receivedError)

            Log.d(TAG, "[$testName] Checking error type...")
            val httpException = receivedError!!.e as? ApiHttpException
            if (httpException == null) {
                Log.e(TAG, "[$testName] Error is NOT ApiHttpException!")
                Log.e(TAG, "[$testName] Actual exception type: ${receivedError!!.e?.javaClass?.name}")
                Log.e(TAG, "[$testName] Actual exception message: ${receivedError!!.e?.message}")
                Log.e(TAG, "[$testName] Actual exception cause: ${receivedError!!.e?.cause}")
                Log.e(TAG, "[$testName] Actual exception cause message: ${receivedError!!.e?.cause?.message}")
                val sw = java.io.StringWriter()
                receivedError!!.e?.printStackTrace(java.io.PrintWriter(sw))
                Log.e(TAG, "[$testName] Full stack trace:\n$sw")
            }
            assertNotNull("Error should be ApiHttpException", httpException)
            assertNotNull("Should have error response", httpException!!.errorResponse)
            Log.d(TAG, "[$testName] errorResponse errorCode: ${httpException.errorResponse!!.responseObject.errorCode}")
            assertEquals(
                ApiErrorCode.POWERAUTH_AUTH_FAIL,
                httpException.errorResponse!!.responseObject.errorCode
            )
        } catch (e: Exception) {
            Log.e(TAG, "[$testName] Unexpected exception: ${e.javaClass.name}: ${e.message}")
            Log.e(TAG, "[$testName] Cause: ${e.cause?.javaClass?.name}: ${e.cause?.message}")
            val sw = java.io.StringWriter()
            e.printStackTrace(java.io.PrintWriter(sw))
            Log.e(TAG, "[$testName] Full stack trace:\n$sw")
            fail("Unexpected exception: $e with message ${e.message} and cause ${e.cause}")
        } finally {
            proxy.cleanup()
        }
    }
}
