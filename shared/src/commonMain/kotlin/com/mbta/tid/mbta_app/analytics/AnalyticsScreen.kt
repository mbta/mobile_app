package com.mbta.tid.mbta_app.analytics

enum class AnalyticsScreen(val pageName: String) {
    NearbyTransit("NearbyTransitPage"),
    TripDetails("TripDetailsPage"),
    StopDetailsFiltered("StopDetailsFilteredPage"),
    StopDetailsUnfiltered("StopDetailsUnfilteredPage"),
    Settings("SettingsPage"),
}
