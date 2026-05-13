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
import com.wultra.android.powerauth.networking.error.ErrorResponseObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ErrorResponseObjectTest {

    @Test
    fun `known code resolves to error code`() {
        val obj = ErrorResponseObject(code = "INVALID_REQUEST", message = "Bad request")
        assertEquals(ApiErrorCode.INVALID_REQUEST, obj.errorCode)
    }

    @Test
    fun `unknown code returns null error code`() {
        val obj = ErrorResponseObject(code = "SOME_FUTURE_CODE", message = "Unknown")
        assertNull(obj.errorCode)
    }

    @Test
    fun `message property is preserved`() {
        val obj = ErrorResponseObject(code = "ERROR_GENERIC", message = "Something went wrong")
        assertEquals("Something went wrong", obj.message)
    }

    @Test
    fun `code property is preserved`() {
        val obj = ErrorResponseObject(code = "TOO_MANY_REQUESTS", message = "Slow down")
        assertEquals("TOO_MANY_REQUESTS", obj.code)
        assertEquals(ApiErrorCode.TOO_MANY_REQUESTS, obj.errorCode)
    }

    @Test
    fun `authentication failure code resolves`() {
        val obj = ErrorResponseObject(code = "POWERAUTH_AUTH_FAIL", message = "Auth failed")
        assertEquals(ApiErrorCode.POWERAUTH_AUTH_FAIL, obj.errorCode)
        assertEquals("Auth failed", obj.message)
    }

    @Test
    fun `gson deserialization`() {
        val json = """{"code":"INVALID_REQUEST","message":"Bad request"}"""
        val obj = com.google.gson.Gson().fromJson(json, ErrorResponseObject::class.java)
        assertEquals("INVALID_REQUEST", obj.code)
        assertEquals("Bad request", obj.message)
        assertEquals(ApiErrorCode.INVALID_REQUEST, obj.errorCode)
    }

    @Test
    fun `gson deserialization with unknown code`() {
        val json = """{"code":"FUTURE_ERROR","message":"Not yet known"}"""
        val obj = com.google.gson.Gson().fromJson(json, ErrorResponseObject::class.java)
        assertEquals("FUTURE_ERROR", obj.code)
        assertNull(obj.errorCode)
    }
}
