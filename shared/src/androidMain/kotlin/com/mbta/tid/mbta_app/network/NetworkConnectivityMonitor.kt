package com.mbta.tid.mbta_app.network

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

public class NetworkConnectivityMonitor(context: Context) : INetworkConnectivityMonitor {
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val connectivityManager =
        context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    private val activeCapabilities =
        listOf(
            NetworkCapabilities.NET_CAPABILITY_INTERNET,
            NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED,
            NetworkCapabilities.NET_CAPABILITY_VALIDATED,
        )

    @SuppressLint("MissingPermission")
    // Permission is included in AndroidManifest.xml
    override fun registerListener(onNetworkAvailable: () -> Unit, onNetworkLost: () -> Unit) {
        fun checkCapabilities(networkCapabilities: NetworkCapabilities) {
            if (activeCapabilities.all(networkCapabilities::hasCapability)) onNetworkAvailable()
            else onNetworkLost()
        }

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

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    checkCapabilities(networkCapabilities)
                }
            }

        // The above callback does not get called when there is no network on app startup,
        // this covers that scenario.
        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork == null) onNetworkLost()
        connectivityManager.getNetworkCapabilities(activeNetwork)?.let { checkCapabilities(it) }

        networkCallback?.let { connectivityManager.registerDefaultNetworkCallback(it) }
    }
}
