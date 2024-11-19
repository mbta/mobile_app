package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.NearbyHierarchy.DirectionOrHeadsign
import com.mbta.tid.mbta_app.model.NearbyHierarchy.LineOrRoute
import com.mbta.tid.mbta_app.model.NearbyHierarchy.NearbyLeaf
import com.mbta.tid.mbta_app.model.NearbyHierarchy.RouteAtStop
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.distance
import kotlinx.datetime.Instant

private typealias InnerLayer = Map<DirectionOrHeadsign, NearbyLeaf>

private typealias MiddleLayerData = RouteAtStop<List<Direction>, InnerLayer>

private typealias MiddleLayer = Map<Stop, MiddleLayerData>

private typealias OuterLayer = Map<LineOrRoute, MiddleLayer>

data class NearbyHierarchy(private val data: OuterLayer) {
    sealed interface LineOrRoute {
        data class Line(
            val line: com.mbta.tid.mbta_app.model.Line,
            val routes: Set<com.mbta.tid.mbta_app.model.Route>
        ) : LineOrRoute

        data class Route(val route: com.mbta.tid.mbta_app.model.Route) : LineOrRoute
    }

    data class RouteAtStop<StopData, T>(val stopData: StopData, val data: T) {
        fun <U> map(f: (T) -> U): RouteAtStop<StopData, U> = RouteAtStop(stopData, f(data))
    }

    sealed interface DirectionOrHeadsign {
        data class Direction(val direction: com.mbta.tid.mbta_app.model.Direction) :
            DirectionOrHeadsign

        data class Headsign(val headsign: String, val route: Route, val line: Line?) :
            DirectionOrHeadsign
    }

    data class PartialHierarchy<StopData, T>(
        val data: Map<LineOrRoute, Map<Stop, RouteAtStop<StopData, T>>>
    ) {
        fun <U> map(f: (T) -> U): PartialHierarchy<StopData, U> =
            data
                .mapValues { (_, routeData) ->
                    routeData.mapValues { (_, routeAtStop) -> routeAtStop.map(f) }
                }
                .let(::PartialHierarchy)

        fun <SD2, U> map(
            processStopData: (StopData) -> SD2,
            processData: (T) -> U
        ): PartialHierarchy<SD2, U> =
            data
                .mapValues { (_, routeData) ->
                    routeData.mapValues { (_, routeAtStop) ->
                        RouteAtStop(
                            processStopData(routeAtStop.stopData),
                            processData(routeAtStop.data)
                        )
                    }
                }
                .let(::PartialHierarchy)

        fun <U> mapInContext(
            f: (LineOrRoute, Stop, StopData, T) -> U
        ): PartialHierarchy<StopData, U> =
            data
                .mapValues { (lineOrRoute, routeData) ->
                    routeData.mapValues { (stop, routeAtStop) ->
                        routeAtStop.map { f(lineOrRoute, stop, routeAtStop.stopData, it) }
                    }
                }
                .let(::PartialHierarchy)

        fun <SD2> mapStopDataInContext(
            f: (LineOrRoute, Stop, StopData, T) -> SD2
        ): PartialHierarchy<SD2, T> =
            data
                .mapValues { (lineOrRoute, routeData) ->
                    routeData.mapValues { (stop, routeAtStop) ->
                        RouteAtStop(
                            f(lineOrRoute, stop, routeAtStop.stopData, routeAtStop.data),
                            routeAtStop.data
                        )
                    }
                }
                .let(::PartialHierarchy)

        fun <SD2, U> zip(
            other: PartialHierarchy<SD2, U>
        ): PartialHierarchy<Pair<StopData?, SD2?>, Pair<T?, U?>> =
            (data.keys + other.data.keys)
                .associateWith { lineOrRoute ->
                    (data[lineOrRoute]?.keys.orEmpty() + other.data[lineOrRoute]?.keys.orEmpty())
                        .associateWith { stop ->
                            val ownData = data[lineOrRoute]?.get(stop)
                            val otherData = other.data[lineOrRoute]?.get(stop)
                            RouteAtStop(
                                Pair(ownData?.stopData, otherData?.stopData),
                                Pair(ownData?.data, otherData?.data)
                            )
                        }
                }
                .let(::PartialHierarchy)
    }

    fun <U> mapEntries(f: (Map.Entry<LineOrRoute, MiddleLayer>) -> U): List<U> = data.map(f)

    data class NearbyLeaf(
        val childStopIds: Set<String>,
        val routePatterns: Set<RoutePattern?>,
        val upcomingTrips: List<UpcomingTrip>,
    ) {
        fun filterCancellations(isSubway: Boolean) =
            this.copy(upcomingTrips = upcomingTrips.filterCancellations(isSubway))

        fun filterPatterns(predicate: (RoutePattern?) -> Boolean): NearbyLeaf {
            val newPatterns = routePatterns.filter(predicate).toSet()
            val newPatternIds = newPatterns.map { it?.id }
            val newTrips = upcomingTrips.filter { it.trip.routePatternId in newPatternIds }
            return this.copy(routePatterns = newPatterns, upcomingTrips = newTrips)
        }
    }

    data class MutableNearbyLeaf(
        val childStopIds: MutableSet<String>,
        val routePatterns: MutableSet<RoutePattern?>,
        val upcomingTrips: MutableList<UpcomingTrip>,
    ) {
        constructor() : this(mutableSetOf(), mutableSetOf(), mutableListOf())

        fun finished() = NearbyLeaf(childStopIds, routePatterns, upcomingTrips)
    }

    companion object {
        fun fromSchedules(
            global: GlobalResponse,
            schedules: ScheduleResponse,
        ): PartialHierarchy<Unit, List<Schedule>> {
            val result = mutablePartialHierarchy<Unit, MutableList<Schedule>>()

            for (schedule in schedules.schedules) {
                val parentStop = parentStop(global, schedule.stopId) ?: continue
                val lineOrRoute = lineOrRoute(global, schedule.routeId) ?: continue
                result
                    .getOrPut(lineOrRoute, ::mutableMapOf)
                    .getOrPut(parentStop) { RouteAtStop(Unit, mutableListOf()) }
                    .data
                    .add(schedule)
            }

            return PartialHierarchy(result).map { it }
        }

        fun fromPredictions(
            global: GlobalResponse,
            predictions: PredictionsStreamDataResponse,
        ): PartialHierarchy<Unit, List<Prediction>> {
            val result = mutablePartialHierarchy<Unit, MutableList<Prediction>>()

            for (prediction in predictions.predictions.values) {
                val parentStop = parentStop(global, prediction.stopId) ?: continue
                val lineOrRoute = lineOrRoute(global, prediction.routeId) ?: continue
                result
                    .getOrPut(lineOrRoute, ::mutableMapOf)
                    .getOrPut(parentStop) { RouteAtStop(Unit, mutableListOf()) }
                    .data
                    .add(prediction)
            }

            return PartialHierarchy(result).map { it }
        }

        fun fromRealtime(
            global: GlobalResponse,
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse,
            filterAtTime: Instant,
        ): PartialHierarchy<Unit, List<UpcomingTrip>> =
            schedules
                ?.let { fromSchedules(global, it) }
                .orEmpty()
                .zip(fromPredictions(global, predictions))
                .map(
                    {},
                    { (schedulesHere, predictionsHere) ->
                        UpcomingTrip.tripsFromData(
                            global.stops,
                            schedulesHere.orEmpty(),
                            predictionsHere.orEmpty(),
                            schedules?.trips.orEmpty() + predictions.trips,
                            predictions.vehicles,
                            filterAtTime,
                        )
                    }
                )

        fun fromStaticData(
            staticData: NearbyStaticData
        ): PartialHierarchy<List<Direction>, Map<String, Set<RoutePattern>>> {
            val routePatternsInHierarchy =
                staticData.data.associate { transitWithStops ->
                    val lineOrRoute =
                        when (transitWithStops) {
                            is NearbyStaticData.TransitWithStops.ByLine ->
                                LineOrRoute.Line(
                                    transitWithStops.line,
                                    transitWithStops.routes.filterNot { it.isShuttle }.toSet()
                                )
                            is NearbyStaticData.TransitWithStops.ByRoute ->
                                LineOrRoute.Route(transitWithStops.route)
                        }
                    val stopPatterns =
                        transitWithStops.patternsByStop.associate { stopPatterns ->
                            val stop = stopPatterns.stop
                            val patternsByChildStop =
                                mutableMapOf<String, MutableSet<RoutePattern>>()
                            for (staticPattern in stopPatterns.patterns) {
                                staticPattern.stopIds.forEach { stopId ->
                                    patternsByChildStop
                                        .getOrPut(stopId, ::mutableSetOf)
                                        .addAll(staticPattern.patterns)
                                }
                            }
                            val routeAtStop =
                                RouteAtStop(
                                    stopPatterns.directions,
                                    patternsByChildStop.mapValues { it.value.toSet() }
                                )
                            Pair(stop, routeAtStop)
                        }
                    Pair(lineOrRoute, stopPatterns)
                }
            return PartialHierarchy(routePatternsInHierarchy)
        }

        private fun lineOrRoute(global: GlobalResponse, routeId: String): LineOrRoute? {
            val route = global.routes[routeId] ?: return null
            return if (NearbyStaticData.groupedLines.contains(route.lineId)) {
                val line = global.lines[route.lineId] ?: return null
                val routes =
                    global.routes.values.filter { it.lineId == line.id && !it.isShuttle }.toSet()
                LineOrRoute.Line(line, routes)
            } else {
                LineOrRoute.Route(route)
            }
        }

        private fun parentStop(global: GlobalResponse, stopId: String): Stop? {
            val stop = global.stops[stopId] ?: return null
            return stop.resolveParent(global.stops)
        }

        private fun <StopData, T> mutablePartialHierarchy():
            MutableMap<LineOrRoute, MutableMap<Stop, RouteAtStop<StopData, T>>> = mutableMapOf()
    }
}

fun <SD, T> NearbyHierarchy.PartialHierarchy<SD, T>?.orEmpty():
    NearbyHierarchy.PartialHierarchy<SD, T> = this ?: NearbyHierarchy.PartialHierarchy(emptyMap())

fun NearbyHierarchy.PartialHierarchy<
    Pair<List<Direction>?, Unit?>, Pair<Map<String, Set<RoutePattern>>?, List<UpcomingTrip>?>
>
    .withLabels(global: GlobalResponse): NearbyHierarchy {
    return this.mapInContext { lineOrRoute, stop, _, (routePatternsByChildStop, upcomingTrips) ->
            val groupedDirectionForPattern: Map<String, Direction?> =
                when (lineOrRoute) {
                    is LineOrRoute.Line -> {
                        val allPatterns = routePatternsByChildStop.orEmpty().flatMap { it.value }

                        val patternsByDirection =
                            allPatterns
                                .groupBy { pattern ->
                                    val route =
                                        global.routes[pattern.routeId] ?: return@groupBy null
                                    Direction.getDirectionForPattern(global, stop, route, pattern)
                                }
                                .filter { it.key != null && it.value.size > 1 }

                        patternsByDirection.entries
                            .flatMap { (direction, patterns) ->
                                patterns.map { pattern -> pattern.id to direction }
                            }
                            .toMap()
                    }
                    is LineOrRoute.Route -> emptyMap()
                }

            val result = mutableMapOf<DirectionOrHeadsign, NearbyHierarchy.MutableNearbyLeaf>()

            fun leaf(direction: Direction) =
                result.getOrPut(DirectionOrHeadsign.Direction(direction)) {
                    NearbyHierarchy.MutableNearbyLeaf()
                }
            fun leaf(headsign: String, route: Route, line: Line?) =
                result.getOrPut(DirectionOrHeadsign.Headsign(headsign, route, line)) {
                    NearbyHierarchy.MutableNearbyLeaf()
                }

            for ((childStopId, allRoutePatterns) in routePatternsByChildStop.orEmpty()) {
                allRoutePatterns
                    .groupBy { groupedDirectionForPattern[it.id] }
                    .forEach { (direction, routePatterns) ->
                        if (direction != null) {
                            val leaf = leaf(direction)
                            leaf.childStopIds.add(childStopId)
                            leaf.routePatterns.addAll(routePatterns)
                        } else {
                            for (routePattern in routePatterns) {
                                val headsign =
                                    global.trips[routePattern.representativeTripId]?.headsign
                                        ?: continue
                                val route = global.routes[routePattern.routeId] ?: continue
                                val line = (lineOrRoute as? LineOrRoute.Line)?.line
                                val leaf = leaf(headsign, route, line)
                                leaf.childStopIds.add(childStopId)
                                leaf.routePatterns.add(routePattern)
                            }
                        }
                    }
            }

            for (upcomingTrip in upcomingTrips.orEmpty()) {
                val direction = groupedDirectionForPattern[upcomingTrip.trip.routePatternId]
                val leaf =
                    if (direction != null) {
                        leaf(direction)
                    } else {
                        val route = global.routes[upcomingTrip.trip.routeId] ?: continue
                        val line = (lineOrRoute as? LineOrRoute.Line)?.line
                        leaf(upcomingTrip.trip.headsign, route, line)
                    }
                val stopId = upcomingTrip.stopId
                if (stopId != null) {
                    leaf.childStopIds.add(upcomingTrip.stopId)
                }
                // allow null here since an added trip may actually have a null route pattern
                val routePattern = global.routePatterns[upcomingTrip.trip.routePatternId]
                leaf.routePatterns.add(routePattern)
                leaf.upcomingTrips.add(upcomingTrip)
            }

            result.mapValues { it.value.finished() }
        }
        .mapStopDataInContext({ lineOrRoute, stop, (stopDirections, _), dataWithinStop ->
            stopDirections
                ?: when (lineOrRoute) {
                    is LineOrRoute.Line -> {
                        listOf(0, 1).map { directionId ->
                            val typicalHere =
                                dataWithinStop.filterValues { leaf ->
                                    leaf.routePatterns.any { pattern ->
                                        pattern?.typicality == RoutePattern.Typicality.Typical &&
                                            pattern.directionId == directionId
                                    }
                                }
                            // Directions in this fallback will not check overrides, but this only
                            // happens if an entire route-at-stop is only present in the realtime
                            // data, and StaticPatterns.ForLine.groupedDirection also doesn't check
                            // overrides, so it's probably fine
                            if (typicalHere.isEmpty()) {
                                Direction(directionId, lineOrRoute.routes.first())
                            } else if (typicalHere.size > 1) {
                                Direction(
                                    lineOrRoute.routes.first().directionNames[directionId] ?: "",
                                    null,
                                    directionId
                                )
                            } else {
                                val typicalEntry = typicalHere.entries.single()
                                when (val key = typicalEntry.key) {
                                    is DirectionOrHeadsign.Direction -> key.direction
                                    is DirectionOrHeadsign.Headsign ->
                                        Direction(
                                            key.route.directionNames[directionId] ?: "",
                                            key.headsign,
                                            directionId
                                        )
                                }
                            }
                        }
                    }
                    is LineOrRoute.Route ->
                        listOf(Direction(0, lineOrRoute.route), Direction(1, lineOrRoute.route))
                }
        })
        .let { NearbyHierarchy(it.data) }
}

@OptIn(ExperimentalTurfApi::class)
fun MiddleLayer.pickOnlyNearest(sortByDistanceFrom: Position?): MiddleLayer {
    if (sortByDistanceFrom == null) {
        // in nearby transit, we always pass sortByDistanceFrom, so this means we're in unfiltered
        // stop details, where there's only one stop anyway
        return this
    }
    val result =
        mutableMapOf<
            Stop, RouteAtStop<List<Direction>, MutableMap<DirectionOrHeadsign, NearbyLeaf>>
        >()
    val routePatternIdsShown = mutableSetOf<String?>()

    for ((stop, routeAtStop) in
        this.entries.sortedBy { distance(it.key.position, sortByDistanceFrom) }) {
        val newRoutePatternIds =
            routeAtStop.data.values
                .flatMap { it.routePatterns.map { it?.id } }
                .distinct()
                .filterNot { routePatternIdsShown.contains(it) }
        for ((directionOrHeadsign, leaf) in routeAtStop.data) {
            val newRoutePatterns = leaf.routePatterns.filter { newRoutePatternIds.contains(it?.id) }
            val newUpcomingTrips =
                leaf.upcomingTrips.filter { newRoutePatternIds.contains(it.trip.routePatternId) }
            result
                .getOrPut(stop) { RouteAtStop(routeAtStop.stopData, mutableMapOf()) }
                .data[directionOrHeadsign] =
                leaf.copy(
                    routePatterns = newRoutePatterns.toSet(),
                    upcomingTrips = newUpcomingTrips
                )
            routePatternIdsShown.addAll(newRoutePatternIds)
        }
    }

    return result.mapValues { it.value.map { it } }
}
