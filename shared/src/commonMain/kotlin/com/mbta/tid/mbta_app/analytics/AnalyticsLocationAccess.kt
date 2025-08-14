package com.mbta.tid.mbta_app.analytics

public enum class AnalyticsLocationAccess(internal val recordedValue: String) {
    Precise("precise"),
    Approximate("approximate"),
    Off("off"),
}
