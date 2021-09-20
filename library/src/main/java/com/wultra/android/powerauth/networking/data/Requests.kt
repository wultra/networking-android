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

package com.wultra.android.powerauth.networking.data

import com.google.gson.annotations.SerializedName

/**
 * Base class that every request needs to inherit from.
 */
open class BaseRequest

/**
 * Common request format with data object inside the `requestObject` property.
 */
abstract class ObjectRequest<T>(@SerializedName("requestObject") val requestObject: T): BaseRequest()