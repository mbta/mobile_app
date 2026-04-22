package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.collections.filter
import kotlin.collections.singleOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed class AlertSummary {
    public abstract val effect: Alert.Effect?
    public abstract val recurrence: Recurrence?

    @Serializable
    @SerialName("standard")
    public data class Standard(
        override val effect: Alert.Effect,
        val location: Location? = null,
        val timeframe: Timeframe? = null,
        override val recurrence: Recurrence? = null,
        @SerialName("is_update") val isUpdate: Boolean = false,
    ) : AlertSummary()

    @Serializable
    @SerialName("all_clear")
    public data class AllClear(val location: Location?) : AlertSummary() {
        override val effect: Alert.Effect? = null
        override val recurrence: Recurrence? = null
    }

    @Serializable
    public data class Unknown(val fallback: String) : AlertSummary() {
        override val effect: Alert.Effect? = null
        override val recurrence: Recurrence? = null
    }

    @Serializable
    public sealed class Location {
        @Serializable
        @SerialName("direction_to_stop")
        public data class DirectionToStop(
            val direction: Direction,
            @SerialName("end_stop_name") val endStopName: String,
        ) : Location()

        @Serializable
        @SerialName("single_stop")
        public data class SingleStop(@SerialName("stop_name") val stopName: String) : Location()

        @Serializable
        @SerialName("stop_to_direction")
        public data class StopToDirection(
            @SerialName("start_stop_name") val startStopName: String,
            val direction: Direction,
        ) : Location()

        @Serializable
        @SerialName("successive_stops")
        public data class SuccessiveStops(
            @SerialName("start_stop_name") val startStopName: String,
            @SerialName("end_stop_name") val endStopName: String,
        ) : Location()

        @Serializable
        @SerialName("whole_route")
        public data class WholeRoute(
            @SerialName("route_label") val routeLabel: String,
            @SerialName("route_type") val routeType: RouteType,
        ) : Location()

        @Serializable public data object Unknown : Location()
    }

    @Serializable
    public sealed class Timeframe {
        @Serializable
        @SerialName("until_further_notice")
        public data object UntilFurtherNotice : Timeframe(), Recurrence.EndDay

        @Serializable @SerialName("end_of_service") public data object EndOfService : Timeframe()

        @Serializable
        @SerialName("tomorrow")
        public data object Tomorrow : Timeframe(), Recurrence.EndDay

        @Serializable
        @SerialName("later_date")
        public data class LaterDate(val time: EasternTimeInstant) : Timeframe(), Recurrence.EndDay

        @Serializable
        @SerialName("this_week")
        public data class ThisWeek(val time: EasternTimeInstant) : Timeframe(), Recurrence.EndDay

        @Serializable
        @SerialName("time")
        public data class Time(val time: EasternTimeInstant) : Timeframe()

        @Serializable
        @SerialName("starting_tomorrow")
        public data object StartingTomorrow : Timeframe()

        @Serializable
        @SerialName("starting_later_today")
        public data class StartingLaterToday(val time: EasternTimeInstant) : Timeframe()

        /*
        this should cover “from start of service to 10AM” and “from 10AM to 9PM” and “from 9PM to end of service” and “from start of service to end of service”
         */
        @Serializable
        @SerialName("time_range")
        public data class TimeRange(
            @SerialName("start_time") val startTime: StartTime,
            @SerialName("end_time") val endTime: EndTime,
        ) : Timeframe() {
            public sealed interface Boundary

            @Serializable public sealed interface StartTime : Boundary

            @Serializable public sealed interface EndTime : Boundary

            @Serializable
            @SerialName("start_of_service")
            public data object StartOfService : StartTime

            @Serializable @SerialName("end_of_service") public data object EndOfService : EndTime

            @Serializable
            @SerialName("time")
            public data class Time(val time: EasternTimeInstant) : StartTime, EndTime

            @Serializable public data object Unknown : StartTime, EndTime
        }

        @Serializable public data object Unknown : Timeframe(), Recurrence.EndDay
    }

    @Serializable
    public sealed class Recurrence {
        @Serializable
        @SerialName("daily")
        public data class Daily(val ending: EndDay) : Recurrence()

        @Serializable
        @SerialName("some_days")
        public data class SomeDays(val ending: EndDay) : Recurrence()

        @Serializable public data object Unknown : Recurrence()

        @Serializable public sealed interface EndDay
    }

    public companion object {
        private const val GL_ID = "line-Green"
        private const val GL_LABEL = "Green Line"

        internal suspend fun summarizing(
            alert: Alert,
            stopId: String,
            directionId: Int,
            patterns: List<RoutePattern>,
            atTime: EasternTimeInstant,
            upcomingTrips: List<UpcomingTrip>?,
            global: GlobalResponse,
        ): AlertSummary? {
            return withContext(Dispatchers.Default) {
                if (alert.significance(atTime) < AlertSignificance.Minor) return@withContext null

                val location by lazy { alertLocation(alert, stopId, directionId, patterns, global) }

                if (alert.allClear(atTime)) {
                    return@withContext AllClear(location)
                }

                val recurrence = alertRecurrence(alert, atTime)

                if (alert.anyInformedEntity { it.trip != null }) {
                    TripSpecificAlertSummary.summary(
                            alert,
                            stopId,
                            directionId,
                            patterns,
                            atTime,
                            upcomingTrips,
                            global,
                            recurrence,
                        )
                        ?.let {
                            return@withContext it
                        }
                }

                val timeframe = alertTimeframe(alert, atTime, hasRecurrence = recurrence != null)

                if (location == null && timeframe == null) return@withContext null
                return@withContext Standard(
                    alert.effect,
                    location,
                    timeframe,
                    recurrence,
                    isUpdate = false,
                )
            }
        }

        private fun alertTimeframe(
            alert: Alert,
            atTime: EasternTimeInstant,
            hasRecurrence: Boolean,
        ): Timeframe? {
            val serviceDate = atTime.serviceDate
            val currentPeriod = alert.currentPeriod(atTime)
            if (currentPeriod == null) {
                val nextPeriod = alert.nextPeriod(atTime) ?: return null
                if (nextPeriod.startServiceDate == serviceDate) {
                    return Timeframe.StartingLaterToday(nextPeriod.start)
                }
                return Timeframe.StartingTomorrow
            }
            if (currentPeriod.endingLaterToday) return null
            val endTime = currentPeriod.end ?: return Timeframe.UntilFurtherNotice
            val endDate = currentPeriod.endServiceDate ?: return null

            if (hasRecurrence) {
                val start =
                    if (currentPeriod.fromStartOfService) Timeframe.TimeRange.StartOfService
                    else Timeframe.TimeRange.Time(currentPeriod.start)
                val end =
                    if (currentPeriod.toEndOfService) Timeframe.TimeRange.EndOfService
                    else Timeframe.TimeRange.Time(endTime)
                return Timeframe.TimeRange(start, end)
            }

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

        private fun laterThisWeek(onDate: LocalDate, endDate: LocalDate): Boolean {
            if (onDate.dayOfWeek.isoDayNumber >= endDate.dayOfWeek.isoDayNumber) return false
            val difference = endDate.minus(onDate)
            return difference.years == 0 && difference.months == 0 && difference.days < 7
        }

        internal fun alertLocation(
            alert: Alert,
            stopId: String,
            directionId: Int,
            patterns: List<RoutePattern>,
            global: GlobalResponse,
        ): Location? {
            val routes = patterns.mapNotNull { global.routes[it.routeId] }.distinct()

            val typicalRoutes =
                patterns
                    .mapNotNull { if (it.isTypical()) global.routes[it.routeId] else null }
                    .distinct()
            val isGL = typicalRoutes.isNotEmpty() && typicalRoutes.all { it.id in greenRoutes }

            // If the route is on the GL, check if the alert applies to the entirety of every
            // branch or an entire single branch (not necessarily a provided branch)
            if (isGL) {
                val affectedBranches =
                    greenRoutes.filter {
                        alertAppliesToWholeGLRoute(alert, it, directionId, global)
                    }
                if (affectedBranches.containsAll(greenRoutes)) {
                    return Location.WholeRoute(GL_LABEL, RouteType.LIGHT_RAIL)
                }
                affectedBranches.singleOrNull()?.let {
                    val route = global.getRoute(it) ?: return@let
                    return Location.WholeRoute(route.label, route.type)
                }
            }

            // Check if an informed entity applies to the entire provided route
            routes.singleOrNull()?.let {
                if (matchesWholeRoute(alert, it.id, directionId))
                    return Location.WholeRoute(it.label, it.type)
            }

            val affectedStops = global.getAlertAffectedStops(alert, routes) ?: return null

            if (affectedStops.size == 1) {
                return Location.SingleStop(affectedStops.first().name)
            }

            // Map each pattern to its list of stops affected by this alert
            val affectedPatternStops =
                mapPatternsToAffectedStops(alert, stopId, directionId, patterns, routes, global)

            // If every affected stop on the patterns are specified in the informed entities,
            // return the whole route location
            if (matchesAllStopsOnPatterns(affectedPatternStops, global)) {
                routes.singleOrNull()?.let {
                    return Location.WholeRoute(it.label, it.type)
                }
            }

            // Never show multiple stops for bus
            if (routes.any { !it.isShuttle && it.type == RouteType.BUS }) {
                return null
            }

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
                        if (routes.any { it.lineId == Line.Id(GL_ID) }) {
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
                                        checkRoute(pattern.routeId, routes.firstOrNull()?.type)
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

        private fun matchesWholeRoute(alert: Alert, routeId: Route.Id, directionId: Int): Boolean =
            alert.anyInformedEntity {
                it.appliesTo(directionId = directionId, routeId = routeId) &&
                    it.trip == null &&
                    it.stop == null &&
                    it.facility == null
            }

        private fun matchesAllStopsOnPatterns(
            patternStops: Map<RoutePattern, List<String>>,
            global: GlobalResponse,
        ): Boolean =
            patternStops.isNotEmpty() &&
                patternStops.all { (pattern, affectedStops) ->
                    val trip = global.trips[pattern.representativeTripId] ?: return@all false
                    return@all trip.stopIds?.all {
                        global.stops[it]?.resolveParent(global)?.id in affectedStops
                    } ?: false
                }

        private fun alertAppliesToWholeGLRoute(
            alert: Alert,
            routeId: Route.Id,
            directionId: Int,
            global: GlobalResponse,
        ): Boolean {
            if (!routeId.idText.startsWith("Green-"))
                throw RuntimeException(
                    "alertAppliesToWholeGLRoute should never be called for a non-GL route"
                )

            if (matchesWholeRoute(alert, routeId, directionId)) return true
            val glRoute = global.routes[routeId] ?: return false
            val glPatterns =
                global.routePatterns.values.filter {
                    it.typicality == RoutePattern.Typicality.Typical && it.routeId == routeId
                }
            // The blank stop ID is fine because the ID is only used to check if the stop is on a GL
            // branch, and here we specifically don't care about branching
            val affectedPatternStops =
                mapPatternsToAffectedStops(
                    alert,
                    "",
                    directionId,
                    glPatterns,
                    listOf(glRoute),
                    global,
                )
            return matchesAllStopsOnPatterns(affectedPatternStops, global)
        }

        private fun alertRecurrence(alert: Alert, atTime: EasternTimeInstant): Recurrence? {
            val range = alert.recurrenceRange() ?: return null
            val serviceDate = atTime.serviceDate
            val lastPeriodEnd = range.end
            val lastServiceDate =
                lastPeriodEnd.serviceDate(EasternTimeInstant.ServiceDateRounding.BACKWARDS)
            if (lastServiceDate == serviceDate) {
                return null
            }
            val ending: Recurrence.EndDay =
                when {
                    !range.endDayKnown -> Timeframe.UntilFurtherNotice
                    serviceDate.plus(DatePeriod(days = 1)) == lastServiceDate -> Timeframe.Tomorrow
                    laterThisWeek(serviceDate, lastServiceDate) -> Timeframe.ThisWeek(lastPeriodEnd)
                    else -> Timeframe.LaterDate(lastPeriodEnd)
                }
            return if (range.daily) {
                Recurrence.Daily(ending)
            } else {
                Recurrence.SomeDays(ending)
            }
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
