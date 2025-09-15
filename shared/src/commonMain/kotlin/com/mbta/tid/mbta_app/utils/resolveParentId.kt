package com.mbta.tid.mbta_app.utils

import com.mbta.tid.mbta_app.model.Stop

internal fun Map<String, Stop>.resolveParentId(stopId: String): String {
    val stop = this[stopId] ?: return stopId
    val parentStopId = stop.parentStationId ?: return stopId
    return resolveParentId(parentStopId)
}
