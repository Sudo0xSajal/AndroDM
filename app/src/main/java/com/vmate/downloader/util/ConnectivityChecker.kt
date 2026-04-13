package com.vmate.downloader.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object ConnectivityChecker {

    /**
     * Returns `true` when the device has an active, validated internet connection.
     * [NET_CAPABILITY_INTERNET] confirms a network path exists; [NET_CAPABILITY_VALIDATED]
     * additionally confirms the network can reach the internet (i.e., is not a captive portal
     * or disconnected stub).
     */
    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
