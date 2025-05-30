package com.mbta.tid.mbta_app.analytics

enum class AnalyticsScreen(val pageName: String) {
    Favorites("FavoritesPage"),
    NearbyTransit("NearbyTransitPage"),
    RouteDetails("RouteDetailsPage"),
    TripDetails("TripDetailsPage"),
    StopDetailsFiltered("StopDetailsFilteredPage"),
    StopDetailsUnfiltered("StopDetailsUnfilteredPage"),
    Settings("SettingsPage"),
}
