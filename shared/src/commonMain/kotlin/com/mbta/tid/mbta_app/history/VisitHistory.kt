package com.mbta.tid.mbta_app.history

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
class VisitHistory {
    companion object {
        const val RETRIEVE_COUNT = 10
        const val SAVE_COUNT = 50
    }

    private var visits: MutableList<Visit> = mutableListOf()

    fun add(visit: Visit) {
        visits.remove(visit)
        visits.add(0, visit)
        if (visits.size > SAVE_COUNT) {
            visits = visits.take(SAVE_COUNT).toMutableList()
        }
    }

    fun latest(n: Int = RETRIEVE_COUNT): List<Visit> {
        return visits.take(n)
    }
}

@Serializable
sealed class Visit {
    val timestamp: Instant = Clock.System.now()

    @Serializable data class StopVisit(val stopId: String) : Visit()
}
