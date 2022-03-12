/*
 * Copyright (c) 2021, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.powerauth.networking

import com.wultra.android.powerauth.networking.data.BaseRequest
import com.wultra.android.powerauth.networking.data.StatusResponse

abstract class Endpoint<TRequestData: BaseRequest, TResponseData: StatusResponse>(val endpointUrlPath: String)

/**
 * Basic endpoint - without any authorization header.
 */
class EndpointBasic<TRequestData: BaseRequest, TResponseData: StatusResponse>(endpointUrlPath: String):
    Endpoint<TRequestData, TResponseData>(endpointUrlPath)

/**
 * Signed endpoint via PowerAuth signature.
 */
class EndpointSigned<TRequestData: BaseRequest, TResponseData: StatusResponse>(endpointUrlPath: String, val uriId: String):
    Endpoint<TRequestData, TResponseData>(endpointUrlPath)

/**
 * Signed endpoint with token.
 */
class EndpointSignedWithToken<TRequestData: BaseRequest, TResponseData: StatusResponse>(endpointUrlPath: String, val tokenName: String):
    Endpoint<TRequestData, TResponseData>(endpointUrlPath)