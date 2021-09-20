/*
 * Copyright (c) 2020, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
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
