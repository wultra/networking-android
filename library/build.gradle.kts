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

android {
    namespace = "com.wultra.android.powerauth"
    compileSdk = Constants.Android.compileSdkVersion
    buildToolsVersion = Constants.Android.buildToolsVersion

    defaultConfig {
        minSdk = Constants.Android.minSdkVersion
        @Suppress("DEPRECATION")
        targetSdk = Constants.Android.targetSdkVersion

        // since Android Gradle Plugin 4.1.0
        // VERSION_CODE and VERSION_NAME are not generated for libraries
        configIntField("VERSION_CODE", 1)
        configStringField("VERSION_NAME", properties["VERSION_NAME"] as String)
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
}

dependencies {
    // Bundled
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Constants.BuildScript.kotlinVersion}")
    implementation("androidx.annotation:annotation:1.7.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.jakewharton.threetenabp:threetenabp:1.1.1")
    // DO NOT UPGRADE OKHTTP ABOVE 3.12.X! Version 3.12 is the last version supporting TLS 1 and 1.1
    // If upgraded, the app will crash on android 4.4
    implementation("com.squareup.okhttp3:okhttp:3.12.13")

    // Dependencies
    compileOnly("com.wultra.android.powerauth:powerauth-sdk:1.8.0")
    compileOnly("io.getlime.core:rest-model-base:1.2.0")
}

apply("android-release-aar.gradle")