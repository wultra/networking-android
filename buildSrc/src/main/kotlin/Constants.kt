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

import org.gradle.api.JavaVersion

object Constants {
    object BuildScript {
        // These have to be defined in buildSrc/gradle.properties
        // It's the only way to make them available in buildSrc/build.gradle.kts
        val androidPluginVersion: String by System.getProperties()
        val kotlinVersion: String by System.getProperties()
        val dokkaVersion: String by System.getProperties()
    }

    object Java {
        val sourceCompatibility = JavaVersion.VERSION_11
        val targetCompatibility = JavaVersion.VERSION_11
        const val kotlinJvmTarget = "11"
    }

    object Android {
        const val compileSdkVersion = 31
        const val targetSdkVersion = 31
        const val minSdkVersion = 21
        const val buildToolsVersion = "33.0.0"
    }
}