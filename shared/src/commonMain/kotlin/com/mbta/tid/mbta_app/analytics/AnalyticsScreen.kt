package com.mbta.tid.mbta_app.analytics

public enum class AnalyticsScreen(internal val pageName: String) {
    Favorites("FavoritesPage"),
    NearbyTransit("NearbyTransitPage"),
    RouteDetails("RouteDetailsPage"),
    RoutePicker("RoutePickerPage"),
    TripDetails("TripDetailsPage"),
    StopDetailsFiltered("StopDetailsFilteredPage"),
    StopDetailsUnfiltered("StopDetailsUnfilteredPage"),
    Settings("SettingsPage"),
}
