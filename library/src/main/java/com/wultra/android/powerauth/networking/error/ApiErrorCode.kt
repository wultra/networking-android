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

    /** Error during activation **/
    ERR_ACTIVATION("ERR_ACTIVATION"),

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

    /** An onboarding process limit reached (e.g. too many reset attempts for identity verification or maximum error score exceeded). **/
    ONBOARDING_PROCESS_LIMIT_REACHED("ONBOARDING_PROCESS_LIMIT_REACHED"),

    /** Too many attempts to start an onboarding process for a user. **/
    ONBOARDING_TOO_MANY_PROCESSES("TOO_MANY_ONBOARDING_PROCESSES"),

    /** Failed to resend onboarding OTP (probably requested too soon) */
    ONBOARDING_OTP_FAILED("ONBOARDING_OTP_FAILED"),

    /** Document is invalid. */
    IDENTITY_INVALID_DOCUMENT("INVALID_DOCUMENT"),

    /** Document submit failed */
    IDENTITY_DOCUMENT_SUBMIT_FAILED("DOCUMENT_SUBMIT_FAILED"),

    /** Identity verification failed. */
    IDENTITY_VERIFICATION_FAILED("IDENTITY_VERIFICATION_FAILED"),

    /** Identity verification limit reached (e.g. exceeded number of upload attempts). */
    IDENTITY_VERIFICATION_LIMIT_REACHED("IDENTITY_VERIFICATION_LIMIT_REACHED"),

    /** Verification of documents failed */
    IDENTITY_DOCUMENT_VERIFICATION_FAILED("DOCUMENT_VERIFICATION_FAILED"),

    /** Presence check failed */
    IDENTITY_PRESENCE_CHECK_FAILED("PRESENCE_CHECK_FAILED"),

    /** Presence check is not enabled */
    IDENTITY_PRESENCE_CHECK_NOT_ENABLED("PRESENCE_CHECK_NOT_ENABLED"),

    /** Maximum limit of presence check attempts was exceeded. */
    IDENTITY_PRESENCE_CHECK_LIMIT_REACHED("PRESENCE_CHECK_LIMIT_REACHED"),

    /* OTHER */

    /** Too many same requests */
    TOO_MANY_REQUESTS("TOO_MANY_REQUESTS"),

    /** Communication with remote system failed */
    REMOTE_COMMUNICATION_ERROR("REMOTE_COMMUNICATION_ERROR");

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