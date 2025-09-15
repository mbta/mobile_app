package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

public data class AlertSummary(
    internal val effect: Alert.Effect,
    val location: Location? = null,
    val timeframe: Timeframe? = null,
) {
    public sealed class Location {
        public data class DirectionToStop(val direction: Direction, val endStopName: String) :
            Location()

        public data class SingleStop(val stopName: String) : Location()

        public data class StopToDirection(val startStopName: String, val direction: Direction) :
            Location()

        public data class SuccessiveStops(val startStopName: String, val endStopName: String) :
            Location()
    }

    public sealed class Timeframe {
        public data object EndOfService : Timeframe()

        public data object Tomorrow : Timeframe()

        public data class LaterDate(val time: EasternTimeInstant) : Timeframe()

        public data class ThisWeek(val time: EasternTimeInstant) : Timeframe()

        public data class Time(val time: EasternTimeInstant) : Timeframe()
    }

    internal companion object {
        suspend fun summarizing(
            alert: Alert,
            stopId: String,
            directionId: Int,
            patterns: List<RoutePattern>,
            atTime: EasternTimeInstant,
            global: GlobalResponse,
        ): AlertSummary? {
            return withContext(Dispatchers.Default) {
                if (alert.significance < AlertSignificance.Secondary) return@withContext null

                val location = alertLocation(alert, stopId, directionId, patterns, global)
                val timeframe = alertTimeframe(alert, atTime)

                if (location == null && timeframe == null) return@withContext null
                return@withContext AlertSummary(alert.effect, location, timeframe)
            }
        }

        private fun alertTimeframe(alert: Alert, atTime: EasternTimeInstant): Timeframe? {
            val currentPeriod = alert.currentPeriod(atTime) ?: return null
            if (currentPeriod.endingLaterToday) return null
            val endTime = currentPeriod.end ?: return null
            val endDate = currentPeriod.endServiceDate ?: return null

            val serviceDate = atTime.serviceDate
            if (serviceDate == endDate && currentPeriod.toEndOfService) {
                return Timeframe.EndOfService
            } else if (serviceDate == endDate) {
                return Timeframe.Time(endTime)
            } else if (serviceDate.plus(DatePeriod(days = 1)) == endDate) {
                return Timeframe.Tomorrow
            } else if (laterThisWeek(serviceDate, endDate)) {
                return Timeframe.ThisWeek(endTime)
            }

            return Timeframe.LaterDate(endTime)
        }

        private fun laterThisWeek(onDate: LocalDate, endDate: LocalDate): Boolean =
            onDate.dayOfWeek.isoDayNumber < endDate.dayOfWeek.isoDayNumber &&
                endDate.minus(onDate).days < 7

        private fun alertLocation(
            alert: Alert,
            stopId: String,
            directionId: Int,
            patterns: List<RoutePattern>,
            global: GlobalResponse,
        ): Location? {
            val routes = patterns.mapNotNull { global.routes[it.routeId] }.distinct()
            val affectedStops = global.getAlertAffectedStops(alert, routes) ?: return null

            if (affectedStops.size == 1) {
                return Location.SingleStop(affectedStops.first().name)
            }

            // Never show multiple stops for bus
            if (routes.any { !it.isShuttle && it.type == RouteType.BUS }) {
                return null
            }

            // Map each pattern to its list of stops affected by this alert
            val affectedPatternStops =
                mapPatternsToAffectedStops(alert, stopId, directionId, patterns, routes, global)

            // Compare the first stop list to all the others to determine if all patterns share the
            // same disrupted stops, or if multiple branches are disrupted
            val firstStops = affectedPatternStops.values.firstOrNull { it.size > 1 } ?: return null
            val orderedStops = firstStops.mapNotNull { global.stops[it] }

            if (affectedPatternStops.all { it.value.toSet() == firstStops.toSet() }) {
                return Location.SuccessiveStops(orderedStops.first().name, orderedStops.last().name)
            }

            // Determine if every effected stop list starts or ends at the same stop, if they do,
            // the disruption starts on the trunk and ends on multiple branches (or vice versa), if
            // not, return null because we have a more complicated branch to branch disruption.
            fun locationFrom(stop: Stop, first: Boolean = true): Location? {
                val directions =
                    Direction.getDirectionsForLine(global, stop, affectedPatternStops.keys.toList())

                val (stopName, direction) =
                    if (affectedPatternStops.values.all { it.firstOrNull() == stop.id }) {
                        Pair(stop.name, directions[directionId])
                    } else if (affectedPatternStops.values.all { it.lastOrNull() == stop.id }) {
                        Pair(stop.name, directions[1 - directionId])
                    } else return null

                return if (first) {
                    Location.StopToDirection(stopName, direction)
                } else {
                    Location.DirectionToStop(direction, stopName)
                }
            }

            return locationFrom(orderedStops.first()) ?: locationFrom(orderedStops.last(), false)
        }

        private fun mapPatternsToAffectedStops(
            alert: Alert,
            stopId: String,
            directionId: Int,
            patterns: List<RoutePattern>,
            routes: List<Route>,
            global: GlobalResponse,
        ): Map<RoutePattern, List<String>> {
            val patternStops =
                patterns
                    .mapNotNull { pattern ->
                        if (pattern.directionId != directionId) return@mapNotNull null
                        val trip =
                            global.trips[pattern.representativeTripId] ?: return@mapNotNull null
                        Pair(pattern, trip.stopIds)
                    }
                    .plus(
                        // Special casing to properly show when alerts affect multiple GL branches
                        if (routes.any { it.lineId == "line-Green" }) {
                            val directionStops =
                                if (directionId == 0) westboundBranches else eastboundBranches
                            // If the provided stop is on a branch, don't take any parallel
                            // branches into account, we only want to group downstream branches
                            if (
                                directionStops.any { (_, branchStops) ->
                                    branchStops.any {
                                        global.stops[it]?.resolveParent(global)?.id == stopId
                                    }
                                }
                            )
                                emptyList()
                            else
                                directionStops.map { (earlier, branched) ->
                                    Pair(
                                        RoutePattern(
                                            branched.joinToString("-"),
                                            directionId,
                                            "",
                                            0,
                                            RoutePattern.Typicality.Typical,
                                            representativeTripId = "",
                                            routeId = routes[0].id,
                                        ),
                                        earlier + branched,
                                    )
                                }
                        } else emptyList()
                    )
                    .mapNotNull { (pattern, stopIds) ->
                        val stopIdsOnPattern =
                            stopIds
                                ?.filter { stopOnTrip ->
                                    alert.anyInformedEntitySatisfies {
                                        // `affectedStops` only includes parent stops, here we check
                                        // if the child stops on each pattern are affected
                                        checkStop(stopOnTrip)
                                        checkRoute(pattern.routeId)
                                    }
                                }
                                ?.mapNotNull { global.stops[it]?.resolveParent(global)?.id }

                        if (!stopIdsOnPattern.isNullOrEmpty()) pattern to stopIdsOnPattern else null
                    }
                    .toMap()

            // On the D branch, there are patterns that terminate at Reservoir and Riverside, this
            // will remove stop lists that are subsets of some other stop list so that we don't
            // display "Westbound stops" instead of "Riverside" in this case.
            return if (patternStops.size > 1)
                patternStops.filter {
                    !patternStops.any { (otherPattern, otherStops) ->
                        otherPattern != it.key &&
                            otherStops.size > it.value.size &&
                            otherStops.containsAll(it.value)
                    }
                }
            else patternStops
        }

        // The first value in these pairs is the list of trunk stops for each route, including a few
        // minor child stop differences at some stops, like Park and Kenmore. Stops on branches on
        // the opposite end of the line are not included, only trunk stops are included.
        // The second value contains all the child stops that exist only on each branch.
        // These are hard coded because the patterns provided to `summarizing` will only include
        // ones served at the selected stop, they don't take other branches into account, but we
        // always want to show when a disruption is happening on all downstream branches.
        private val westboundBranches =
            listOf(
                Pair( // B Branch
                    listOf(
                        "70502", // Lechmere
                        "70208",
                        "70206",
                        "70204",
                        "70202",
                        "70196",
                        "70159",
                        "70157",
                        "70155",
                        "70153",
                        "71151", // Kenmore
                    ),
                    listOf(
                        "70149", // Blandford Street
                        "70147",
                        "70145",
                        "170141",
                        "170137",
                        "70135",
                        "70131",
                        "70129",
                        "70127",
                        "70125",
                        "70121",
                        "70117",
                        "70115",
                        "70113",
                        "70111",
                        "70107", // Boston College
                    ),
                ),
                Pair( // C Branch
                    listOf(
                        "70502", // Lechmere
                        "70208",
                        "70206",
                        "70204",
                        "70202",
                        "70197",
                        "70159",
                        "70157",
                        "70155",
                        "70153",
                        "70151", // Kenmore
                    ),
                    listOf(
                        "70211", // Saint Mary's Street
                        "70213",
                        "70215",
                        "70217",
                        "70219",
                        "70223",
                        "70225",
                        "70227",
                        "70229",
                        "70231",
                        "70233",
                        "70235",
                        "70237", // Cleveland Circle
                    ),
                ),
                Pair( // D Branch
                    listOf(
                        "70502", // Lechmere
                        "70208",
                        "70206",
                        "70204",
                        "70202",
                        "70198",
                        "70159",
                        "70157",
                        "70155",
                        "70153",
                        "70151", // Kenmore
                    ),
                    listOf(
                        "70187", // Fenway
                        "70183",
                        "70181",
                        "70179",
                        "70177",
                        "70175",
                        "70173",
                        "70171",
                        "70169",
                        "70167",
                        "70165",
                        "70163",
                        "70161", // Riverside
                    ),
                ),
                Pair( // E Branch
                    listOf(
                        "70502", // Lechmere
                        "70208",
                        "70206",
                        "70204",
                        "70202",
                        "70199",
                        "70159",
                        "70157",
                        "70155", // Copley
                    ),
                    listOf(
                        "70239", // Prudential
                        "70241",
                        "70243",
                        "70245",
                        "70247",
                        "70249",
                        "70251",
                        "70253",
                        "70255",
                        "70257",
                        "70260", // Heath Street
                    ),
                ),
            )

        private val eastboundBranches =
            listOf(
                Pair( // Medford/Tufts
                    listOf(
                        "70150", // Kenmore
                        "70152",
                        "70154",
                        "70156",
                        "70158",
                        "70200",
                        "70201",
                        "70203",
                        "70205",
                        "70207",
                        "70501", // Lechmere
                    ),
                    listOf(
                        "70513", // East Somerville
                        "70505",
                        "70507",
                        "70509",
                        "70511", // Medford/Tufts
                    ),
                ),
                Pair( // Union
                    listOf(
                        "70150", // Kenmore
                        "70152",
                        "70154",
                        "70156",
                        "70158",
                        "70200",
                        "70201",
                        "70203",
                        "70205",
                        "70207",
                        "70501", // Lechmere
                    ),
                    listOf("70503"), // Union Square
                ),
            )
    }
}
