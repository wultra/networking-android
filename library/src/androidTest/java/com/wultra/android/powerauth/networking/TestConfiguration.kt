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

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.InputStreamReader

/**
 * Loads integration test configuration from `config.json` bundled
 * in the androidTest assets. See `assets/README.md` for setup instructions.
 */
class TestConfiguration private constructor(
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

        private const val TAG = "TestConfiguration"
        private const val CONFIG_FILE = "config.json"

        /**
         * Loads the integration [TestConfiguration] from the test assets.
         * Returns `null` if no `config.json` is present or if parsing fails.
         */
        fun load(context: Context): TestConfiguration? {
            return try {
                context.assets.open(CONFIG_FILE).use { stream ->
                    InputStreamReader(stream).use { reader ->
                        val config = Gson().fromJson(reader, TestConfiguration::class.java)
                        Log.i(TAG, "Config loaded from $CONFIG_FILE")
                        config
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Config not found in test assets: ${e.message}")
                null
            }
        }
    }
}
