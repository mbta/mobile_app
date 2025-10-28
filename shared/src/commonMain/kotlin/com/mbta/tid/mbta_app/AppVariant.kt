package com.mbta.tid.mbta_app

public enum class AppVariant(internal val backendRoot: String, mapEnvironment: MapEnvironment) {
    Local(localBackendOrigin, MapEnvironment.Staging),
    DevOrange("https://mobile-app-backend-dev-orange.mbtace.com", MapEnvironment.Staging),
    Staging("https://mobile-app-backend-staging.mbtace.com", MapEnvironment.Staging),
    Prod("https://mobile-app-backend.mbtace.com", MapEnvironment.Prod);

    private enum class MapEnvironment(val lightMapStyle: String, val darkMapStyle: String) {
        Staging(
            "mapbox://styles/mbtamobileapp/cm02vf0cf00cu01pnfgnu5onh",
            "mapbox://styles/mbtamobileapp/cm02vgvsp003c01ombyewgu70",
        ),
        Prod(
            "mapbox://styles/mbtamobileapp/cm02vja9z00dz01psa55ba0ew",
            "mapbox://styles/mbtamobileapp/cm02vjs2000e001psbu2v10p9",
        ),
    }

    public val lightMapStyle: String = mapEnvironment.lightMapStyle
    public val darkMapStyle: String = mapEnvironment.darkMapStyle

    // https -> wss, http -> ws
    public val socketUrl: String =
        backendRoot.replace(Regex("^http(?<sIfEncrypted>s?)://"), $$"ws${sIfEncrypted}://") +
            "/socket"
}
