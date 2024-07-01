package com.mbta.tid.mbta_app

enum class AppVariant(val backendHost: String) {
    Staging("mobile-app-backend-dev-orange.mbtace.com"),
    Prod("mobile-app-backend.mbtace.com");

    val backendRoot = "https://$backendHost"
    val socketUrl = "wss://$backendHost/socket"
}
