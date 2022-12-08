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
import com.google.gson.TypeAdapter
import com.wultra.android.powerauth.networking.Logger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.Charset

/**
 * GSON converter for serializing data to bytes.
 *
 * Inspired by Retrofit implementation.
 */
@PublishedApi
internal class GsonRequestBodyBytes<T>(private val gson: Gson, private val adapter: TypeAdapter<T>) {

    @Throws(IOException::class)
    fun convert(value: T): ByteArray {
        try {
            val outputStream = ByteArrayOutputStream()
            val writer = OutputStreamWriter(outputStream, UTF_8)
            gson.newJsonWriter(writer).use {
                adapter.write(it, value)
            }
            return outputStream.toByteArray()
        } catch (t: Throwable) {
            Logger.e("Failed to process request", t)
            throw t
        }
    }

    companion object {
        private val UTF_8 = Charset.forName("UTF-8")
    }
}