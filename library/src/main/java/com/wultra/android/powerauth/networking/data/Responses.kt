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

package com.wultra.android.powerauth.networking.data

import com.google.gson.annotations.SerializedName

/**
 * Default response class. All responses must inherit from this class.
 */
open class StatusResponse(@SerializedName("status") val status: Status) {

    enum class Status {
        @SerializedName("OK")
        OK,

        @SerializedName("ERROR")
        ERROR
    }
}

/**
 * Common response where data returned are inside the `responseObject` property.
 */
abstract class ObjectResponse<T>(@SerializedName("responseObject") val responseObject: T, status: Status): StatusResponse(status)