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

import com.wultra.android.powerauth.networking.error.ApiErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiErrorCodeTest {

    @Test
    fun `all known codes resolve via loop`() {
        for (code in ApiErrorCode.entries) {
            assertEquals("Code ${code.message} should resolve to $code", code, ApiErrorCode.errorCodeFromCodeString(code.message))
        }
    }

    // --- Edge cases ---

    @Test fun `unknown code returns null`() = assertNull(ApiErrorCode.errorCodeFromCodeString("COMPLETELY_UNKNOWN_CODE"))

    @Test fun `empty string returns null`() = assertNull(ApiErrorCode.errorCodeFromCodeString(""))

    @Test fun `lowercase code returns null`() = assertNull(ApiErrorCode.errorCodeFromCodeString("invalid_request"))

    @Test fun `mixed case code returns null`() = assertNull(ApiErrorCode.errorCodeFromCodeString("Invalid_Request"))
}
