package com.mbta.tid.mbta_app.network

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.Network

internal class NetworkConnectivityMonitor(context: Context) : INetworkConnectivityMonitor {
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val connectivityManager =
        context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

    @SuppressLint("MissingPermission")
    // Permission is included in AndroidManifest.xml
    override fun registerListener(onNetworkAvailable: () -> Unit, onNetworkLost: () -> Unit) {
        networkCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    onNetworkAvailable()
                }

                override fun onUnavailable() {
                    onNetworkLost()
                }

                override fun onLost(network: Network) {
                    onNetworkLost()
                }
            }
        // The above callback does not get called when there is no network on app startup,
        // this covers that scenario.
        if (connectivityManager.activeNetwork == null) {
            onNetworkLost()
        }
        networkCallback?.let { connectivityManager.registerDefaultNetworkCallback(it) }
    }
}
