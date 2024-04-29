package com.mbta.tid.mbta_app

enum class AppVariant(val backendHost: String) {
    Staging("mobile-app-backend-staging.mbtace.com"),
    // TODO use prod backend once created
    Prod("mobile-app-backend-staging.mbtace.com");

    val backendRoot = "https://$backendHost"
    val socketUrl = "wss://$backendHost/socket"
}
