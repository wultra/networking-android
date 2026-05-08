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
    fun `known error code resolves correctly`() {
        assertEquals(
            ApiErrorCode.POWERAUTH_AUTH_FAIL,
            ApiErrorCode.errorCodeFromCodeString("POWERAUTH_AUTH_FAIL")
        )
    }

    @Test
    fun `invalid request code resolves`() {
        assertEquals(
            ApiErrorCode.INVALID_REQUEST,
            ApiErrorCode.errorCodeFromCodeString("INVALID_REQUEST")
        )
    }

    @Test
    fun `too many requests code resolves`() {
        assertEquals(
            ApiErrorCode.TOO_MANY_REQUESTS,
            ApiErrorCode.errorCodeFromCodeString("TOO_MANY_REQUESTS")
        )
    }

    @Test
    fun `unknown error code returns null`() {
        assertNull(ApiErrorCode.errorCodeFromCodeString("COMPLETELY_UNKNOWN_CODE"))
    }

    @Test
    fun `empty string returns null`() {
        assertNull(ApiErrorCode.errorCodeFromCodeString(""))
    }

    @Test
    fun `all known codes resolve correctly`() {
        for (code in ApiErrorCode.entries) {
            assertEquals(
                "Code ${code.message} should resolve to $code",
                code,
                ApiErrorCode.errorCodeFromCodeString(code.message)
            )
        }
    }

    @Test
    fun `error generic code resolves`() {
        assertEquals(
            ApiErrorCode.ERROR_GENERIC,
            ApiErrorCode.errorCodeFromCodeString("ERROR_GENERIC")
        )
    }

    @Test
    fun `onboarding codes resolve`() {
        assertEquals(
            ApiErrorCode.ONBOARDING_FAILED,
            ApiErrorCode.errorCodeFromCodeString("ONBOARDING_FAILED")
        )
        assertEquals(
            ApiErrorCode.ONBOARDING_PROCESS_LIMIT_REACHED,
            ApiErrorCode.errorCodeFromCodeString("ONBOARDING_PROCESS_LIMIT_REACHED")
        )
        // Note: ONBOARDING_TOO_MANY_PROCESSES maps to "TOO_MANY_ONBOARDING_PROCESSES"
        assertEquals(
            ApiErrorCode.ONBOARDING_TOO_MANY_PROCESSES,
            ApiErrorCode.errorCodeFromCodeString("TOO_MANY_ONBOARDING_PROCESSES")
        )
    }

    @Test
    fun `operation codes resolve`() {
        assertEquals(
            ApiErrorCode.OPERATION_ALREADY_FINISHED,
            ApiErrorCode.errorCodeFromCodeString("OPERATION_ALREADY_FINISHED")
        )
        assertEquals(
            ApiErrorCode.OPERATION_ALREADY_FAILED,
            ApiErrorCode.errorCodeFromCodeString("OPERATION_ALREADY_FAILED")
        )
        assertEquals(
            ApiErrorCode.OPERATION_ALREADY_CANCELED,
            ApiErrorCode.errorCodeFromCodeString("OPERATION_ALREADY_CANCELED")
        )
        assertEquals(
            ApiErrorCode.OPERATION_EXPIRED,
            ApiErrorCode.errorCodeFromCodeString("OPERATION_EXPIRED")
        )
        assertEquals(
            ApiErrorCode.OPERATION_FAILED,
            ApiErrorCode.errorCodeFromCodeString("OPERATION_FAILED")
        )
    }

    @Test
    fun `identity verification codes resolve`() {
        assertEquals(
            ApiErrorCode.IDENTITY_INVALID_DOCUMENT,
            ApiErrorCode.errorCodeFromCodeString("INVALID_DOCUMENT")
        )
        assertEquals(
            ApiErrorCode.IDENTITY_VERIFICATION_FAILED,
            ApiErrorCode.errorCodeFromCodeString("IDENTITY_VERIFICATION_FAILED")
        )
        assertEquals(
            ApiErrorCode.IDENTITY_PRESENCE_CHECK_FAILED,
            ApiErrorCode.errorCodeFromCodeString("PRESENCE_CHECK_FAILED")
        )
    }

    @Test
    fun `case sensitive matching`() {
        // Error codes are case-sensitive
        assertNull(ApiErrorCode.errorCodeFromCodeString("invalid_request"))
        assertNull(ApiErrorCode.errorCodeFromCodeString("Invalid_Request"))
    }

    @Test
    fun `message property matches code string`() {
        assertEquals("ERROR_GENERIC", ApiErrorCode.ERROR_GENERIC.message)
        assertEquals("POWERAUTH_AUTH_FAIL", ApiErrorCode.POWERAUTH_AUTH_FAIL.message)
        assertEquals("INVALID_REQUEST", ApiErrorCode.INVALID_REQUEST.message)
    }
}
