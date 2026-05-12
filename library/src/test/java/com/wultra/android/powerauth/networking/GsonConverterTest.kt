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
import com.google.gson.TypeAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.wultra.android.powerauth.networking.data.BaseRequest
import com.wultra.android.powerauth.networking.data.ObjectRequest
import com.wultra.android.powerauth.networking.data.ObjectResponse
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.processing.GsonRequestBodyBytes
import com.wultra.android.powerauth.networking.processing.GsonResponseBodyConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests for GsonRequestBodyBytes and GsonResponseBodyConverter.
 * Mirrors Apple's WPNHttpRequestTests for request/response processing.
 */
class GsonConverterTest {

    private val gson: Gson = GsonBuilder().create()

    // --- Test models ---

    data class Payload(
        @SerializedName("value") val value: String
    )

    class TestObjectRequest(payload: Payload) : ObjectRequest<Payload>(payload)

    class TestObjectResponse(
        responseObject: Payload? = null,
        status: StatusResponse.Status = StatusResponse.Status.OK
    ) : ObjectResponse<Payload?>(responseObject, status)

    // --- GsonRequestBodyBytes tests ---

    @Test
    fun `request body bytes serializes BaseRequest`() {
        val adapter = gson.getAdapter(TypeToken.get(BaseRequest::class.java))
        val converter = GsonRequestBodyBytes(gson, adapter)
        val bytes = converter.convert(BaseRequest())
        val json = String(bytes, Charsets.UTF_8)
        assertEquals("{}", json)
    }

    @Test
    fun `request body bytes serializes ObjectRequest with envelope`() {
        val adapter = gson.getAdapter(TypeToken.get(TestObjectRequest::class.java))
        val converter = GsonRequestBodyBytes(gson, adapter)
        val request = TestObjectRequest(Payload("hello"))
        val bytes = converter.convert(request)
        val json = String(bytes, Charsets.UTF_8)

        // Verify the JSON contains requestObject wrapper
        val parsed = gson.fromJson(json, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val requestObject = parsed["requestObject"] as? Map<String, Any>
        assertNotNull("Should contain requestObject", requestObject)
        assertEquals("hello", requestObject!!["value"])
    }

    @Test
    fun `request body bytes produces valid UTF-8`() {
        val adapter = gson.getAdapter(TypeToken.get(TestObjectRequest::class.java))
        val converter = GsonRequestBodyBytes(gson, adapter)
        val request = TestObjectRequest(Payload("héllo wörld ñ"))
        val bytes = converter.convert(request)
        val json = String(bytes, Charsets.UTF_8)
        assertTrue("Should preserve Unicode characters", json.contains("héllo wörld ñ"))
    }

    // --- GsonResponseBodyConverter tests ---

    @Test
    fun `response converter decodes success envelope`() {
        val adapter = gson.getAdapter(TypeToken.get(TestObjectResponse::class.java))
        val converter = GsonResponseBodyConverter(gson, adapter)
        val bytes = """{"status":"OK","responseObject":{"value":"done"}}""".toByteArray()
        val response = converter.convert(bytes)

        assertEquals(StatusResponse.Status.OK, response.status)
        assertNotNull(response.responseObject)
        assertEquals("done", response.responseObject!!.value)
    }

    @Test
    fun `response converter decodes error envelope`() {
        val adapter = gson.getAdapter(TypeToken.get(TestObjectResponse::class.java))
        val converter = GsonResponseBodyConverter(gson, adapter)
        val bytes = """{"status":"ERROR","responseObject":null}""".toByteArray()
        val response = converter.convert(bytes)

        assertEquals(StatusResponse.Status.ERROR, response.status)
    }

    @Test
    fun `response converter decodes StatusResponse`() {
        val adapter = gson.getAdapter(TypeToken.get(StatusResponse::class.java))
        val converter = GsonResponseBodyConverter(gson, adapter)
        val bytes = """{"status":"OK"}""".toByteArray()
        val response = converter.convert(bytes)

        assertEquals(StatusResponse.Status.OK, response.status)
    }

    @Test(expected = Exception::class)
    fun `response converter throws on malformed JSON`() {
        val adapter = gson.getAdapter(TypeToken.get(TestObjectResponse::class.java))
        val converter = GsonResponseBodyConverter(gson, adapter)
        val bytes = "not valid json{{{".toByteArray()
        converter.convert(bytes)
    }

    // --- Serialization failure tests ---

    @Test
    fun `request body bytes throws when serialization fails`() {
        val failingAdapter = object : TypeAdapter<BaseRequest>() {
            override fun write(out: JsonWriter, value: BaseRequest?) {
                throw RuntimeException("Simulated serialization failure")
            }
            override fun read(reader: JsonReader): BaseRequest {
                return BaseRequest()
            }
        }
        val converter = GsonRequestBodyBytes(gson, failingAdapter)
        try {
            converter.convert(BaseRequest())
            fail("Expected serialization to throw")
        } catch (e: RuntimeException) {
            assertEquals("Simulated serialization failure", e.message)
        }
    }

    // --- Round-trip tests ---

    @Test
    fun `request serialization produces bytes that can be deserialized`() {
        val requestAdapter = gson.getAdapter(TypeToken.get(TestObjectRequest::class.java))
        val requestConverter = GsonRequestBodyBytes(gson, requestAdapter)
        val request = TestObjectRequest(Payload("round-trip"))
        val bytes = requestConverter.convert(request)

        // Deserialize the request bytes and verify the payload
        val json = String(bytes, Charsets.UTF_8)
        val parsed = gson.fromJson(json, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val requestObject = parsed["requestObject"] as Map<String, Any>
        assertEquals("round-trip", requestObject["value"])
    }
}
