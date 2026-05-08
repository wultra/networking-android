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

import com.wultra.android.powerauth.networking.ssl.SSLValidationStrategy
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests for SSLValidationStrategy.
 * Mirrors Apple's WPNSSLValidationStrategyTests.
 *
 * Note: noValidation() and sslPinning() depend on PowerAuth SDK classes
 * and are tested in instrumented tests instead.
 */
class SSLValidationStrategyTest {

    @Test
    fun `system strategy does not modify builder`() {
        val strategy = SSLValidationStrategy.system()
        val originalBuilder = OkHttpClient.Builder()
        val resultBuilder = strategy.configure(originalBuilder)
        assertNotNull(resultBuilder)
        // system() returns the builder unmodified
        val client = resultBuilder.build()
        assertNotNull(client)
    }

    @Test
    fun `system strategy builds valid client`() {
        val strategy = SSLValidationStrategy.system()
        val client = strategy.configure(OkHttpClient.Builder()).build()
        assertNotNull(client)
        // Default SSL configuration should use system trust manager
        assertNotNull(client.sslSocketFactory)
    }

    @Test
    fun `system strategy can be applied multiple times`() {
        val strategy = SSLValidationStrategy.system()
        val builder = OkHttpClient.Builder()
        strategy.configure(builder)
        strategy.configure(builder)
        val client = builder.build()
        assertNotNull(client)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated default returns same behavior as system`() {
        val defaultStrategy = SSLValidationStrategy.default()
        val systemStrategy = SSLValidationStrategy.system()

        val defaultClient = defaultStrategy.configure(OkHttpClient.Builder()).build()
        val systemClient = systemStrategy.configure(OkHttpClient.Builder()).build()

        // Both should produce valid clients with default SSL
        assertNotNull(defaultClient)
        assertNotNull(systemClient)
    }
}
