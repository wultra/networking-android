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

package com.wultra.android.powerauth.networking.support

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.wultra.android.powerauth.networking.UserAgent
import io.getlime.security.powerauth.networking.response.CreateActivationResult
import io.getlime.security.powerauth.networking.response.IActivationRemoveListener
import io.getlime.security.powerauth.networking.response.ICreateActivationListener
import io.getlime.security.powerauth.networking.response.IPersistActivationListener
import io.getlime.security.powerauth.sdk.PowerAuthActivation
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthClientConfiguration
import io.getlime.security.powerauth.sdk.PowerAuthConfiguration
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A pre-generated PowerAuth mobile SDK configuration string (valid for SDK 2.0+).
 *
 * This constant is used by [createDummyPowerAuth] to build a [PowerAuthSDK] instance
 * that can be initialized without contacting a real server — useful for offline or
 * mock test scenarios where only the SDK object is needed (e.g. verifying request
 * serialization), not actual cryptographic operations.
 */
const val DUMMY_SDK_CONFIG = "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"

/**
 * Creates a [PowerAuthSDK] instance backed by [DUMMY_SDK_CONFIG].
 *
 * The returned SDK is functional enough to construct API requests but is **not**
 * connected to a real PowerAuth server. Use it in tests that only need an SDK
 * reference without performing real signing or activation.
 *
 * @param context Android application context required by the SDK builder.
 * @param baseUrl Base URL stored in the SDK configuration (not actually contacted).
 * @return A locally-initialised [PowerAuthSDK] with no persisted activation.
 */
fun createDummyPowerAuth(context: Context, baseUrl: String): PowerAuthSDK {
    val config = PowerAuthConfiguration.Builder(
        "test-instance",
        baseUrl,
        DUMMY_SDK_CONFIG
    ).build()
    return PowerAuthSDK.Builder(config)
        .clientConfiguration(PowerAuthClientConfiguration.Builder().build())
        .build(context)
}

/**
 * Manages the full PowerAuth activation lifecycle against a real PowerAuth Cloud server.
 *
 * This helper encapsulates the server-side registration, client-side activation, and
 * cleanup steps so that integration tests can focus on the networking layer itself.
 *
 * ### Typical usage
 * ```
 * val proxy = PowerAuthIntegrationProxy(config, context)
 * proxy.initializePowerAuth()
 * proxy.prepareActivation()   // optional — only needed for signed/token endpoints
 * val api = proxy.createApi(baseUrl)
 * // ... run tests ...
 * proxy.cleanup()
 * ```
 *
 * @property config     Server connection details loaded from [TestConfiguration].
 * @property appContext Android application context.
 * @property pin        Password used to persist the activation. Defaults to a random UUID so
 *                      each test run uses a unique credential.
 */
class PowerAuthIntegrationProxy(
    private val config: TestConfiguration,
    private val appContext: Context,
    val pin: String = UUID.randomUUID().toString()
) {

    private val httpClient = OkHttpClient()
    private val gson = Gson()
    private val activationName = UUID.randomUUID().toString()

    /** The initialised PowerAuth SDK instance, or `null` before [initializePowerAuth] is called. */
    var powerAuth: PowerAuthSDK? = null
        private set

    /** Server-assigned activation identifier, or `null` before [prepareActivation] is called. */
    var activationId: String? = null
        private set

    /**
     * Fetches the mobile SDK configuration from PowerAuth Cloud and builds a [PowerAuthSDK]
     * instance. Must be called before [prepareActivation] or [createApi].
     */
    fun initializePowerAuth() {
        val detail = cloudGet<ApplicationDetail>("/admin/applications/${config.cloudApplicationId}")

        val paConfig = PowerAuthConfiguration.Builder(
            "integration-test",
            config.enrollmentServerUrl,
            detail.mobileSdkConfig
        ).build()

        val pa = PowerAuthSDK.Builder(paConfig)
            .clientConfiguration(PowerAuthClientConfiguration.Builder().build())
            .build(appContext)

        pa.removeActivationLocal(appContext)
        powerAuth = pa
        log("PowerAuthSDK initialized")
    }

    /**
     * Creates a server-side registration via the Cloud API and completes the client-side
     * activation (key exchange + persist). After this call the SDK is ready to sign requests.
     *
     * @throws IllegalStateException if [initializePowerAuth] has not been called.
     */
    fun prepareActivation() {
        val pa = requirePowerAuth()

        val response = cloudPost<RegistrationResponse>(
            "/v2/registrations",
            """{"appId":"${config.cloudApplicationId}","userId":"$activationName","commitPhase":"ON_KEY_EXCHANGE"}"""
        )
        activationId = response.registrationId
        log("Server activation created: ${response.registrationId}")

        val paActivation = PowerAuthActivation.Builder
            .activation(response.activationCode)
            .setActivationName(activationName)
            .build()

        awaitCallback { latch, setError ->
            pa.createActivation(
                paActivation,
                object : ICreateActivationListener {
                    override fun onActivationCreateSucceed(result: CreateActivationResult) {
                        pa.persistActivationWithPassword(
                            appContext,
                            pin,
                            object : IPersistActivationListener {
                                override fun onPersistActivationSucceeded() { latch.countDown() }
                                override fun onPersistActivationFailed(t: Throwable) { setError(t); latch.countDown() }
                                override fun onPersistActivationCancelled(userCancellation: Boolean) {
                                    setError(IllegalStateException("Activation persist cancelled")); latch.countDown()
                                }
                            }
                        )
                    }
                    override fun onActivationCreateFailed(t: Throwable) { setError(t); latch.countDown() }
                }
            )
        }
        log("Activation persisted")
    }

    /**
     * Removes the activation both on the server and locally.
     * Falls back to a local-only removal if the server call fails.
     * Safe to call even when no activation exists (the method is a no-op in that case).
     */
    fun cleanup() {
        val pa = powerAuth ?: return
        val latch = CountDownLatch(1)
        pa.removeActivationWithAuthentication(
            appContext,
            PowerAuthAuthentication.possessionWithPassword(pin),
            object : IActivationRemoveListener {
                override fun onActivationRemoveSucceed() { latch.countDown() }
                override fun onActivationRemoveFailed(t: Throwable) {
                    pa.removeActivationLocal(appContext)
                    latch.countDown()
                }
            }
        )
        latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    /**
     * Creates an [IntegrationTestApi] wired to the initialised [PowerAuthSDK].
     *
     * @param baseUrl Root URL of the server the test will communicate with.
     * @return A ready-to-use API instance.
     * @throws IllegalStateException if [initializePowerAuth] has not been called.
     */
    fun createApi(baseUrl: String): IntegrationTestApi {
        return IntegrationTestApi(
            baseUrl = baseUrl,
            okHttpClient = OkHttpClient(),
            powerAuthSDK = requirePowerAuth(),
            appContext = appContext,
            userAgent = UserAgent.customValue("WPNAndroidTest/1.0")
        )
    }

    // --- Internals ---

    private fun requirePowerAuth() =
        powerAuth ?: throw IllegalStateException("Call initializePowerAuth() first")

    private inline fun <reified T> cloudGet(path: String): T = cloudRequest(path, "GET")
    private inline fun <reified T> cloudPost(path: String, jsonBody: String): T = cloudRequest(path, "POST", jsonBody)

    private inline fun <reified T> cloudRequest(path: String, method: String, jsonBody: String = ""): T {
        val url = "${config.cloudServerUrl.removeSuffix("/")}$path"
        val credentials = "${config.cloudServerLogin}:${config.cloudServerPassword}"
        val base64 = android.util.Base64.encodeToString(credentials.toByteArray(), android.util.Base64.NO_WRAP)

        val body = if (jsonBody.isNotEmpty()) jsonBody.toRequestBody("application/json".toMediaType()) else null
        val request = Request.Builder()
            .url(url).method(method, body)
            .header("Authorization", "Basic $base64")
            .header("Content-Type", "application/json")
            .build()

        log("$method $url")
        return httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) throw IOException("Cloud server error: HTTP ${response.code} — $responseBody")
            gson.fromJson(responseBody, T::class.java)
        }
    }

    /** Runs a callback-based async operation synchronously with a timeout. */
    private fun awaitCallback(block: (CountDownLatch, (Throwable) -> Unit) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null
        block(latch) { error = it }
        if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) throw IllegalStateException("Operation timed out")
        error?.let { throw it }
    }

    private fun log(message: String) = Log.d("IntegrationProxy", message)

    private data class ApplicationDetail(@SerializedName("mobileSdkConfig") val mobileSdkConfig: String)
    private data class RegistrationResponse(
        @SerializedName("registrationId") val registrationId: String,
        @SerializedName("activationCode") val activationCode: String
    )

    companion object {
        private const val TIMEOUT_SECONDS = 30L
    }
}
