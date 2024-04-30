package com.wultra.android.powerauth.networking.log

/** Log listener receives logs from the library logger for further processing. */
interface WPNLogListener {
    /**
     * If the listener should follow selected verbosity level.
     *
     * When set to true, then when [WPNLogger.VerboseLevel.ERROR] is selected as a [WPNLogger.verboseLevel], only [error] methods will be called.
     * When set to false, all methods might be called no matter the selected [WPNLogger.verboseLevel].
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