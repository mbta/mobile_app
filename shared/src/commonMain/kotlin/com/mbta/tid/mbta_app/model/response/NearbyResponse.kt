package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.utils.EasternTimeInstant

public data class NearbyResponse(val stopIds: List<String>) {
    public constructor(objects: ObjectCollectionBuilder) : this(objects.stops.keys.toList())

    public fun filter(
        globalData: GlobalResponse,
        alerts: AlertsStreamDataResponse?,
        atTime: EasternTimeInstant,
    ): List<String> {
        val originalStopIdSet = stopIds.toSet()
        val originalStopOrder = stopIds.mapIndexed { index, id -> Pair(id, index) }.toMap()

        val parentToAllStops = Stop.resolvedParentToAllStops(stopIds, globalData)

        val filteredStopIds =
            RoutePattern.patternsGroupedByLineOrRouteAndStop(
                    parentToAllStops,
                    globalData,
                    context = RouteCardData.Context.NearbyTransit,
                    alerts,
                    atTime,
                )
                .flatMap {
                    it.value.keys.flatMap { stop ->
                        stop.childStopIds.toSet().plus(stop.id).intersect(originalStopIdSet)
                    }
                }
                .distinct()
                .sortedBy { originalStopOrder[it] }

        return filteredStopIds
    }
}
