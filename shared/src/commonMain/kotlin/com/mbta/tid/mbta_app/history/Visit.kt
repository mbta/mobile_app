package com.mbta.tid.mbta_app.history

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
sealed class Visit {
    val timestamp: Instant = Clock.System.now()

    @Serializable data class StopVisit(val stopId: String) : Visit()
}
