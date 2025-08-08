package com.mbta.tid.mbta_app

public enum class AppVariant(
    internal val backendHost: String,
    public val lightMapStyle: String,
    public val darkMapStyle: String,
) {
    DevOrange(
        "mobile-app-backend-dev-orange.mbtace.com",
        "mapbox://styles/mbtamobileapp/cm02vf0cf00cu01pnfgnu5onh",
        "mapbox://styles/mbtamobileapp/cm02vgvsp003c01ombyewgu70",
    ),
    Staging(
        "mobile-app-backend-staging.mbtace.com",
        "mapbox://styles/mbtamobileapp/cm02vf0cf00cu01pnfgnu5onh",
        "mapbox://styles/mbtamobileapp/cm02vgvsp003c01ombyewgu70",
    ),
    Prod(
        "mobile-app-backend.mbtace.com",
        "mapbox://styles/mbtamobileapp/cm02vja9z00dz01psa55ba0ew",
        "mapbox://styles/mbtamobileapp/cm02vjs2000e001psbu2v10p9",
    );

    internal val backendRoot = "https://$backendHost"
    public val socketUrl: String = "wss://$backendHost/socket"
}
