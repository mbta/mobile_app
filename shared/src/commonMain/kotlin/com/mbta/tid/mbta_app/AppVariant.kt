package com.mbta.tid.mbta_app

enum class AppVariant(
    val backendHost: String,
    val lightMapStyle: String,
    val darkMapStyle: String
) {
    Staging(
        "mobile-app-backend-staging.mbtace.com",
        "mapbox://styles/mbtamobileapp/cm02vf0cf00cu01pnfgnu5onh",
        "mapbox://styles/mbtamobileapp/cm02vgvsp003c01ombyewgu70"
    ),
    Prod(
        "mobile-app-backend.mbtace.com",
        "mapbox://styles/mbtamobileapp/cm02vja9z00dz01psa55ba0ew",
        "mapbox://styles/mbtamobileapp/cm02vjs2000e001psbu2v10p9"
    );

    val backendRoot = "https://$backendHost"
    val socketUrl = "wss://$backendHost/socket"
}
