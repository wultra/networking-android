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

package com.wultra.android.powerauth.networking.error

/**
 * Error codes denoting known API errors.
 */
@Suppress("unused")
enum class ApiErrorCode(val message: String) {

    /* COMMON ERRORS */

    /** When unexpected error happened */
    ERROR_GENERIC("ERROR_GENERIC"),

    /** General authentication failure (wrong password, wrong activation state, etc...) **/
    POWERAUTH_AUTH_FAIL("POWERAUTH_AUTH_FAIL"),

    /** Invalid request sent - missing request object in the request **/
    INVALID_REQUEST("INVALID_REQUEST"),

    /** Activation is not valid (it is different from configured activation) **/
    INVALID_ACTIVATION("INVALID_ACTIVATION"),

    /** Error in case that PowerAuth authentication fails **/
    ERR_AUTHENTICATION("ERR_AUTHENTICATION"),

    /** Error during secure vault unlocking **/
    ERR_SECURE_VAULT("ERR_SECURE_VAULT"),

    /** Returned in case encryption or decryption fails **/
    ERR_ENCRYPTION("ERR_ENCRYPTION"),

    /* PUSH ERRORS */

    /** Error code for a situation when registration to push notification fails **/
    PUSH_REGISTRATION_FAILED("PUSH_REGISTRATION_FAILED"),

    /* OPERATIONS ERRORS */

    /** Operation is already finished **/
    OPERATION_ALREADY_FINISHED("OPERATION_ALREADY_FINISHED"),

    /** Operation is already failed **/
    OPERATION_ALREADY_FAILED("OPERATION_ALREADY_FAILED"),

    /** Operation is canceled **/
    OPERATION_ALREADY_CANCELED("OPERATION_ALREADY_CANCELED"),

    /** Operation is expired **/
    OPERATION_EXPIRED("OPERATION_EXPIRED"),

    /* ACTIVATION SPAWN ERRORS */

    /** Unable to fetch activation code. */
    ACTIVATION_CODE_FAILED("ACTIVATION_CODE_FAILED"),

    /* IDENTITY ONBOARDING ERRORS */

    /** Onboarding process failed or failed to start */
    ONBOARDING_FAILED("ONBOARDING_FAILED"),

    /** Document is invalid. */
    INVALID_DOCUMENT("INVALID_DOCUMENT"),

    /** Identity verification failed. */
    IDENTITY_VERIFICATION_FAILED("IDENTITY_VERIFICATION_FAILED"),

    /** Failed to resend onboarding OTP (probably requested too soon) */
    ONBOARDING_OTP_FAILED("ONBOARDING_OTP_FAILED");

    companion object {
        private val map = mutableMapOf<String, ApiErrorCode>()

        init {
            values().forEach { ec -> map[ec.message] = ec }
        }

        /**
         * Decodes the code into the [ApiErrorCode].
         * @param code Code received from the backend.
         * @return Enum value if the code is recognized.
         */
        fun errorCodeFromCodeString(code: String): ApiErrorCode? = map[code]
    }
}