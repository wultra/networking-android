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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wultra.android.powerauth.networking

import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenListener
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that the compatibility token APIs remain available and deprecated.
 */
class DeprecatedTokenApisTest {

    @Test
    fun tokenApisAreDeprecatedForCompatibility() {
        assertTrue(IPowerAuthTokenProvider::class.java.isAnnotationPresent(java.lang.Deprecated::class.java))
        assertTrue(IPowerAuthTokenListener::class.java.isAnnotationPresent(java.lang.Deprecated::class.java))
    }
}
