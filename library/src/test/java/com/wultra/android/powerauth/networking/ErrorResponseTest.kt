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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.wultra.android.powerauth.networking.error.ErrorResponse
import com.wultra.android.powerauth.networking.error.ErrorResponseObject
import com.wultra.android.powerauth.networking.error.ApiErrorCode
import com.wultra.android.powerauth.networking.error.ApiHttpException
import com.wultra.android.powerauth.networking.data.StatusResponse
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for error response deserialization and ApiHttpException.
 * These tests verify the error handling pipeline without PowerAuth SDK dependencies.
 */
class ErrorResponseTest {

    private val gson: Gson = GsonBuilder().create()

    @Test
    fun `ErrorResponse deserializes from JSON`() {
        val json = """{
            "status": "ERROR",
            "responseObject": {
                "code": "INVALID_REQUEST",
                "message": "Bad request"
            }
        }"""
        val response = gson.fromJson(json, ErrorResponse::class.java)

        assertEquals(StatusResponse.Status.ERROR, response.status)
        assertNotNull(response.responseObject)
        assertEquals("INVALID_REQUEST", response.responseObject.code)
        assertEquals("Bad request", response.responseObject.message)
        assertEquals(ApiErrorCode.INVALID_REQUEST, response.responseObject.errorCode)
    }

    @Test
    fun `ErrorResponse with unknown error code`() {
        val json = """{
            "status": "ERROR",
            "responseObject": {
                "code": "SOME_NEW_ERROR",
                "message": "Future error type"
            }
        }"""
        val response = gson.fromJson(json, ErrorResponse::class.java)

        assertEquals("SOME_NEW_ERROR", response.responseObject.code)
        assertNull(response.responseObject.errorCode)
    }

    @Test
    fun `ApiHttpException preserves response code`() {
        val okResponse = buildOkHttpResponse(401, "Unauthorized")
        val exception = ApiHttpException(okResponse)

        assertEquals(401, exception.code)
        assertEquals("Unauthorized", exception.message)
    }

    @Test
    fun `ApiHttpException with error response`() {
        val okResponse = buildOkHttpResponse(400, "Bad Request")
        val errorObj = ErrorResponseObject("INVALID_REQUEST", "Missing field")
        val errorResponse = ErrorResponse(errorObj, StatusResponse.Status.ERROR)
        val exception = ApiHttpException(okResponse, errorResponse)

        assertEquals(400, exception.code)
        assertNotNull(exception.errorResponse)
        assertEquals(ApiErrorCode.INVALID_REQUEST, exception.errorResponse!!.responseObject.errorCode)
    }

    @Test
    fun `ApiHttpException with null error response`() {
        val okResponse = buildOkHttpResponse(500, "Internal Server Error")
        val exception = ApiHttpException(okResponse, null)

        assertEquals(500, exception.code)
        assertNull(exception.errorResponse)
    }

    @Test
    fun `ApiHttpException with cause`() {
        val okResponse = buildOkHttpResponse(502, "Bad Gateway")
        val cause = RuntimeException("Connection failed")
        val exception = ApiHttpException(okResponse, null, cause)

        assertEquals(502, exception.code)
        assertNotNull(exception.cause)
        assertEquals("Connection failed", exception.cause!!.message)
    }

    @Test
    fun `ApiHttpException message format`() {
        val okResponse = buildOkHttpResponse(403, "Forbidden")
        val exception = ApiHttpException(okResponse)

        // The exception message should contain HTTP code and message
        assertNotNull(exception.message)
        assert(exception.toString().contains("403")) { "Should contain status code" }
    }

    private fun buildOkHttpResponse(code: Int, message: String): Response {
        return Response.Builder()
            .request(Request.Builder().url("https://example.com/test").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .build()
    }
}
