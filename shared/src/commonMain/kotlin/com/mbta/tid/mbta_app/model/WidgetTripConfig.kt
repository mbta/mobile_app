package com.mbta.tid.mbta_app.model

import kotlinx.serialization.Serializable

/**
 * Configuration for a trip widget instance.
 *
 * Stored per widget (keyed by appWidgetId) in file-based storage.
 */
@Serializable
public data class WidgetTripConfig(
    val fromStopId: String,
    val toStopId: String,
    val fromLabel: String = "",
    val toLabel: String = "",
)
