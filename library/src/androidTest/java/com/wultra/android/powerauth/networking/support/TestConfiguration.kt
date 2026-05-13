package com.wultra.android.powerauth.networking.support

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.InputStreamReader

/**
 * Integration test configuration loaded from `config.json` in androidTest assets.
 * Returns `null` when the file is absent (mock-only test runs).
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