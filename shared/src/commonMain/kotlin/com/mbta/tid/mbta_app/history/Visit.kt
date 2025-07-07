package com.mbta.tid.mbta_app.history

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
sealed class Visit {
    val timestamp: Instant = Clock.System.now()

    @Serializable data class StopVisit(val stopId: String) : Visit()
}
