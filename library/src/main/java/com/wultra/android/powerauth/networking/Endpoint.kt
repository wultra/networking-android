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