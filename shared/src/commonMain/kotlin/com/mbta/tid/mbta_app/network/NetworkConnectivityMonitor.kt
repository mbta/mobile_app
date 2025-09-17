package com.mbta.tid.mbta_app.network

/** Observe changes in the device's network connectivity. */
public interface INetworkConnectivityMonitor {
    public fun registerListener(onNetworkAvailable: () -> Unit, onNetworkLost: () -> Unit)
}
