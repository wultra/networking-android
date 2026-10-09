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

import com.wultra.android.powerauth.networking.error.ApiError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Tests for the helpers extracted from [Api.makeBlockingCall]: the `0`-timeout mapping and the
 * listener wrapper that keeps callbacks off the calling (PowerAuthSDK serial executor) thread.
 */
class BlockingCallSupportTest {

    // --- blockingCallTimeoutMillis ---

    @Test
    fun `sums positive timeouts`() {
        assertEquals(3_000L, blockingCallTimeoutMillis(connectionTimeoutMillis = 1_000L, readTimeoutMillis = 2_000L))
    }

    @Test
    fun `maps zero connection timeout to unbounded`() {
        assertEquals(Long.MAX_VALUE, blockingCallTimeoutMillis(connectionTimeoutMillis = 0L, readTimeoutMillis = 2_000L))
    }

    @Test
    fun `maps zero read timeout to unbounded`() {
        assertEquals(Long.MAX_VALUE, blockingCallTimeoutMillis(connectionTimeoutMillis = 1_000L, readTimeoutMillis = 0L))
    }

    @Test
    fun `maps negative timeout to unbounded`() {
        assertEquals(Long.MAX_VALUE, blockingCallTimeoutMillis(connectionTimeoutMillis = -1L, readTimeoutMillis = 2_000L))
    }

    // --- dispatchingListener ---

    @Test
    fun `dispatchingListener runs onSuccess on the executor, not the caller thread`() {
        val executor = Executors.newSingleThreadExecutor()
        val latch = CountDownLatch(1)
        var invokedOnThread: Thread? = null

        val wrapped = dispatchingListener<String>(
            executor,
            object : IApiCallResponseListener<String> {
                override fun onSuccess(result: String) {
                    invokedOnThread = Thread.currentThread()
                    latch.countDown()
                }
                override fun onFailure(error: ApiError) = Unit
            }
        )

        val callerThread = Thread.currentThread()
        wrapped.onSuccess("result")

        assertTrue("Callback should complete within 5s", latch.await(5, TimeUnit.SECONDS))
        assertNotEquals("Listener must not run on the calling thread", callerThread, invokedOnThread)
        executor.shutdown()
    }

    @Test
    fun `dispatchingListener runs onFailure on the executor, not the caller thread`() {
        val executor = Executors.newSingleThreadExecutor()
        val latch = CountDownLatch(1)
        var invokedOnThread: Thread? = null

        val wrapped = dispatchingListener<String>(
            executor,
            object : IApiCallResponseListener<String> {
                override fun onSuccess(result: String) = Unit
                override fun onFailure(error: ApiError) {
                    invokedOnThread = Thread.currentThread()
                    latch.countDown()
                }
            }
        )

        val callerThread = Thread.currentThread()
        wrapped.onFailure(ApiError(Exception("test")))

        assertTrue("Callback should complete within 5s", latch.await(5, TimeUnit.SECONDS))
        assertNotEquals("Listener must not run on the calling thread", callerThread, invokedOnThread)
        executor.shutdown()
    }

    // --- deadlock reproduction ---

    /**
     * Submits a "second blocking call" to [serial] and waits *indefinitely* for it - mirroring
     * an app that makes another blocking PowerAuth call from within a callback and synchronously
     * awaits its result, with no timeout of its own.
     */
    private fun runNestedCallAndAwait(serial: java.util.concurrent.ExecutorService) {
        val nestedLatch = CountDownLatch(1)
        serial.execute { nestedLatch.countDown() }
        nestedLatch.await()
    }

    @Test
    fun `undispatched listener deadlocks a nested call on the shared serial executor`() {
        val serial = Executors.newSingleThreadExecutor()
        val outerDone = CountDownLatch(1)

        // Pre-fix behaviour: the listener runs directly on `serial`, still occupying its only
        // thread while it waits for the nested call - which can never run on that same thread.
        serial.execute {
            runNestedCallAndAwait(serial)
            outerDone.countDown()
        }

        assertTrue(
            "The `serial` thread is stuck waiting on itself and must not complete",
            !outerDone.await(2, TimeUnit.SECONDS)
        )
        serial.shutdownNow()
    }

    @Test
    fun `dispatchingListener lets a nested call on the shared serial executor complete`() {
        val serial = Executors.newSingleThreadExecutor()
        val callbackExecutor = Executors.newSingleThreadExecutor()
        val outerDone = CountDownLatch(1)

        // Post-fix behaviour: the "listener" body runs on callbackExecutor, so `serial` is
        // already free by the time the nested call needs it.
        serial.execute {
            callbackExecutor.execute {
                runNestedCallAndAwait(serial)
                outerDone.countDown()
            }
        }

        assertTrue("The nested call must complete once `serial` is free", outerDone.await(5, TimeUnit.SECONDS))
        serial.shutdown()
        callbackExecutor.shutdown()
    }
}
