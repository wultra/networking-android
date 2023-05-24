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

class ConnectionMonitor(context: Context) {

    enum class Status(val value: String) {
        UNKNOWN("unknown"),
        NO_CONNECTION("noConnection"),
        WIFI("wifi"),
        CELLULAR("cellular"),
        WIRED("wired")
    }

    var status: Status = Status.NO_CONNECTION
        private set

    init {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val networkRequest = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
                connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        status = getConnectionStatus(connectivityManager)
                    }

                    override fun onLost(network: Network) {
                        status = Status.NO_CONNECTION
                    }
                })
                // Initial connection status check
                status = getConnectionStatus(connectivityManager)
            } else {
                // for older android
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                status = if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                    getOlderConnectionStatus(activeNetworkInfo)
                } else {
                    Status.UNKNOWN
                }
            }
        } catch (e: Throwable) {
            status = Status.UNKNOWN
            Logger.d("Failed to create Connectivity Manager with Exception: $e")
        }
    }

    private fun getConnectionStatus(connectivityManager: ConnectivityManager): Status {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

            when {
                networkCapabilities == null -> Status.NO_CONNECTION
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Status.WIFI
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Status.CELLULAR
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Status.WIRED
                else -> Status.UNKNOWN
            }
        } else {
            Status.UNKNOWN
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
