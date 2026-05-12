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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for UserAgent factory methods.
 * Mirrors Apple's WPNUserAgentTests.
 *
 * Note: UserAgent.libraryDefault(context) requires Android Context
 * and is tested in instrumented tests instead.
 */
class UserAgentTest {

    @Test
    fun `systemDefault has null value`() {
        val agent = UserAgent.systemDefault()
        assertNull(agent.value)
    }

    @Test
    fun `customValue returns the provided string`() {
        val agent = UserAgent.customValue("Custom/1.0")
        assertEquals("Custom/1.0", agent.value)
    }

    @Test
    fun `customValue with complex user agent string`() {
        val ua = "MyApp/2.3.1 (en; wifi) com.example.app/1.0.0 (Samsung; Android/14; SM-S911B)"
        val agent = UserAgent.customValue(ua)
        assertEquals(ua, agent.value)
    }

    @Test
    fun `customValue with empty string`() {
        val agent = UserAgent.customValue("")
        assertEquals("", agent.value)
    }
}
