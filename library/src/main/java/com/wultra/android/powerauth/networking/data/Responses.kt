/*
 * Copyright (c) 2020, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
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