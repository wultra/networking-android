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