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

    /** Collects diagnostic messages that will be included in failure assertions. */
    private class DiagLog {
        private val sb = StringBuilder()

        fun log(msg: String) {
            sb.appendLine(msg)
            // Also send to logcat for local debugging
            Log.d("PostRealServerTest", msg)
        }

        fun logError(error: ApiError?) {
            if (error == null) {
                log("  error: null")
                return
            }
            log("  ApiError.e class: ${error.e?.javaClass?.name}")
            log("  ApiError.e message: ${error.e?.message}")
            log("  cause: ${error.e?.cause?.javaClass?.name}: ${error.e?.cause?.message}")
            log("  cause.cause: ${error.e?.cause?.cause?.javaClass?.name}: ${error.e?.cause?.cause?.message}")

            val httpException = error.e as? ApiHttpException
            if (httpException != null) {
                log("  ApiHttpException.errorResponse: ${httpException.errorResponse}")
            }

            val sw = java.io.StringWriter()
            error.e?.printStackTrace(java.io.PrintWriter(sw))
            log("  Stack trace:\n$sw")
        }

        fun logPowerAuthState(proxy: PowerAuthIntegrationProxy) {
            val pa = proxy.powerAuth
            if (pa == null) {
                log("  PowerAuth SDK: null!")
                return
            }
            log("  hasValidActivation: ${pa.hasValidActivation()}")
            log("  canStartActivation: ${pa.canStartActivation()}")
            log("  hasPendingActivation: ${pa.hasPendingActivation()}")
            log("  activationIdentifier: ${pa.activationIdentifier}")
        }

        override fun toString(): String = sb.toString()
    }

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
        val diag = DiagLog()
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        diag.log("enrollmentServerOnboardingUrl: ${config.enrollmentServerOnboardingUrl}")
        diag.log("enrollmentServerUrl: ${config.enrollmentServerUrl}")
        diag.log("cloudServerUrl: ${config.cloudServerUrl}")

        val proxy = PowerAuthIntegrationProxy(config, context)
        diag.log("initializePowerAuth()...")
        proxy.initializePowerAuth()
        diag.log("initializePowerAuth() done")

        diag.log("prepareActivation()...")
        proxy.prepareActivation()
        diag.log("prepareActivation() done, activationId=${proxy.activationId}")
        diag.logPowerAuthState(proxy)

        try {
            val testApi = proxy.createApi(config.enrollmentServerOnboardingUrl)
            diag.log("API baseUrl: ${config.enrollmentServerOnboardingUrl}")

            val latch = CountDownLatch(1)
            var receivedResponse: StatusResponse? = null
            var receivedError: ApiError? = null

            val requestId = UUID.randomUUID().toString()
            diag.log("Sending E2EE POST, requestId=$requestId")

            testApi.post(
                data = TestEndpoints.StartObjectRequest(
                    TestEndpoints.StartRequest(
                        identification = mapOf("clientNumber" to requestId)
                    )
                ),
                endpoint = TestEndpoints.start,
                listener = object : IApiCallResponseListener<StatusResponse> {
                    override fun onSuccess(result: StatusResponse) {
                        diag.log("onSuccess: status=${result.status}")
                        receivedResponse = result
                        latch.countDown()
                    }

                    override fun onFailure(error: ApiError) {
                        diag.log("onFailure:")
                        diag.logError(error)
                        receivedError = error
                        latch.countDown()
                    }
                }
            )

            val completed = latch.await(30, TimeUnit.SECONDS)
            diag.log("Latch: completed=$completed")
            if (!completed) fail("E2EE request timed out (30s).\n\nDiagnostics:\n$diag")
            if (receivedError != null) {
                fail("Expected success but got error.\n\nDiagnostics:\n$diag")
            }
            assertNotNull("Should receive success response.\n\nDiagnostics:\n$diag", receivedResponse)
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
        val diag = DiagLog()
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        diag.log("operationsServerUrl: ${config.operationsServerUrl}")
        diag.log("enrollmentServerUrl: ${config.enrollmentServerUrl}")
        diag.log("cloudServerUrl: ${config.cloudServerUrl}")

        val proxy = PowerAuthIntegrationProxy(config, context)
        diag.log("initializePowerAuth()...")
        proxy.initializePowerAuth()
        diag.log("initializePowerAuth() done")

        diag.log("prepareActivation()...")
        proxy.prepareActivation()
        diag.log("prepareActivation() done, activationId=${proxy.activationId}")
        diag.logPowerAuthState(proxy)

        try {
            val testApi = proxy.createApi(config.operationsServerUrl)
            diag.log("API baseUrl: ${config.operationsServerUrl}")
            diag.log("Endpoint: /api/auth/token/app/operation/history, uriId=/operation/history")
            diag.log("PIN length: ${proxy.pin.length}")

            val latch = CountDownLatch(1)
            var receivedResponse: StatusResponse? = null
            var receivedError: ApiError? = null

            diag.log("Sending authenticated POST...")
            testApi.post(
                data = BaseRequest(),
                endpoint = TestEndpoints.history,
                authentication = PowerAuthAuthentication.possessionWithPassword(proxy.pin),
                listener = object : IApiCallResponseListener<StatusResponse> {
                    override fun onSuccess(result: StatusResponse) {
                        diag.log("onSuccess: status=${result.status}")
                        receivedResponse = result
                        latch.countDown()
                    }

                    override fun onFailure(error: ApiError) {
                        diag.log("onFailure:")
                        diag.logError(error)
                        receivedError = error
                        latch.countDown()
                    }
                }
            )

            val completed = latch.await(30, TimeUnit.SECONDS)
            diag.log("Latch: completed=$completed")
            if (!completed) fail("Authenticated request timed out (30s).\n\nDiagnostics:\n$diag")
            if (receivedError != null) {
                fail("Expected success but got error.\n\nDiagnostics:\n$diag")
            }
            assertNotNull("Should receive success response.\n\nDiagnostics:\n$diag", receivedResponse)
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
        val diag = DiagLog()
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        diag.log("operationsServerUrl: ${config.operationsServerUrl}")
        diag.log("enrollmentServerUrl: ${config.enrollmentServerUrl}")
        diag.log("cloudServerUrl: ${config.cloudServerUrl}")

        val proxy = PowerAuthIntegrationProxy(config, context)
        diag.log("initializePowerAuth()...")
        proxy.initializePowerAuth()
        diag.log("initializePowerAuth() done")

        diag.log("prepareActivation()...")
        proxy.prepareActivation()
        diag.log("prepareActivation() done, activationId=${proxy.activationId}")
        diag.logPowerAuthState(proxy)

        try {
            val testApi = proxy.createApi(config.operationsServerUrl)
            diag.log("API baseUrl: ${config.operationsServerUrl}")
            diag.log("Endpoint: /api/auth/token/app/operation/list, token=possession_universal")

            val latch = CountDownLatch(1)
            var receivedResponse: StatusResponse? = null
            var receivedError: ApiError? = null

            diag.log("Sending token-authenticated POST...")
            testApi.post(
                data = BaseRequest(),
                endpoint = TestEndpoints.operationList,
                listener = object : IApiCallResponseListener<StatusResponse> {
                    override fun onSuccess(result: StatusResponse) {
                        diag.log("onSuccess: status=${result.status}")
                        receivedResponse = result
                        latch.countDown()
                    }

                    override fun onFailure(error: ApiError) {
                        diag.log("onFailure:")
                        diag.logError(error)
                        receivedError = error
                        latch.countDown()
                    }
                }
            )

            val completed = latch.await(30, TimeUnit.SECONDS)
            diag.log("Latch: completed=$completed")
            if (!completed) fail("Token-authenticated request timed out (30s).\n\nDiagnostics:\n$diag")
            if (receivedError != null) {
                fail("Expected success but got error.\n\nDiagnostics:\n$diag")
            }
            assertNotNull("Should receive success response.\n\nDiagnostics:\n$diag", receivedResponse)
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
        val diag = DiagLog()
        val config = loadConfigOrSkip()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        diag.log("operationsServerUrl: ${config.operationsServerUrl}")
        diag.log("enrollmentServerUrl: ${config.enrollmentServerUrl}")

        val proxy = PowerAuthIntegrationProxy(config, context)
        diag.log("initializePowerAuth()...")
        proxy.initializePowerAuth()
        diag.log("initializePowerAuth() done")

        diag.log("prepareActivation()...")
        proxy.prepareActivation()
        diag.log("prepareActivation() done, activationId=${proxy.activationId}")
        diag.logPowerAuthState(proxy)

        try {
            val testApi = proxy.createApi(config.operationsServerUrl)
            diag.log("API baseUrl: ${config.operationsServerUrl}")
            diag.log("Endpoint: /api/auth/token/app/operation/history")
            diag.log("Using wrong PIN '0000' (correct pin length: ${proxy.pin.length})")

            val latch = CountDownLatch(1)
            var receivedError: ApiError? = null

            diag.log("Sending authenticated POST with wrong PIN...")
            testApi.post(
                data = BaseRequest(),
                endpoint = TestEndpoints.history,
                authentication = PowerAuthAuthentication.possessionWithPassword("0000"),
                listener = object : IApiCallResponseListener<StatusResponse> {
                    override fun onSuccess(result: StatusResponse) {
                        diag.log("onSuccess called UNEXPECTEDLY: status=${result.status}")
                        latch.countDown()
                        fail("Request should have failed with wrong PIN but succeeded.\n\nDiagnostics:\n$diag")
                    }

                    override fun onFailure(error: ApiError) {
                        diag.log("onFailure (expected):")
                        diag.logError(error)
                        receivedError = error
                        latch.countDown()
                    }
                }
            )

            val completed = latch.await(30, TimeUnit.SECONDS)
            diag.log("Latch: completed=$completed")
            if (!completed) fail("Request timed out (30s).\n\nDiagnostics:\n$diag")

            if (receivedError == null) {
                fail("Should receive error but got null.\n\nDiagnostics:\n$diag")
            }

            val httpException = receivedError!!.e as? ApiHttpException
            if (httpException == null) {
                diag.log("Error is NOT ApiHttpException!")
                diag.log("Actual type: ${receivedError!!.e?.javaClass?.name}")
                fail("Error should be ApiHttpException but was ${receivedError!!.e?.javaClass?.name}.\n\nDiagnostics:\n$diag")
            }
            if (httpException!!.errorResponse == null) {
                fail("Should have error response but was null.\n\nDiagnostics:\n$diag")
            }
            diag.log("errorResponse errorCode: ${httpException.errorResponse!!.responseObject.errorCode}")
            assertEquals(
                "Expected POWERAUTH_AUTH_FAIL.\n\nDiagnostics:\n$diag",
                ApiErrorCode.POWERAUTH_AUTH_FAIL,
                httpException.errorResponse!!.responseObject.errorCode
            )
        } catch (e: AssertionError) {
            throw e
        } catch (e: Exception) {
            diag.log("Unexpected exception: ${e.javaClass.name}: ${e.message}")
            diag.log("Cause: ${e.cause?.javaClass?.name}: ${e.cause?.message}")
            val sw = java.io.StringWriter()
            e.printStackTrace(java.io.PrintWriter(sw))
            diag.log("Stack trace:\n$sw")
            fail("Unexpected exception: ${e.javaClass.name}: ${e.message}\n\nDiagnostics:\n$diag")
        } finally {
            proxy.cleanup()
        }
    }
}
