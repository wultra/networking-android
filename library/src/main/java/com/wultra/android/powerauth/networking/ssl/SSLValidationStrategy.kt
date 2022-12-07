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

import io.getlime.security.powerauth.networking.ssl.HttpClientSslNoValidationStrategy
import okhttp3.OkHttpClient

/**
 * Strategy for validating SSL certificates.
 * For concrete implementation, use static methods [default], [noValidation] & [sslPinning]
 */
abstract class SSLValidationStrategy {

    companion object {
        /**
         * Default (system) certificate validation
         */
        @JvmStatic
        fun default(): SSLValidationStrategy = DefaultSSLValidationStrategy()

        /**
         * Disables validation of SSL certificate. Use this strategy for HTTP endpoints
         * and self-signed HTTPS endpoints.
         */
        @JvmStatic
        fun noValidation(): SSLValidationStrategy = NoSSLValidationStrategy()

        /**
         * Custom SSL validation strategy.
         */
        @JvmStatic
        fun sslPinning(provider: ISSLPinningProvider): SSLValidationStrategy = PinningSSLValidationStrategy(provider)
    }

    abstract fun configure(builder: OkHttpClient.Builder): OkHttpClient.Builder
}

internal class DefaultSSLValidationStrategy: SSLValidationStrategy() {
    override fun configure(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        // Nothing to change, use default
        return builder
    }

}
internal class NoSSLValidationStrategy: SSLValidationStrategy() {
    override fun configure(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        val noValidationStrategy = HttpClientSslNoValidationStrategy()
        val trustAllCertsTrustManager = TrustAllCertsTrustManager()
        builder.sslSocketFactory(noValidationStrategy.sslSocketFactory!!, trustAllCertsTrustManager)
        builder.hostnameVerifier(noValidationStrategy.hostnameVerifier!!)
        return builder
    }

}
internal class PinningSSLValidationStrategy(private val provider: ISSLPinningProvider): SSLValidationStrategy() {
    override fun configure(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        provider.configureOkHttpClient(builder)
        return builder
    }
}

