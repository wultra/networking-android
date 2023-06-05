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
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.*
import android.os.Build
import androidx.annotation.RequiresApi
import com.wultra.android.powerauth.networking.Logger

class AppUtils {
    companion object {
        @Throws(PackageManager.NameNotFoundException::class)
        internal fun getMyPackageBasicInfo(appContext: Context): PackageInfo {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            return appContext.packageManager.getPackageInfo(appContext.packageName, flags)
        }
    }
}

object ConnectionMonitor {

    enum class Status(val value: String) {
        UNKNOWN("unknown"),
        NO_CONNECTION("noConnection"),
        WIFI("wifi"),
        CELLULAR("cellular"),
        WIRED("wired")
    }

    private var status: Status = Status.NO_CONNECTION

    fun getConnectivityStatus(context: Context): String {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    getConnectionStatus(connectivityManager)
            } else {
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                    getOlderConnectionStatus(activeNetworkInfo)
                } else {
                    Status.UNKNOWN
                }
            }
        } catch (e: Throwable) {
            status = Status.UNKNOWN
            Logger.d("Failed to create Connectivity Manager with Exception: $e")
        }
        return status.value
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getConnectionStatus(connectivityManager: ConnectivityManager): Status {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        return when {
            networkCapabilities == null -> Status.NO_CONNECTION
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Status.WIFI
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Status.CELLULAR
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Status.WIRED
            else -> Status.UNKNOWN
        }
    }

    private fun getOlderConnectionStatus(networkInfo: NetworkInfo): Status {
        return when (networkInfo.type) {
            ConnectivityManager.TYPE_WIFI -> Status.WIFI
            ConnectivityManager.TYPE_MOBILE -> Status.CELLULAR
            ConnectivityManager.TYPE_ETHERNET -> Status.WIRED
            else -> Status.UNKNOWN
        }
    }
}
