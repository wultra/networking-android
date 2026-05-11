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

    private fun assertCode(expected: ApiErrorCode, code: String) = assertEquals(expected, ApiErrorCode.errorCodeFromCodeString(code))

    // --- Common errors ---

    @Test fun `ERROR_GENERIC resolves`() = assertCode(ApiErrorCode.ERROR_GENERIC, "ERROR_GENERIC")

    @Test fun `POWERAUTH_AUTH_FAIL resolves`() = assertCode(ApiErrorCode.POWERAUTH_AUTH_FAIL, "POWERAUTH_AUTH_FAIL")

    @Test fun `INVALID_REQUEST resolves`() = assertCode(ApiErrorCode.INVALID_REQUEST, "INVALID_REQUEST")

    @Test fun `INVALID_ACTIVATION resolves`() = assertCode(ApiErrorCode.INVALID_ACTIVATION, "INVALID_ACTIVATION")

    @Test fun `INVALID_APPLICATION resolves`() = assertCode(ApiErrorCode.INVALID_APPLICATION, "INVALID_APPLICATION")

    @Test fun `INVALID_OPERATION resolves`() = assertCode(ApiErrorCode.INVALID_OPERATION, "INVALID_OPERATION")

    @Test fun `ERR_ACTIVATION resolves`() = assertCode(ApiErrorCode.ERR_ACTIVATION, "ERR_ACTIVATION")

    @Test fun `ERR_AUTHENTICATION resolves`() = assertCode(ApiErrorCode.ERR_AUTHENTICATION, "ERR_AUTHENTICATION")

    @Test fun `ERR_SECURE_VAULT resolves`() = assertCode(ApiErrorCode.ERR_SECURE_VAULT, "ERR_SECURE_VAULT")

    @Test fun `ERR_ENCRYPTION resolves`() = assertCode(ApiErrorCode.ERR_ENCRYPTION, "ERR_ENCRYPTION")

    @Test fun `ERR_TEMPORARY_KEY resolves`() = assertCode(ApiErrorCode.ERR_TEMPORARY_KEY, "ERR_TEMPORARY_KEY")

    // --- Push errors ---

    @Test fun `PUSH_REGISTRATION_FAILED resolves`() = assertCode(ApiErrorCode.PUSH_REGISTRATION_FAILED, "PUSH_REGISTRATION_FAILED")

    // --- Operation errors ---

    @Test fun `OPERATION_ALREADY_FINISHED resolves`() = assertCode(ApiErrorCode.OPERATION_ALREADY_FINISHED, "OPERATION_ALREADY_FINISHED")

    @Test fun `OPERATION_ALREADY_FAILED resolves`() = assertCode(ApiErrorCode.OPERATION_ALREADY_FAILED, "OPERATION_ALREADY_FAILED")

    @Test fun `OPERATION_ALREADY_CANCELED resolves`() = assertCode(ApiErrorCode.OPERATION_ALREADY_CANCELED, "OPERATION_ALREADY_CANCELED")

    @Test fun `OPERATION_EXPIRED resolves`() = assertCode(ApiErrorCode.OPERATION_EXPIRED, "OPERATION_EXPIRED")

    @Test fun `OPERATION_FAILED resolves`() = assertCode(ApiErrorCode.OPERATION_FAILED, "OPERATION_FAILED")

    // --- Activation spawn errors ---

    @Test fun `ACTIVATION_CODE_FAILED resolves`() = assertCode(ApiErrorCode.ACTIVATION_CODE_FAILED, "ACTIVATION_CODE_FAILED")

    // --- Onboarding errors ---

    @Test fun `ONBOARDING_FAILED resolves`() = assertCode(ApiErrorCode.ONBOARDING_FAILED, "ONBOARDING_FAILED")

    @Test fun `ONBOARDING_PROCESS_LIMIT_REACHED resolves`() = assertCode(ApiErrorCode.ONBOARDING_PROCESS_LIMIT_REACHED, "ONBOARDING_PROCESS_LIMIT_REACHED")

    @Test fun `ONBOARDING_TOO_MANY_PROCESSES resolves`() = assertCode(ApiErrorCode.ONBOARDING_TOO_MANY_PROCESSES, "TOO_MANY_ONBOARDING_PROCESSES")

    @Test fun `ONBOARDING_OTP_FAILED resolves`() = assertCode(ApiErrorCode.ONBOARDING_OTP_FAILED, "ONBOARDING_OTP_FAILED")

    // --- Identity verification errors ---

    @Test fun `IDENTITY_INVALID_DOCUMENT resolves`() = assertCode(ApiErrorCode.IDENTITY_INVALID_DOCUMENT, "INVALID_DOCUMENT")

    @Test fun `IDENTITY_DOCUMENT_SUBMIT_FAILED resolves`() = assertCode(ApiErrorCode.IDENTITY_DOCUMENT_SUBMIT_FAILED, "DOCUMENT_SUBMIT_FAILED")

    @Test fun `IDENTITY_VERIFICATION_FAILED resolves`() = assertCode(ApiErrorCode.IDENTITY_VERIFICATION_FAILED, "IDENTITY_VERIFICATION_FAILED")

    @Test fun `IDENTITY_VERIFICATION_LIMIT_REACHED resolves`() = assertCode(ApiErrorCode.IDENTITY_VERIFICATION_LIMIT_REACHED, "IDENTITY_VERIFICATION_LIMIT_REACHED")

    @Test fun `IDENTITY_DOCUMENT_VERIFICATION_FAILED resolves`() = assertCode(ApiErrorCode.IDENTITY_DOCUMENT_VERIFICATION_FAILED, "DOCUMENT_VERIFICATION_FAILED")

    @Test fun `IDENTITY_PRESENCE_CHECK_FAILED resolves`() = assertCode(ApiErrorCode.IDENTITY_PRESENCE_CHECK_FAILED, "PRESENCE_CHECK_FAILED")

    @Test fun `IDENTITY_PRESENCE_CHECK_NOT_ENABLED resolves`() = assertCode(ApiErrorCode.IDENTITY_PRESENCE_CHECK_NOT_ENABLED, "PRESENCE_CHECK_NOT_ENABLED")

    @Test fun `IDENTITY_PRESENCE_CHECK_LIMIT_REACHED resolves`() = assertCode(ApiErrorCode.IDENTITY_PRESENCE_CHECK_LIMIT_REACHED, "PRESENCE_CHECK_LIMIT_REACHED")

    // --- Other errors ---

    @Test fun `TOO_MANY_REQUESTS resolves`() = assertCode(ApiErrorCode.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS")

    @Test fun `REMOTE_COMMUNICATION_ERROR resolves`() = assertCode(ApiErrorCode.REMOTE_COMMUNICATION_ERROR, "REMOTE_COMMUNICATION_ERROR")

    // --- Edge cases ---

    @Test fun `unknown code returns null`() = assertNull(ApiErrorCode.errorCodeFromCodeString("COMPLETELY_UNKNOWN_CODE"))

    @Test fun `empty string returns null`() = assertNull(ApiErrorCode.errorCodeFromCodeString(""))

    @Test fun `lowercase code returns null`() = assertNull(ApiErrorCode.errorCodeFromCodeString("invalid_request"))

    @Test fun `mixed case code returns null`() = assertNull(ApiErrorCode.errorCodeFromCodeString("Invalid_Request"))

    @Test
    fun `all known codes resolve via loop`() {
        for (code in ApiErrorCode.entries) {
            assertEquals("Code ${code.message} should resolve to $code", code, ApiErrorCode.errorCodeFromCodeString(code.message))
        }
    }
}
