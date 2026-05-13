package com.wultra.android.powerauth.networking.support

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.InputStreamReader

/**
 * Integration test configuration loaded from a `config.json` file placed in the
 * `androidTest/assets` directory. The JSON structure must match the constructor
 * parameters (Gson deserialization).
 *
 * @property cloudServerUrl        Base URL of the PowerAuth Cloud admin API.
 * @property cloudServerLogin      HTTP Basic auth login for the Cloud admin API.
 * @property cloudServerPassword   HTTP Basic auth password for the Cloud admin API.
 * @property cloudApplicationId    Application identifier registered in PowerAuth Cloud.
 * @property enrollmentServerUrl   Base URL of the PowerAuth Enrollment Server.
 * @property enrollmentServerOnboardingUrl  URL used for onboarding-related test endpoints.
 * @property operationsServerUrl   Base URL of the Operations Server.
 * @property oidcProviderId        Optional OIDC provider identifier for token-based auth tests.
 * @property oidcProviderIdPkce    Optional OIDC provider identifier for PKCE flow tests.
 */
data class TestConfiguration(
    val cloudServerUrl: String,
    val cloudServerLogin: String,
    val cloudServerPassword: String,
    val cloudApplicationId: String,
    val enrollmentServerUrl: String,
    val enrollmentServerOnboardingUrl: String,
    val operationsServerUrl: String,
    val oidcProviderId: String?,
    val oidcProviderIdPkce: String?
) {
    companion object {
        /**
         * Attempts to load the configuration from `config.json` in the app's assets.
         *
         * @param context Android context used to access the asset file.
         * @return Parsed [TestConfiguration], or `null` when the file is missing
         *         (e.g. mock-only test runs that don't need a live server).
         */
        fun load(context: Context): TestConfiguration? = try {
            context.assets.open("config.json").use { stream ->
                InputStreamReader(stream).use { reader ->
                    Gson().fromJson(reader, TestConfiguration::class.java).also {
                        Log.i("TestConfiguration", "Config loaded")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("TestConfiguration", "config.json not found: ${e.message}")
            null
        }
    }
}
