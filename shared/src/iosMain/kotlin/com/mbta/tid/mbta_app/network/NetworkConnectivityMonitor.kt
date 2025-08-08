package com.mbta.tid.mbta_app.network

import platform.Network.*
import platform.darwin.dispatch_get_main_queue

internal class NetworkConnectivityMonitor : INetworkConnectivityMonitor {
    private val monitor = nw_path_monitor_create()

    override fun registerListener(onNetworkAvailable: () -> Unit, onNetworkLost: () -> Unit) {
        nw_path_monitor_set_update_handler(monitor) { path ->
            val pathStatus = nw_path_get_status(path)

            if (pathStatus == nw_path_status_satisfied) {
                onNetworkAvailable()
            } else {
                onNetworkLost()
            }
        }

        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
    }
}
