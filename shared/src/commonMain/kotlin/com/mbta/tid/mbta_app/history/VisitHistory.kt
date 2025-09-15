package com.mbta.tid.mbta_app.history

import kotlinx.serialization.Serializable

@Serializable
public class VisitHistory {
    internal companion object {
        const val RETRIEVE_COUNT = 10
        const val SAVE_COUNT = 50
    }

    private var visits: MutableList<Visit> = mutableListOf()

    public fun add(visit: Visit) {
        visits.remove(visit)
        visits.add(0, visit)
        if (visits.size > SAVE_COUNT) {
            visits = visits.take(SAVE_COUNT).toMutableList()
        }
    }

    internal fun latest(n: Int = RETRIEVE_COUNT): List<Visit> {
        return visits.take(n)
    }
}
