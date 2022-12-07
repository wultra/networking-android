/*
 * Copyright 2022 Wultra s.r.o.
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

package com.wultra.android.powerauth.networking.processing

import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonToken
import com.wultra.android.powerauth.networking.Logger
import okhttp3.ResponseBody
import java.io.IOException
import java.io.Reader

/**
 * GSON converter for parsing responses.
 *
 * Inspired by Retrofit implementation.
 */
@PublishedApi
internal class GsonResponseBodyConverter<T>(private val gson: Gson, private val adapter: TypeAdapter<T>) {

    @Throws(IOException::class)
    fun convert(value: ResponseBody): T {
        return convert(value.charStream())
    }

    @Throws(IOException::class)
    fun convert(value: ByteArray): T {
        return convert(value.inputStream().reader())
    }

    @Throws(IOException::class)
    fun convert(reader: Reader): T {
        val jsonReader = gson.newJsonReader(reader)
        try {
            reader.use {
                val result = adapter.read(jsonReader)
                if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                    throw JsonIOException("JSON document was not fully consumed.")
                }
                return result
            }
        } catch (t: Throwable) {
            Logger.e("Failed to process response", t)
            throw t
        }
    }
}
