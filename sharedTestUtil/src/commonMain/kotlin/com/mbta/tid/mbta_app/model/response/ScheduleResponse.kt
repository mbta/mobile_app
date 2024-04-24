package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder

fun ScheduleResponse.Companion.fromObjectCollection(objects: ObjectCollectionBuilder) =
    ScheduleResponse(objects.schedules.values.toList(), objects.trips)
