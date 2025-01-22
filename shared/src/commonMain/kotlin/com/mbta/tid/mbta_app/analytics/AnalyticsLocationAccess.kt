package com.mbta.tid.mbta_app.analytics

enum class AnalyticsLocationAccess(val recordedValue: String) {
    Precise("precise"),
    Approximate("approximate"),
    Off("off")
}
