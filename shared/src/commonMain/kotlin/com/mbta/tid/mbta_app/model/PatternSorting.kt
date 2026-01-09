package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.units.Length
import org.maplibre.spatialk.units.extensions.feet

internal object PatternSorting {
    private fun veryCloseBucket(distance: Length) =
        when {
            distance <= 100.feet -> 1
            else -> 2
        }

    private fun patternServiceBucket(leafData: RouteCardData.Leaf, now: EasternTimeInstant) =
        when {
            // showing either a trip or an alert
            leafData.hasMajorAlerts(now) || leafData.upcomingTrips.isNotEmpty() -> 1
            // service ended
            leafData.hasSchedulesToday -> 2
            // no service today
            else -> 3
        }

    private fun subwayBucket(route: Route) =
        when {
            route.type.isSubway() -> 1
            else -> 2
        }

    fun compareRouteCards(
        sortByDistanceFrom: Position?,
        context: RouteCardData.Context,
    ): Comparator<RouteCardData> =
        compareBy(
            if (sortByDistanceFrom != null) {
                { veryCloseBucket(it.distanceFrom(sortByDistanceFrom)) }
            } else {
                { 0 }
            },
            { patternServiceBucket(it.stopData.first().data.first(), it.at) },
            if (context != RouteCardData.Context.Favorites) {
                { subwayBucket(it.lineOrRoute.sortRoute) }
            } else {
                { 0 }
            },
            if (sortByDistanceFrom != null) {
                { it.distanceFrom(sortByDistanceFrom) }
            } else {
                { 0 }
            },
            { it.lineOrRoute.sortRoute },
        )

    fun compareStopsOnRoute(
        sortByDistanceFrom: Position?
    ): Comparator<RouteCardData.RouteStopData> =
        compareBy(
            if (sortByDistanceFrom != null) {
                { veryCloseBucket(it.stop.distanceFrom(sortByDistanceFrom)) }
            } else {
                { 0 }
            },
            { patternServiceBucket(it.data.first(), EasternTimeInstant.now()) },
            if (sortByDistanceFrom != null) {
                { it.stop.distanceFrom(sortByDistanceFrom) }
            } else {
                { 0 }
            },
        )

    fun compareLeavesAtStop(): Comparator<RouteCardData.Leaf> =
        compareBy({ patternServiceBucket(it, EasternTimeInstant.now()) }, { it.directionId })
}
