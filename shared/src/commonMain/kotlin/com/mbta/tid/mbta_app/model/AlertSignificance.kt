package com.mbta.tid.mbta_app.model

public enum class AlertSignificance : Comparable<AlertSignificance> {
    // defined in ascending order so that Major > Secondary > Minor > None
    /** Hidden everywhere. */
    None,
    /**
     * Shown in filtered stop details but not indicated in nearby transit or at downstream stops.
     */
    Minor,
    /** May be shown in nearby transit and stop details if enabled, but not called out as ahead. */
    Accessibility,
    /**
     * Shown in nearby transit alongside predictions. Shown in stop details. Called out as ahead but
     * not listed at downstream stops.
     */
    Secondary,
    /**
     * Replaces predictions in nearby transit, stop details, and trip details. Called out as ahead.
     */
    Major,
}
