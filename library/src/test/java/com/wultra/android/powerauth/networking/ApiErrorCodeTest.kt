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
    fun `all known codes resolve via errorCodeFromCodeString`() {
        for (code in ApiErrorCode.entries) {
            assertEquals(
                "Code ${code.message} should resolve to $code",
                code,
                ApiErrorCode.errorCodeFromCodeString(code.message)
            )
        }
    }

    @Test
    fun `codes where enum name matches message string`() {
        val matchingCodes = ApiErrorCode.entries.filter { it.name == it.message }
        for (code in matchingCodes) {
            assertEquals(code, ApiErrorCode.errorCodeFromCodeString(code.name))
        }
    }

    @Test
    fun `codes where enum name differs from message string`() {
        // These entries have a message that does not match the enum name,
        // so they deserve an explicit assertion beyond the exhaustive loop.
        assertEquals(ApiErrorCode.ONBOARDING_TOO_MANY_PROCESSES, ApiErrorCode.errorCodeFromCodeString("TOO_MANY_ONBOARDING_PROCESSES"))
        assertEquals(ApiErrorCode.IDENTITY_INVALID_DOCUMENT, ApiErrorCode.errorCodeFromCodeString("INVALID_DOCUMENT"))
        assertEquals(ApiErrorCode.IDENTITY_DOCUMENT_SUBMIT_FAILED, ApiErrorCode.errorCodeFromCodeString("DOCUMENT_SUBMIT_FAILED"))
        assertEquals(ApiErrorCode.IDENTITY_DOCUMENT_VERIFICATION_FAILED, ApiErrorCode.errorCodeFromCodeString("DOCUMENT_VERIFICATION_FAILED"))
        assertEquals(ApiErrorCode.IDENTITY_PRESENCE_CHECK_FAILED, ApiErrorCode.errorCodeFromCodeString("PRESENCE_CHECK_FAILED"))
        assertEquals(ApiErrorCode.IDENTITY_PRESENCE_CHECK_NOT_ENABLED, ApiErrorCode.errorCodeFromCodeString("PRESENCE_CHECK_NOT_ENABLED"))
        assertEquals(ApiErrorCode.IDENTITY_PRESENCE_CHECK_LIMIT_REACHED, ApiErrorCode.errorCodeFromCodeString("PRESENCE_CHECK_LIMIT_REACHED"))
    }

    @Test
    fun `unknown code returns null`() {
        assertNull(ApiErrorCode.errorCodeFromCodeString("COMPLETELY_UNKNOWN_CODE"))
    }

    @Test
    fun `empty string returns null`() {
        assertNull(ApiErrorCode.errorCodeFromCodeString(""))
    }

    @Test
    fun `lookup is case sensitive`() {
        assertNull(ApiErrorCode.errorCodeFromCodeString("invalid_request"))
        assertNull(ApiErrorCode.errorCodeFromCodeString("Invalid_Request"))
    }
}
