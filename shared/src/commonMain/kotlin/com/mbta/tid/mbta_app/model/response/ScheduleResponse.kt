package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Schedule
import com.mbta.tid.mbta_app.model.Trip
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleResponse(
    internal val schedules: List<Schedule>,
    internal val trips: Map<String, Trip>
) {
    constructor(
        objects: ObjectCollectionBuilder
    ) : this(objects.schedules.values.toList(), objects.trips)
}
