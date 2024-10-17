package com.mbta.tid.mbta_app.network

import platform.Network.*
import platform.darwin.dispatch_get_main_queue

class NetworkHelper : INetworkHelper {
    private val monitor = nw_path_monitor_create()

    override fun registerListener(onNetworkAvailable: () -> Unit, onNetworkLost: () -> Unit) {
        println("KB HIT: register listener ios")
        nw_path_monitor_set_update_handler(monitor) { path ->
            val pathStatus = nw_path_get_status(path)

            if (pathStatus == nw_path_status_satisfied) {
                println("KB HIT: path satisfied")

                onNetworkAvailable()
            } else {
                println("KB HIT: path lost")

                onNetworkLost()
            }
        }

        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
    }

    override fun unregisterListener() {
        nw_path_monitor_cancel(monitor)
    }
}
