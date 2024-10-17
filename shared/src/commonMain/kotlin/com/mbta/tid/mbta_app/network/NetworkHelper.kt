package com.mbta.tid.mbta_app.network

interface INetworkHelper {
    fun registerListener(onNetworkAvailable: () -> Unit, onNetworkLost: () -> Unit)

    fun unregisterListener()
}
