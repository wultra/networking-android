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

package com.wultra.android.powerauth.networking.ssl

import okhttp3.OkHttpClient


/**
 * SSL pinning provider configures HTTP client to verify SSL certificates
 */
interface ISSLPinningProvider {
    /**
     * Called when HTTP client builder needs to be configured
     *
     * @param builder OkHttpClient builder to configure
     */
    fun configureOkHttpClient(builder: OkHttpClient.Builder)
}