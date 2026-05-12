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

import com.wultra.android.powerauth.networking.log.HeaderBlockList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeaderBlockListTest {

    @Test
    fun `default block list contains standard headers`() {
        val blockList = HeaderBlockList()
        val list = blockList.toList()
        assertTrue(list.contains("content-type"))
        assertTrue(list.contains("accept-language"))
        assertTrue(list.contains("user-agent"))
        assertTrue(list.contains("content-length"))
        assertTrue(list.contains("date"))
        assertTrue(list.contains("server"))
    }

    @Test
    fun `add header normalizes to lowercase`() {
        val blockList = HeaderBlockList()
        blockList.add("X-Custom-Header")
        assertTrue(blockList.toList().contains("x-custom-header"))
    }

    @Test
    fun `addAll normalizes to lowercase`() {
        val blockList = HeaderBlockList()
        blockList.addAll(listOf("Authorization", "X-Request-ID"))
        val list = blockList.toList()
        assertTrue(list.contains("authorization"))
        assertTrue(list.contains("x-request-id"))
    }

    @Test
    fun `remove header normalizes to lowercase`() {
        val blockList = HeaderBlockList()
        assertTrue(blockList.toList().contains("content-type"))
        blockList.remove("Content-Type")
        assertFalse(blockList.toList().contains("content-type"))
    }

    @Test
    fun `removeAll normalizes to lowercase`() {
        val blockList = HeaderBlockList()
        blockList.removeAll(listOf("Content-Type", "User-Agent"))
        val list = blockList.toList()
        assertFalse(list.contains("content-type"))
        assertFalse(list.contains("user-agent"))
    }

    @Test
    fun `remove non-existing header returns false`() {
        val blockList = HeaderBlockList()
        assertFalse(blockList.remove("non-existing-header"))
    }

    @Test
    fun `add returns true`() {
        val blockList = HeaderBlockList()
        assertTrue(blockList.add("new-header"))
    }

    @Test
    fun `toList returns snapshot`() {
        val blockList = HeaderBlockList()
        val snapshot1 = blockList.toList()
        blockList.add("new-header")
        val snapshot2 = blockList.toList()
        // snapshot1 should not be affected by subsequent add
        assertFalse(snapshot1.contains("new-header"))
        assertTrue(snapshot2.contains("new-header"))
    }

    @Test
    fun `default list size`() {
        val blockList = HeaderBlockList()
        val expectedDefaults = listOf(
            "accept-language", "content-type", "content-length", "transfer-encoding",
            "date", "server", "user-agent", "connection", "x-content-type-options",
            "x-xss-protection", "cache-control", "pragma", "expires", "x-frame-options", "vary"
        )
        assertEquals(expectedDefaults.size, blockList.toList().size)
    }
}
