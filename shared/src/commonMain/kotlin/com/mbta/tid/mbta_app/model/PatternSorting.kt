package com.mbta.tid.mbta_app.model

import io.github.dellisd.spatialk.geojson.Position

object PatternSorting {
    private fun patternServiceBucket(leafData: ILeafData) =
        when {
            // showing either a trip or an alert
            leafData.hasMajorAlerts || leafData.upcomingTrips.orEmpty().isNotEmpty() -> 1
            // service ended
            leafData.hasSchedulesToday -> 2
            // no service today
            else -> 3
        }

    private fun pinnedRouteBucket(route: Route, pinnedRoutes: Set<String>) =
        when {
            pinnedRoutes.contains(route.id) || pinnedRoutes.contains(route.lineId) -> 1
            else -> 2
        }

    private fun subwayBucket(route: Route) =
        when {
            route.type.isSubway() -> 1
            else -> 2
        }

    fun compareStaticPatterns(): Comparator<NearbyStaticData.StaticPatterns> =
        compareBy(
            { it.patterns.first().directionId },
            {
                when (it) {
                    is NearbyStaticData.StaticPatterns.ByDirection -> -1
                    is NearbyStaticData.StaticPatterns.ByHeadsign -> 1
                }
            },
            { it.patterns.first() }
        )

    fun compareRealtimePatterns(): Comparator<RealtimePatterns> =
        compareBy(
            ::patternServiceBucket,
            { it.directionId() },
            {
                when (it) {
                    is RealtimePatterns.ByDirection -> -1
                    is RealtimePatterns.ByHeadsign -> 1
                }
            },
            { it.patterns.first() },
        )

    fun comparePatternsByStop(
        pinnedRoutes: Set<String>,
        sortByDistanceFrom: Position?
    ): Comparator<PatternsByStop> =
        compareBy<PatternsByStop>(
                { pinnedRouteBucket(it.representativeRoute, pinnedRoutes) },
                { patternServiceBucket(it.patterns.first()) },
                { subwayBucket(it.representativeRoute) },
                if (sortByDistanceFrom != null) {
                    { it.distanceFrom(sortByDistanceFrom) }
                } else {
                    { 0 }
                },
            )
            .thenBy(compareRealtimePatterns()) { it.patterns.first() }

    fun compareTransitWithStops(): Comparator<NearbyStaticData.TransitWithStops> =
        compareBy({ subwayBucket(it.sortRoute()) }, { it.sortRoute() })

    fun compareStopsAssociated(
        pinnedRoutes: Set<String>,
        sortByDistanceFrom: Position?
    ): Comparator<StopsAssociated> =
        compareBy(
            { pinnedRouteBucket(it.sortRoute(), pinnedRoutes) },
            { patternServiceBucket(it.patternsByStop.first().patterns.first()) },
            { subwayBucket(it.sortRoute()) },
            if (sortByDistanceFrom != null) {
                { it.distanceFrom(sortByDistanceFrom) }
            } else {
                { 0 }
            },
            { it.sortRoute() },
        )

    fun compareRouteCards(
        pinnedRoutes: Set<String>,
        sortByDistanceFrom: Position?
    ): Comparator<RouteCardData> =
        compareBy(
            { pinnedRouteBucket(it.lineOrRoute.sortRoute, pinnedRoutes) },
            { patternServiceBucket(it.stopData.first().data.first()) },
            { subwayBucket(it.lineOrRoute.sortRoute) },
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
            { patternServiceBucket(it.data.first()) },
            if (sortByDistanceFrom != null) {
                { it.stop.distanceFrom(sortByDistanceFrom) }
            } else {
                { 0 }
            }
        )

    fun compareLeavesAtStop(): Comparator<RouteCardData.Leaf> =
        compareBy({ patternServiceBucket(it) }, { it.directionId })
}
