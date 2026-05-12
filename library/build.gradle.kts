/*
 * Copyright 2021 Wultra s.r.o.
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

plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.dokka")
}

apply<com.wultra.plugin.WultraAndroidReleasePlugin>()

android {
    namespace = "com.wultra.android.powerauth"
    compileSdk = Constants.Android.compileSdkVersion
    buildToolsVersion = Constants.Android.buildToolsVersion

    defaultConfig {
        minSdk = Constants.Android.minSdkVersion
        @Suppress("DEPRECATION")
        targetSdk = Constants.Android.targetSdkVersion

        configIntField("VERSION_CODE", 1)
        configStringField("VERSION_NAME", properties["VERSION_NAME"] as String)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFiles("consumer-proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = Constants.Java.sourceCompatibility
        targetCompatibility = Constants.Java.targetCompatibility
        kotlinOptions {
            jvmTarget = Constants.Java.kotlinJvmTarget
            suppressWarnings = false
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    // Custom ktlint script
    tasks.register("ktlint") {
        logger.lifecycle("ktlint")
        exec {
            commandLine = listOf("./../scripts/lint.sh", "--no-error")
        }
    }

    // Make ktlint run before build
    tasks.getByName("preBuild").dependsOn("ktlint")
}

dependencies {
    // Bundled
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Constants.BuildScript.kotlinVersion}")
    implementation("androidx.annotation:annotation:1.10.0")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Dependencies
    compileOnly("com.wultra.android.powerauth:powerauth-sdk:2.0.0-SNAPSHOT")
    compileOnly("io.getlime.core:rest-model-base:1.12.0")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    // Instrumented tests
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("com.wultra.android.powerauth:powerauth-sdk:2.0.0-SNAPSHOT")
}
