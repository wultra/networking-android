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

package com.wultra.android.powerauth.networking.utils

import android.content.Context
import android.os.Build
import java.util.*

/**
 * Get currently selected locale.
 */
fun Context.getCurrentLocale(): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        this.resources.configuration.locales.get(0)
    } else {
        this.resources.configuration.locale
    }
}


/**
 * Returns a well-formed IETF BCP 47 language tag representing
 * this locale.
 *
 * @author Tomas Kypta, tomas.kypta@wultra.com
 */
fun Locale.toBcp47LanguageTag(): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        return this.toLanguageTag()
    }

    val sep = '-'       // we will use a dash as per BCP 47
    var language = this.language
    var region = this.country
    var variant = variant

    // special case for Norwegian Nynorsk since "NY" cannot be a variant as per BCP 47
    // this goes before the string matching since "NY" wont pass the variant checks
    if (language == "no" && region == "NO" && variant == "NY") {
        language = "nn"
        region = "NO"
        variant = ""
    }

    if (language.isEmpty() || !language.matches("\\p{Alpha}{2,8}".toRegex())) {
        language = "und"       // Follow the Locale#toLanguageTag() implementation
        // which says to return "und" for Undetermined
    } else if (language == "iw") {
        language = "he"        // correct deprecated "Hebrew"
    } else if (language == "in") {
        language = "id"        // correct deprecated "Indonesian"
    } else if (language == "ji") {
        language = "yi"        // correct deprecated "Yiddish"
    }

    // ensure valid country code, if not well formed, it's omitted
    if (!region.matches("\\p{Alpha}{2}|\\p{Digit}{3}".toRegex())) {
        region = ""
    }

    // variant subtags that begin with a letter must be at least 5 characters long
    if (!variant.matches("\\p{Alnum}{5,8}|\\p{Digit}\\p{Alnum}{3}".toRegex())) {
        variant = ""
    }

    val bcp47Tag = StringBuilder(language)
    if (!region.isEmpty()) {
        bcp47Tag.append(sep).append(region)
    }
    if (!variant.isEmpty()) {
        bcp47Tag.append(sep).append(variant)
    }

    return bcp47Tag.toString()
}