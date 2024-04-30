/*
 * Copyright (c) 2024, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.powerauth.networking

/** Log listener receives logs from the library logger for further processing. */
interface WPNLogListener {
    /**
     * If the listener should follow selected verbosity level.
     *
     * When set to true, then when [Logger.VerboseLevel.ERROR] is selected as a [Logger.verboseLevel], only [error] methods will be called.
     * When set to false, all methods might be called no matter the selected [Logger.verboseLevel].
     */
    val followVerboseLevel: Boolean

    /** Error log */
    fun error(message: String)

    /** Warning log */
    fun warning(message: String)

    /** Info log */
    fun info(message: String)

    /** Debug log */
    fun debug(message: String)
}
