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
import com.wultra.android.powerauth.networking.data.BaseRequest
import com.wultra.android.powerauth.networking.data.ObjectRequest
import com.wultra.android.powerauth.networking.data.ObjectResponse
import com.wultra.android.powerauth.networking.data.StatusResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests Gson serialization/deserialization of request and response models.
 * Mirrors Apple's WPNBaseNetworkingObjectsTests.
 */
class BaseNetworkingObjectsTest {

    private val gson: Gson = GsonBuilder().create()

    // --- Test models ---

    data class Payload(
        @SerializedName("value") val value: String
    )

    class PayloadRequest(payload: Payload) : ObjectRequest<Payload>(payload)

    class PayloadResponse(
        @SerializedName("responseObject") val payload: Payload?,
        status: Status
    ) : ObjectResponse<Payload?>(payload, status)

    // --- Request tests ---

    @Test
    fun `BaseRequest serializes to empty JSON object`() {
        val json = gson.toJson(BaseRequest())
        assertEquals("{}", json)
    }

    @Test
    fun `ObjectRequest encodes requestObject envelope`() {
        val request = PayloadRequest(Payload("hello"))
        val json = gson.toJson(request)

        // Verify the JSON contains the requestObject wrapper
        val parsed = gson.fromJson(json, Map::class.java)
        @Suppress("UNCHECKED_CAST")
        val requestObject = parsed["requestObject"] as? Map<String, Any>
        assertNotNull("JSON should contain requestObject", requestObject)
        assertEquals("hello", requestObject!!["value"])
    }

    // --- Response tests ---

    @Test
    fun `StatusResponse OK deserializes`() {
        val json = """{"status":"OK"}"""
        val response = gson.fromJson(json, StatusResponse::class.java)
        assertEquals(StatusResponse.Status.OK, response.status)
    }

    @Test
    fun `StatusResponse ERROR deserializes`() {
        val json = """{"status":"ERROR"}"""
        val response = gson.fromJson(json, StatusResponse::class.java)
        assertEquals(StatusResponse.Status.ERROR, response.status)
    }

    @Test
    fun `StatusResponse serializes back to JSON`() {
        val response = StatusResponse(StatusResponse.Status.OK)
        val json = gson.toJson(response)
        assert(json.contains("\"OK\"")) { "Serialized JSON should contain OK status" }
    }

    @Test
    fun `ObjectResponse success envelope deserializes`() {
        val json = """{"status":"OK","responseObject":{"value":"hello"}}"""
        val response = gson.fromJson(json, PayloadResponse::class.java)

        assertEquals(StatusResponse.Status.OK, response.status)
        assertNotNull(response.payload)
        assertEquals("hello", response.payload!!.value)
        assertEquals("hello", response.responseObject!!.value)
    }

    @Test
    fun `ObjectResponse error envelope deserializes`() {
        val json = """{"status":"ERROR","responseObject":null}"""
        val response = gson.fromJson(json, PayloadResponse::class.java)

        assertEquals(StatusResponse.Status.ERROR, response.status)
        assertNull(response.payload)
    }

    @Test
    fun `ObjectResponse with missing responseObject`() {
        val json = """{"status":"OK"}"""
        val response = gson.fromJson(json, PayloadResponse::class.java)

        assertEquals(StatusResponse.Status.OK, response.status)
        assertNull(response.payload)
    }

    // --- Error response tests ---

    @Test
    fun `error response with error object deserializes`() {
        val json = """{"status":"ERROR","responseObject":{"code":"INVALID_REQUEST","message":"Bad request"}}"""

        // Parse as a generic map to verify structure
        val parsed = gson.fromJson(json, Map::class.java)
        assertEquals("ERROR", parsed["status"])
        @Suppress("UNCHECKED_CAST")
        val responseObject = parsed["responseObject"] as Map<String, Any>
        assertEquals("INVALID_REQUEST", responseObject["code"])
        assertEquals("Bad request", responseObject["message"])
    }

    // --- Status enum tests ---

    @Test
    fun `Status enum has expected values`() {
        assertEquals(2, StatusResponse.Status.entries.size)
        assertNotNull(StatusResponse.Status.OK)
        assertNotNull(StatusResponse.Status.ERROR)
    }

    @Test
    fun `Status OK serializes as string`() {
        val json = gson.toJson(StatusResponse.Status.OK)
        assertEquals("\"OK\"", json)
    }

    @Test
    fun `Status ERROR serializes as string`() {
        val json = gson.toJson(StatusResponse.Status.ERROR)
        assertEquals("\"ERROR\"", json)
    }
}
