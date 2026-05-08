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

import com.wultra.android.powerauth.networking.log.WPNLogListener
import com.wultra.android.powerauth.networking.log.WPNLogger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for WPNLogger configuration and log listener.
 * Uses `unitTests.isReturnDefaultValues = true` so android.util.Log calls succeed.
 */
class WPNLoggerTest {

    private var originalVerboseLevel: WPNLogger.VerboseLevel = WPNLogger.VerboseLevel.WARNING
    private var originalListener: WPNLogListener? = null
    private var originalLogHttpTraffic: Boolean = true

    @Before
    fun setUp() {
        originalVerboseLevel = WPNLogger.verboseLevel
        originalListener = WPNLogger.logListener
        originalLogHttpTraffic = WPNLogger.logHttpTraffic
    }

    @After
    fun tearDown() {
        WPNLogger.verboseLevel = originalVerboseLevel
        WPNLogger.logListener = originalListener
        WPNLogger.logHttpTraffic = originalLogHttpTraffic
    }

    @Test
    fun `default verbose level is WARNING`() {
        assertEquals(WPNLogger.VerboseLevel.WARNING, originalVerboseLevel)
    }

    @Test
    fun `verbose level can be changed`() {
        WPNLogger.verboseLevel = WPNLogger.VerboseLevel.DEBUG
        assertEquals(WPNLogger.VerboseLevel.DEBUG, WPNLogger.verboseLevel)
    }

    @Test
    fun `default log listener is null`() {
        assertNull(originalListener)
    }

    @Test
    fun `log listener can be set`() {
        val listener = TestLogListener()
        WPNLogger.logListener = listener
        assertEquals(listener, WPNLogger.logListener)
    }

    @Test
    fun `logHttpTraffic defaults to true`() {
        assertTrue(originalLogHttpTraffic)
    }

    @Test
    fun `logHttpTraffic can be disabled`() {
        WPNLogger.logHttpTraffic = false
        assertFalse(WPNLogger.logHttpTraffic)
    }

    @Test
    fun `VerboseLevel enum ordering`() {
        val levels = WPNLogger.VerboseLevel.entries
        assertEquals(5, levels.size)
        assertEquals(WPNLogger.VerboseLevel.OFF, levels[0])
        assertEquals(WPNLogger.VerboseLevel.ERROR, levels[1])
        assertEquals(WPNLogger.VerboseLevel.WARNING, levels[2])
        assertEquals(WPNLogger.VerboseLevel.INFO, levels[3])
        assertEquals(WPNLogger.VerboseLevel.DEBUG, levels[4])
    }

    @Test
    fun `VerboseLevel ordinal increases with verbosity`() {
        assertTrue(WPNLogger.VerboseLevel.OFF.ordinal < WPNLogger.VerboseLevel.ERROR.ordinal)
        assertTrue(WPNLogger.VerboseLevel.ERROR.ordinal < WPNLogger.VerboseLevel.WARNING.ordinal)
        assertTrue(WPNLogger.VerboseLevel.WARNING.ordinal < WPNLogger.VerboseLevel.INFO.ordinal)
        assertTrue(WPNLogger.VerboseLevel.INFO.ordinal < WPNLogger.VerboseLevel.DEBUG.ordinal)
    }

    private class TestLogListener : WPNLogListener {
        override val followVerboseLevel = true
        val messages = mutableListOf<Pair<String, String>>()

        override fun error(message: String) { messages.add("error" to message) }
        override fun warning(message: String) { messages.add("warning" to message) }
        override fun info(message: String) { messages.add("info" to message) }
        override fun debug(message: String) { messages.add("debug" to message) }
    }
}
