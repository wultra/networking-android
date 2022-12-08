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

package com.wultra.android.powerauth.networking.error

/**
 * Error reported in functions using callback with `Result<T>` type.
 */
class ApiErrorException(
    /**
     * Error code extracted from the response's error.
     */
    val errorCode: ApiErrorCode? = null,
    /**
     * The detailed message.
     */
    message: String? = null,
    /**
     * The cause of the exception.
     */
    cause: Throwable? = null

): Exception(message, cause) {

    /**
     * Construct exception from existing [ApiError].
     *
     * @param apiError Instance of [ApiError].
     */
    constructor(apiError: ApiError) : this(apiError.error, apiError.e.message, apiError.e)

    /**
     * Construct exception from another exception. If exception is one from known exceptions,
     * then the [errorCode] property is also initialized.
     */
    constructor(cause: Throwable) : this(ApiError(cause).error, cause.message, cause)

    companion object {
        /**
         * Wrap any [Throwable] object into [ApiErrorException]. If throwable is already [ApiErrorException]
         * then return the object as is.
         */
        fun wrap(t: Throwable): ApiErrorException {
            return if (t is ApiErrorException) t else ApiErrorException(t)
        }
    }
}