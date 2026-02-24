package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Schedule
import com.mbta.tid.mbta_app.model.Trip
import kotlinx.serialization.Serializable

@Serializable
public data class ScheduleResponse(
    internal val schedules: List<Schedule>,
    internal val trips: Map<String, Trip>,
) {
    public constructor(
        objects: ObjectCollectionBuilder
    ) : this(objects.schedules.values.toList(), objects.trips)

    internal fun getSchedulesTodayByPattern(): Map<String, Boolean> {
        val scheduledTrips = this.trips
        val hasSchedules: MutableMap<String, Boolean> = mutableMapOf()
        for (schedule in this.schedules) {
            val trip = scheduledTrips[schedule.tripId]
            val patternId = trip?.routePatternId ?: continue
            hasSchedules[patternId] = true
        }
        return hasSchedules
    }

    override fun toString(): String = "[ScheduleResponse]"

    public companion object {
        public fun merge(schedules: List<ScheduleResponse>): ScheduleResponse {
            return ScheduleResponse(
                schedules.fold(emptyList()) { joined, next -> joined + next.schedules },
                schedules.fold(emptyMap()) { joined, next -> joined + next.trips },
            )
        }
    }
}
