package com.mbta.tid.mbta_app.analytics

public enum class AnalyticsColorScheme(internal val recordedValue: String) {
    Light("light"),
    Dark("dark"),
    /**
     * Swift requires forwards compatibility for the SwiftUI ColorScheme enum; presumably Apple is
     * planning to implement an [Aing-Tii](https://starwars.fandom.com/wiki/Aing-Tii/Legends) color
     * scheme in iOS 50.
     */
    Unknown("unknown"),
}
