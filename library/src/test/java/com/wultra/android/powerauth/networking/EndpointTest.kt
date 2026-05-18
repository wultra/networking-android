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

import com.wultra.android.powerauth.networking.data.BaseRequest
import com.wultra.android.powerauth.networking.data.StatusResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EndpointTest {

    @Test
    fun `basic endpoint has correct path`() {
        val endpoint = EndpointBasic<BaseRequest, StatusResponse>("/api/test")
        assertEquals("/api/test", endpoint.endpointUrlPath)
    }

    @Test
    fun `basic endpoint defaults to not encrypted`() {
        val endpoint = EndpointBasic<BaseRequest, StatusResponse>("/api/test")
        assertEquals(E2EEConfiguration.NOT_ENCRYPTED, endpoint.e2eeConfiguration)
    }

    @Test
    fun `basic endpoint with application scope encryption`() {
        val endpoint = EndpointBasic<BaseRequest, StatusResponse>(
            "/api/onboarding/start",
            E2EEConfiguration.APPLICATION_SCOPE
        )
        assertEquals("/api/onboarding/start", endpoint.endpointUrlPath)
        assertEquals(E2EEConfiguration.APPLICATION_SCOPE, endpoint.e2eeConfiguration)
    }

    @Test
    fun `basic endpoint with activation scope encryption`() {
        val endpoint = EndpointBasic<BaseRequest, StatusResponse>(
            "/api/secure",
            E2EEConfiguration.ACTIVATION_SCOPE
        )
        assertEquals(E2EEConfiguration.ACTIVATION_SCOPE, endpoint.e2eeConfiguration)
    }

    @Test
    fun `authenticated endpoint has correct path and uriId`() {
        val endpoint = EndpointAuthenticated<BaseRequest, StatusResponse>(
            "/api/auth/token/app/operation/history",
            "/operation/history"
        )
        assertEquals("/api/auth/token/app/operation/history", endpoint.endpointUrlPath)
        assertEquals("/operation/history", endpoint.uriId)
    }

    @Test
    fun `authenticated endpoint defaults to not encrypted`() {
        val endpoint = EndpointAuthenticated<BaseRequest, StatusResponse>("/api/sign", "/sign")
        assertEquals(E2EEConfiguration.NOT_ENCRYPTED, endpoint.e2eeConfiguration)
    }

    @Test
    fun `authenticated endpoint with encryption`() {
        val endpoint = EndpointAuthenticated<BaseRequest, StatusResponse>(
            "/api/sign",
            "/sign",
            E2EEConfiguration.APPLICATION_SCOPE
        )
        assertEquals(E2EEConfiguration.APPLICATION_SCOPE, endpoint.e2eeConfiguration)
    }

    @Test
    fun `token authenticated endpoint has correct path and token name`() {
        val endpoint = EndpointAuthenticatedWithToken<BaseRequest, StatusResponse>(
            "/api/auth/token/app/operation/list",
            "possession_universal"
        )
        assertEquals("/api/auth/token/app/operation/list", endpoint.endpointUrlPath)
        assertEquals("possession_universal", endpoint.tokenName)
    }

    @Test
    fun `token authenticated endpoint defaults to not encrypted`() {
        val endpoint = EndpointAuthenticatedWithToken<BaseRequest, StatusResponse>(
            "/api/list",
            "access-token"
        )
        assertEquals(E2EEConfiguration.NOT_ENCRYPTED, endpoint.e2eeConfiguration)
    }

    @Test
    fun `token authenticated endpoint with encryption`() {
        val endpoint = EndpointAuthenticatedWithToken<BaseRequest, StatusResponse>(
            "/api/list",
            "access-token",
            E2EEConfiguration.ACTIVATION_SCOPE
        )
        assertEquals(E2EEConfiguration.ACTIVATION_SCOPE, endpoint.e2eeConfiguration)
    }

    @Test
    fun `E2EEConfiguration enum values`() {
        val values = E2EEConfiguration.entries
        assertEquals(3, values.size)
        assertNotNull(E2EEConfiguration.APPLICATION_SCOPE)
        assertNotNull(E2EEConfiguration.ACTIVATION_SCOPE)
        assertNotNull(E2EEConfiguration.NOT_ENCRYPTED)
    }
}
