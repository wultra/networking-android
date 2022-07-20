/*
 * Copyright (c) 2022, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

import com.android.build.api.dsl.BaseFlavor
import java.util.*

fun wrapString(value: String?) = if (value != null) "\"${value}\"" else "null"

fun BaseFlavor.configStringField(name: String, value: String?) = buildConfigField("String", name, wrapString(value))
fun BaseFlavor.configBoolField(name: String, value: Boolean) = buildConfigField("boolean", name, "$value")
fun BaseFlavor.configLongField(name: String, value: Long) = buildConfigField("Long", name, "${value}L")
fun BaseFlavor.configIntField(name: String, value: Int) = buildConfigField("Integer", name, "$value")
