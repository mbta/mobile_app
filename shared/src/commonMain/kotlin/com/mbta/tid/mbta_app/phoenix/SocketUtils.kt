package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.Backend

class SocketUtils {
    companion object {
        val url = "wss://${Backend.mobileBackendHost}/socket"
    }
}
