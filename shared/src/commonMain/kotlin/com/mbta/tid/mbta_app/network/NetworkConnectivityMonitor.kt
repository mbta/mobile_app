package com.mbta.tid.mbta_app.network

/** Observe changes in the device's network connectivity. */
internal interface INetworkConnectivityMonitor {
    fun registerListener(onNetworkAvailable: () -> Unit, onNetworkLost: () -> Unit)
}
