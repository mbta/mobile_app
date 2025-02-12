package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.distance
import kotlinx.datetime.Instant

private typealias ByDirectionOrHeadsign =
    Map<NearbyHierarchy.DirectionOrHeadsign, NearbyHierarchy.NearbyLeaf>

private typealias ByStop = Map<Stop, NearbyHierarchy.ByStopData>

private typealias ByLineOrRoute = Map<NearbyHierarchy.LineOrRoute, ByStop>

private typealias StaticByStopData = Pair<List<Direction>, Map<String, Set<RoutePattern>>>

/**
 * An intermediate data structure containing data to show in nearby transit grouped by line/route,
 * parent stop, and headsign/direction.
 *
 * Built out of [Map]s to make it easier to merge different data sources together.
 *
 * Note that [NearbyLeaf]s do not contain the [LineOrRoute], [Stop], or [DirectionOrHeadsign] that
 * are their context, so pieces of this cannot be displayed directly.
 */
data class NearbyHierarchy(private val data: ByLineOrRoute) {
    sealed interface LineOrRoute {
        data class Line(
            val line: com.mbta.tid.mbta_app.model.Line,
            val routes: Set<com.mbta.tid.mbta_app.model.Route>
        ) : LineOrRoute

        data class Route(val route: com.mbta.tid.mbta_app.model.Route) : LineOrRoute
    }

    /**
     * Attaches the route directions to a [LineOrRoute] at a [Stop] across all
     * [DirectionOrHeadsign]s that will be listed there.
     */
    data class ByStopData(val directions: List<Direction>, val data: ByDirectionOrHeadsign)

    sealed interface DirectionOrHeadsign {
        data class Direction(val direction: com.mbta.tid.mbta_app.model.Direction) :
            DirectionOrHeadsign

        data class Headsign(val headsign: String, val route: Route, val line: Line?) :
            DirectionOrHeadsign
    }

    /**
     * Since we only know whether or not to group data by direction once all the data is available,
     * we need to represent partially-constructed hierarchies that only group by route and stop.
     */
    data class PartialHierarchy<T>(val data: Map<LineOrRoute, Map<Stop, T>>) {
        /** Transforms each [T]. */
        fun <U> map(f: (T) -> U): PartialHierarchy<U> =
            data
                .mapValues { (_, routeData) ->
                    routeData.mapValues { (_, routeAtStop) -> f(routeAtStop) }
                }
                .let(::PartialHierarchy)

        /** Transforms each [T] with a function that receives the full context of that data. */
        fun <U> mapInContext(f: (LineOrRoute, Stop, T) -> U): PartialHierarchy<U> =
            data
                .mapValues { (lineOrRoute, routeData) ->
                    routeData.mapValues { (stop, routeAtStop) -> f(lineOrRoute, stop, routeAtStop) }
                }
                .let(::PartialHierarchy)

        /**
         * Combines this [PartialHierarchy] with another [PartialHierarchy], creating [Pair]s. Data
         * that is absent in one [PartialHierarchy] will be represented by `null` in the [Pair].
         */
        fun <U> zip(other: PartialHierarchy<U>): PartialHierarchy<Pair<T?, U?>> =
            (data.keys + other.data.keys)
                .associateWith { lineOrRoute ->
                    (data[lineOrRoute]?.keys.orEmpty() + other.data[lineOrRoute]?.keys.orEmpty())
                        .associateWith { stop ->
                            val ownData = data[lineOrRoute]?.get(stop)
                            val otherData = other.data[lineOrRoute]?.get(stop)
                            Pair(ownData, otherData)
                        }
                }
                .let(::PartialHierarchy)
    }

    /** Turns each [Map.Entry] into a list item. */
    fun <U> mapEntries(f: (Map.Entry<LineOrRoute, ByStop>) -> U): List<U> = data.map(f)

    /** Represents the data on a [LineOrRoute] at a [Stop] for a [DirectionOrHeadsign]. */
    data class NearbyLeaf(
        val childStopIds: Set<String>,
        val routePatterns: Set<RoutePattern?>,
        val upcomingTrips: List<UpcomingTrip>,
    ) {
        /**
         * Return a copy of this [NearbyLeaf] with cancelled trips possibly removed depending on
         * context and whether or not the route is a subway route.
         */
        fun filterCancellations(isSubway: Boolean) =
            this.copy(upcomingTrips = upcomingTrips.filterCancellations(isSubway))

        /**
         * Return a copy of this [NearbyLeaf] with only patterns that pass the given [predicate] and
         * only trips on those patterns.
         */
        fun filterPatterns(predicate: (RoutePattern?) -> Boolean): NearbyLeaf {
            val newPatterns = routePatterns.filter(predicate).toSet()
            val newPatternIds = newPatterns.map { it?.id }
            val newTrips = upcomingTrips.filter { it.trip.routePatternId in newPatternIds }
            return this.copy(routePatterns = newPatterns, upcomingTrips = newTrips)
        }
    }

    /** A [NearbyLeaf] containing mutable collections, useful during construction. */
    data class MutableNearbyLeaf(
        val childStopIds: MutableSet<String>,
        val routePatterns: MutableSet<RoutePattern?>,
        val upcomingTrips: MutableList<UpcomingTrip>,
    ) {
        constructor() : this(mutableSetOf(), mutableSetOf(), mutableListOf())

        /** Convert this [MutableNearbyLeaf] into an immutable [NearbyLeaf]. */
        fun finished() = NearbyLeaf(childStopIds, routePatterns, upcomingTrips)
    }

    companion object {
        /** Groups schedules from the given [ScheduleResponse] into a [PartialHierarchy]. */
        fun fromSchedules(
            global: GlobalResponse,
            schedules: ScheduleResponse,
        ): PartialHierarchy<List<Schedule>> {
            val result = mutablePartialHierarchy<MutableList<Schedule>>()

            for (schedule in schedules.schedules) {
                val parentStop = parentStop(global, schedule.stopId) ?: continue
                val lineOrRoute = lineOrRoute(global, schedule.routeId) ?: continue
                result
                    .getOrPut(lineOrRoute, ::mutableMapOf)
                    .getOrPut(parentStop) { mutableListOf() }
                    .add(schedule)
            }

            return PartialHierarchy(result).map { it }
        }

        /**
         * Groups predictions from the given [PredictionsStreamDataResponse] into a
         * [PartialHierarchy].
         */
        fun fromPredictions(
            global: GlobalResponse,
            predictions: PredictionsStreamDataResponse,
        ): PartialHierarchy<List<Prediction>> {
            val result = mutablePartialHierarchy<MutableList<Prediction>>()

            for (prediction in predictions.predictions.values) {
                val parentStop = parentStop(global, prediction.stopId) ?: continue
                val lineOrRoute = lineOrRoute(global, prediction.routeId) ?: continue
                result
                    .getOrPut(lineOrRoute, ::mutableMapOf)
                    .getOrPut(parentStop) { mutableListOf() }
                    .add(prediction)
            }

            return PartialHierarchy(result).map { it }
        }

        /**
         * Groups the given [schedules] and [predictions] into [PartialHierarchy]s and then combines
         * those lists into [UpcomingTrip] lists.
         */
        fun fromRealtime(
            global: GlobalResponse,
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse,
            filterAtTime: Instant,
        ): PartialHierarchy<List<UpcomingTrip>> =
            schedules
                ?.let { fromSchedules(global, it) }
                .orEmpty()
                .zip(fromPredictions(global, predictions))
                .map { (schedulesHere, predictionsHere) ->
                    UpcomingTrip.tripsFromData(
                        global.stops,
                        schedulesHere.orEmpty(),
                        predictionsHere.orEmpty(),
                        schedules?.trips.orEmpty() + predictions.trips,
                        predictions.vehicles,
                        filterAtTime,
                    )
                }

        /**
         * Converts a [NearbyStaticData] grouping into a [PartialHierarchy].
         *
         * The resulting [Pair.first] is the list of directions on a route at a stop, and the
         * [Pair.second] is a map from child stop IDs to the [RoutePattern]s at that child stop.
         */
        fun fromStaticData(staticData: NearbyStaticData): PartialHierarchy<StaticByStopData> {
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
                                Pair(
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
            val line = global.lines[route.lineId]
            return if (line?.isGrouped == true) {
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

        private fun <T> mutablePartialHierarchy(): MutableMap<LineOrRoute, MutableMap<Stop, T>> =
            mutableMapOf()
    }
}

fun <T> NearbyHierarchy.PartialHierarchy<T>?.orEmpty(): NearbyHierarchy.PartialHierarchy<T> =
    this ?: NearbyHierarchy.PartialHierarchy(emptyMap())

/**
 * Turns a [NearbyHierarchy.PartialHierarchy] created by [NearbyHierarchy.PartialHierarchy.zip]ping
 * [NearbyHierarchy.fromStaticData] with [NearbyHierarchy.fromRealtime] into a full
 * [NearbyHierarchy].
 *
 * Groups line patterns by [Direction] if there are multiple [RoutePattern]s headed in the same
 * [Direction].
 *
 * Preserves the directions from the static data if they exist, and recomputes them if a
 * [NearbyHierarchy.ByStopData] contains only realtime data, which should only happen in exceptional
 * circumstances.
 */
fun NearbyHierarchy.PartialHierarchy<Pair<StaticByStopData?, List<UpcomingTrip>?>>.withLabels(
    global: GlobalResponse
): NearbyHierarchy {
    return this.mapInContext { lineOrRoute, stop, (staticData, upcomingTrips) ->
            val (stopDirections, routePatternsByChildStop) = staticData ?: Pair(null, null)
            val groupedDirectionForPattern: Map<String, Direction?> =
                when (lineOrRoute) {
                    is NearbyHierarchy.LineOrRoute.Line -> {
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
                    is NearbyHierarchy.LineOrRoute.Route -> emptyMap()
                }

            val resultData =
                mutableMapOf<
                    NearbyHierarchy.DirectionOrHeadsign, NearbyHierarchy.MutableNearbyLeaf
                >()

            fun leaf(direction: Direction) =
                resultData.getOrPut(NearbyHierarchy.DirectionOrHeadsign.Direction(direction)) {
                    NearbyHierarchy.MutableNearbyLeaf()
                }
            fun leaf(headsign: String, route: Route, line: Line?) =
                resultData.getOrPut(
                    NearbyHierarchy.DirectionOrHeadsign.Headsign(headsign, route, line)
                ) {
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
                                val line = (lineOrRoute as? NearbyHierarchy.LineOrRoute.Line)?.line
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
                        val line = (lineOrRoute as? NearbyHierarchy.LineOrRoute.Line)?.line
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

            val resultDirections =
                stopDirections
                    ?: when (lineOrRoute) {
                        is NearbyHierarchy.LineOrRoute.Line -> {
                            listOf(0, 1).map { directionId ->
                                val typicalHere =
                                    resultData.filterValues { leaf ->
                                        leaf.routePatterns.any { pattern ->
                                            pattern?.typicality ==
                                                RoutePattern.Typicality.Typical &&
                                                pattern.directionId == directionId
                                        }
                                    }
                                // Directions in this fallback will not check overrides, but this
                                // only
                                // happens if an entire route-at-stop is only present in the
                                // realtime
                                // data, and StaticPatterns.ForLine.groupedDirection also doesn't
                                // check
                                // overrides, so it's probably fine
                                if (typicalHere.isEmpty()) {
                                    Direction(directionId, lineOrRoute.routes.first())
                                } else if (typicalHere.size > 1) {
                                    Direction(
                                        lineOrRoute.routes.first().directionNames[directionId]
                                            ?: "",
                                        null,
                                        directionId
                                    )
                                } else {
                                    val typicalEntry = typicalHere.entries.single()
                                    when (val key = typicalEntry.key) {
                                        is NearbyHierarchy.DirectionOrHeadsign.Direction ->
                                            key.direction
                                        is NearbyHierarchy.DirectionOrHeadsign.Headsign ->
                                            Direction(
                                                key.route.directionNames[directionId] ?: "",
                                                key.headsign,
                                                directionId
                                            )
                                    }
                                }
                            }
                        }
                        is NearbyHierarchy.LineOrRoute.Route ->
                            listOf(Direction(0, lineOrRoute.route), Direction(1, lineOrRoute.route))
                    }
            NearbyHierarchy.ByStopData(
                resultDirections,
                resultData.mapValues { it.value.finished() }
            )
        }
        .let { NearbyHierarchy(it.data) }
}

/**
 * Ensures that each route pattern is only represented at the single stop nearest to
 * [sortByDistanceFrom].
 */
@OptIn(ExperimentalTurfApi::class)
fun ByStop.pickOnlyNearest(sortByDistanceFrom: Position?): ByStop {
    if (sortByDistanceFrom == null) {
        // in nearby transit, we always pass sortByDistanceFrom, so this means we're in unfiltered
        // stop details, where there's only one stop anyway
        return this
    }
    val result =
        mutableMapOf<
            Stop,
            Pair<
                List<Direction>,
                MutableMap<NearbyHierarchy.DirectionOrHeadsign, NearbyHierarchy.NearbyLeaf>
            >
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
                .getOrPut(stop) { Pair(routeAtStop.directions, mutableMapOf()) }
                .second[directionOrHeadsign] =
                leaf.copy(
                    routePatterns = newRoutePatterns.toSet(),
                    upcomingTrips = newUpcomingTrips
                )
            routePatternIdsShown.addAll(newRoutePatternIds)
        }
    }

    return result.mapValues { NearbyHierarchy.ByStopData(it.value.first, it.value.second) }
}
