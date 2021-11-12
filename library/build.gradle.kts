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
    id("kotlin-android")
    id("org.jetbrains.dokka")
}

apply("android-release-aar.gradle")

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(19)
        targetSdkVersion(30)
        versionCode = 1
        versionName = properties["VERSION_NAME"] as String
    }

    buildTypes {
        getByName("debug") {

        }
        getByName("release") {
            minifyEnabled(false)
            consumerProguardFiles("consumer-proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Bundled
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Constants.kotlinVersion}")
    implementation("androidx.annotation:annotation:1.3.0")
    implementation("com.google.code.gson:gson:2.8.8")
    implementation("com.jakewharton.threetenabp:threetenabp:1.1.1")
    // DO NOT UPGRADE OKHTTP ABOVE 3.12.X! Version 3.12 is the last version supporting TLS 1 and 1.1
    // If upgraded, the app will crash on android 4.4
    implementation("com.squareup.okhttp3:okhttp:3.12.12")

    // Dependencies
    compileOnly("com.wultra.android.powerauth:powerauth-sdk:1.6.2")
    compileOnly("io.getlime.core:rest-model-base:1.2.0")
}
