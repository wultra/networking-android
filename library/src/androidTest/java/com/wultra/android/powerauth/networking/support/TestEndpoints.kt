package com.wultra.android.powerauth.networking.support

import com.wultra.android.powerauth.networking.E2EEConfiguration
import com.wultra.android.powerauth.networking.EndpointBasic
import com.wultra.android.powerauth.networking.EndpointSigned
import com.wultra.android.powerauth.networking.EndpointSignedWithToken
import com.wultra.android.powerauth.networking.data.BaseRequest
import com.wultra.android.powerauth.networking.data.ObjectRequest
import com.wultra.android.powerauth.networking.data.StatusResponse

// ============================================================================
// Test endpoints & request models
// ============================================================================

object TestEndpoints {
    val todo = EndpointBasic<BaseRequest, StatusResponse>("/posts")

    val start = EndpointBasic<StartObjectRequest, StatusResponse>(
        "/api/onboarding/start",
        E2EEConfiguration.APPLICATION_SCOPE
    )

    val history = EndpointSigned<BaseRequest, StatusResponse>(
        "/api/auth/token/app/operation/history",
        "/operation/history"
    )

    val operationList = EndpointSignedWithToken<BaseRequest, StatusResponse>(
        "/api/auth/token/app/operation/list",
        "possession_universal"
    )

    val failingStart = EndpointBasic<BaseRequest, StatusResponse>(
        "/api/onboarding/start",
        E2EEConfiguration.ACTIVATION_SCOPE
    )

    data class StartRequest(val identification: Map<String, String>)
    class StartObjectRequest(requestObject: StartRequest) : ObjectRequest<StartRequest>(requestObject)
}