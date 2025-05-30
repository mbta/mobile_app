package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.RouteStopsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RouteDetailsStopList(val stops: List<Entry>) {
    data class Entry(val stop: Stop, val connectingRoutes: List<Route>)

    data class RouteParameters(
        val availableDirections: List<Int>,
        val directions: List<Direction>,
    ) {
        constructor(
            lineOrRoute: RouteCardData.LineOrRoute,
            globalData: GlobalResponse,
        ) : this(
            globalData.routePatterns.values
                .asSequence<RoutePattern>()
                .filter<RoutePattern> { it.routeId in lineOrRoute.allRoutes.map { it.id } }
                .map { it.directionId }
                .distinct()
                .sorted()
                .toList(),
            listOf(0, 1).map { directionId ->
                val name =
                    lineOrRoute.allRoutes
                        .map { it.directionNames[directionId] }
                        .distinct()
                        .singleOrNull()
                val destination =
                    lineOrRoute.allRoutes
                        .map { it.directionDestinations[directionId] }
                        .distinct()
                        .singleOrNull()
                Direction(name, destination, directionId)
            },
        )
    }

    companion object {
        fun getLineOrRoute(
            selectionId: String,
            globalData: GlobalResponse,
        ): RouteCardData.LineOrRoute? {
            val route = globalData.getRoute(selectionId)
            val line = globalData.getLine(selectionId) ?: globalData.getLine(route?.lineId)
            return when {
                line != null && line.isGrouped ->
                    RouteCardData.LineOrRoute.Line(
                        line,
                        globalData.routesByLineId[line.id].orEmpty().toSet(),
                    )
                route != null -> RouteCardData.LineOrRoute.Route(route)
                else -> null
            }
        }

        suspend fun fromPieces(
            routeId: String,
            routeStops: RouteStopsResponse?,
            globalData: GlobalResponse,
        ): RouteDetailsStopList? =
            withContext(Dispatchers.Default) {
                if (routeStops == null) return@withContext null

                val stops =
                    routeStops.stopIds.mapNotNull { stopId ->
                        val stop =
                            globalData.getStop(stopId)?.resolveParent(globalData)
                                ?: return@mapNotNull null
                        val transferRoutes =
                            TripDetailsStopList.getTransferRoutes(stopId, routeId, globalData)
                        Entry(stop, transferRoutes)
                    }

                RouteDetailsStopList(stops)
            }
    }
}
