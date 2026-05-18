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

/**
 * Class that describes a server endpoint.
 *
 * @param TRequestData Type of the request data.
 * @param TResponseData Type of the response data.
 * @property endpointUrlPath URL path for the endpoint. For example "/my/custom/endpoint".
 * @property e2eeConfiguration End-to-end encryption configuration for the endpoint.
 */
abstract class Endpoint<TRequestData: BaseRequest, TResponseData: StatusResponse>(
    val endpointUrlPath: String,
    val e2eeConfiguration: E2EEConfiguration
)

/**
 * Basic endpoint not authenticated with PowerAuth.
 *
 * @param TRequestData Type of the request data.
 * @param TResponseData Type of the response data.
 * @param endpointUrlPath URL path for the endpoint. For example "/my/custom/endpoint".
 * @param e2eeConfiguration End-to-end encryption configuration for the endpoint. `NOT_ENCRYPTED` by default
 */
class EndpointBasic<TRequestData: BaseRequest, TResponseData: StatusResponse>(
    endpointUrlPath: String,
    e2eeConfiguration: E2EEConfiguration = E2EEConfiguration.NOT_ENCRYPTED
): Endpoint<TRequestData, TResponseData>(endpointUrlPath, e2eeConfiguration)

/**
 * Endpoint authenticated with PowerAuth authentication code.
 *
 * @param TRequestData Type of the request data.
 * @param TResponseData Type of the response data.
 * @param endpointUrlPath URL path for the endpoint. For example "/my/custom/endpoint".
 * @property uriId Endpoint ID. Note that this is different from endpoint URL
 * @param e2eeConfiguration End-to-end encryption configuration for the endpoint.
 */
class EndpointAuthenticated<TRequestData: BaseRequest, TResponseData: StatusResponse>(
    endpointUrlPath: String,
    val uriId: String,
    e2eeConfiguration: E2EEConfiguration = E2EEConfiguration.NOT_ENCRYPTED
): Endpoint<TRequestData, TResponseData>(endpointUrlPath, e2eeConfiguration)

@Deprecated("Renamed to EndpointAuthenticated", replaceWith = ReplaceWith("EndpointAuthenticated"))
typealias EndpointSigned<TRequestData, TResponseData> = EndpointAuthenticated<TRequestData, TResponseData>

/**
 * Endpoint authenticated with PowerAuth Token authentication code.
 *
 * @param TRequestData Type of the request data.
 * @param TResponseData Type of the response data.
 * @param endpointUrlPath  URL path for the endpoint. For example "/my/custom/endpoint".
 * @property tokenName Name of the token used for authentication code.
 * @param e2eeConfiguration End-to-end encryption configuration for the endpoint.
 */
class EndpointAuthenticatedWithToken<TRequestData: BaseRequest, TResponseData: StatusResponse>(
    endpointUrlPath: String,
    val tokenName: String,
    e2eeConfiguration: E2EEConfiguration = E2EEConfiguration.NOT_ENCRYPTED
): Endpoint<TRequestData, TResponseData>(endpointUrlPath, e2eeConfiguration)

@Deprecated("Renamed to EndpointAuthenticatedWithToken", replaceWith = ReplaceWith("EndpointAuthenticatedWithToken"))
typealias EndpointSignedWithToken<TRequestData, TResponseData> = EndpointAuthenticatedWithToken<TRequestData, TResponseData>

/** End-to-end encryption configuration for an endpoint. */
enum class E2EEConfiguration {
    /** Endpoint is encrypted with the application scope. */
    APPLICATION_SCOPE,
    /** Endpoint is encrypted with the activation scope. */
    ACTIVATION_SCOPE,
    /** Endpoint is not encrypted. */
    NOT_ENCRYPTED
}
