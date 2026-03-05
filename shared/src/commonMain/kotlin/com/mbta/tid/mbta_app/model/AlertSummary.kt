package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.collections.get
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
public data class AlertSummary(val pieces: List<Piece>) {
    @Serializable public sealed interface Piece

    internal sealed interface Timeframe

    @Serializable @SerialName("update") public data object Update : Piece

    @Serializable
    @SerialName("all_clear_regular_service")
    public data object AllClearRegularService : Piece

    @Serializable
    @SerialName("effect_noun")
    public data class EffectNoun(val effect: Alert.Effect) : Piece

    @Serializable
    @SerialName("trip_time_from_stop")
    public data class TripTimeFromStop(
        val time: EasternTimeInstant,
        @SerialName("stop_name") val stopName: String,
    ) : Piece

    @Serializable
    @SerialName("trip_time_to_headsign")
    public data class TripTimeToHeadsign(val time: EasternTimeInstant, val headsign: String) : Piece

    @Serializable @SerialName("multiple_trips") public data object MultipleTrips : Piece

    @Serializable
    @SerialName("is_affected_today")
    public data class IsAffectedToday(val effect: Alert.Effect) : Piece

    @Serializable
    @SerialName("are_affected_today")
    public data class AreAffectedToday(val effect: Alert.Effect) : Piece

    @Serializable
    @SerialName("shuttle_buses_replace")
    public data class ShuttleBusesReplace(
        val time: EasternTimeInstant,
        @SerialName("route_type") val routeType: RouteType,
    ) : Piece

    @Serializable
    @SerialName("at_stop")
    public data class AtStop(@SerialName("stop_name") val stopName: String) : Piece

    @Serializable
    @SerialName("from_stop")
    public data class FromStop(@SerialName("stop_name") val stopName: String) : Piece

    @Serializable
    @SerialName("from_direction")
    public data class FromDirection(val direction: Direction) : Piece

    @Serializable
    @SerialName("to_stop")
    public data class ToStop(@SerialName("stop_name") val stopName: String) : Piece

    @Serializable
    @SerialName("to_direction")
    public data class ToDirection(val direction: Direction) : Piece

    @Serializable
    @SerialName("whole_route")
    public data class WholeRoute(
        @SerialName("route_label") val routeLabel: String,
        @SerialName("route_type") val routeType: RouteType,
    ) : Piece

    /** Used for both active period and recurrence */
    @Serializable
    @SerialName("until_further_notice")
    public data object UntilFurtherNotice : Piece, Timeframe

    @Serializable
    @SerialName("through_end_of_service")
    public data object ThroughEndOfService : Piece, Timeframe

    /** Used for active period, not recurrence */
    @Serializable
    @SerialName("through_tomorrow")
    public data object ThroughTomorrow : Piece, Timeframe

    /** Used for active period, not recurrence */
    @Serializable
    @SerialName("through_later_date")
    public data class ThroughLaterDate(val time: EasternTimeInstant) : Piece, Timeframe

    /** Used for active period, not recurrence */
    @Serializable
    @SerialName("through_later_this_week")
    public data class ThroughLaterThisWeek(val time: EasternTimeInstant) : Piece, Timeframe

    @Serializable
    @SerialName("through_later_today")
    public data class ThroughLaterToday(val time: EasternTimeInstant) : Piece, Timeframe

    @Serializable
    @SerialName("starting_tomorrow")
    public data object StartingTomorrow : Piece, Timeframe

    @Serializable
    @SerialName("starting_later_today")
    public data class StartingLaterToday(val time: EasternTimeInstant) : Piece, Timeframe

    @Serializable
    @SerialName("time_range")
    public data class TimeRange(
        @SerialName("start_time") val startTime: StartTime,
        @SerialName("end_time") val endTime: EndTime,
    ) : Piece, Timeframe {
        public sealed interface Boundary

        @Serializable public sealed interface StartTime : Boundary

        @Serializable public sealed interface EndTime : Boundary

        @Serializable @SerialName("start_of_service") public data object StartOfService : StartTime

        @Serializable @SerialName("end_of_service") public data object EndOfService : EndTime

        @Serializable
        @SerialName("time")
        public data class Time(val time: EasternTimeInstant) : StartTime, EndTime

        @Serializable public data object Unknown : StartTime, EndTime
    }

    @Serializable
    @SerialName("due_to_cause")
    public data class DueToCause(val cause: Alert.Cause) : Piece

    @Serializable @SerialName("daily") public data object Daily : Piece

    @Serializable @SerialName("some_days") public data object SomeDays : Piece

    /** Used for recurrence, not active period */
    @Serializable @SerialName("until_tomorrow") public data object UntilTomorrow : Piece

    /** Used for recurrence, not active period */
    @Serializable
    @SerialName("until_later_this_week")
    public data class UntilLaterThisWeek(val time: EasternTimeInstant) : Piece

    /** Used for recurrence, not active period */
    @Serializable
    @SerialName("until_later_date")
    public data class UntilLaterDate(val time: EasternTimeInstant) : Piece

    @Serializable public data object Unknown : Piece

    public val effect: Alert.Effect? by lazy {
        pieces.firstNotNullOfOrNull { when (it) {
            is EffectNoun -> it.effect
            is IsAffectedToday -> it.effect
            is AreAffectedToday -> it.effect
            else -> null
        } }
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

                val location = alertLocation(alert, stopId, directionId, patterns, global)
                val update = alertUpdated(alert, atTime)
                val recurrence = alertRecurrence(alert, atTime)
                val timeframe =
                    alertTimeframe(
                        alert,
                        atTime,
                        upcomingTrips,
                        hasRecurrence = recurrence.isNotEmpty(),
                    )

                if (location.isEmpty() && timeframe == null) return@withContext null
                return@withContext AlertSummary(
                    listOfNotNull(update, EffectNoun(alert.effect)) +
                        location +
                        listOfNotNull(timeframe) +
                        recurrence
                )
            }
        }

        private fun alertTimeframe(
            alert: Alert,
            atTime: EasternTimeInstant,
            upcomingTrips: List<UpcomingTrip>?,
            hasRecurrence: Boolean,
        ): Piece? {
            val serviceDate = atTime.serviceDate
            val currentPeriod = alert.currentPeriod(atTime)
            if (currentPeriod == null) {
                val nextPeriod = alert.nextPeriod(atTime) ?: return null
                if (nextPeriod.startServiceDate == serviceDate) {
                    return StartingLaterToday(nextPeriod.start)
                }
                return StartingTomorrow
            }
            if (currentPeriod.endingLaterToday) return null
            val endTime = currentPeriod.end ?: return UntilFurtherNotice
            val endDate = currentPeriod.endServiceDate ?: return null

            if (hasRecurrence) {
                val start =
                    if (currentPeriod.fromStartOfService) TimeRange.StartOfService
                    else TimeRange.Time(currentPeriod.start)
                val end =
                    if (currentPeriod.toEndOfService) TimeRange.EndOfService
                    else TimeRange.Time(endTime)
                return TimeRange(start, end)
            }

            if (serviceDate == endDate && currentPeriod.toEndOfService) {
                return ThroughEndOfService
            } else if (serviceDate == endDate) {
                return ThroughLaterToday(endTime)
            } else if (serviceDate.plus(DatePeriod(days = 1)) == endDate) {
                return ThroughTomorrow
            } else if (laterThisWeek(serviceDate, endDate)) {
                return ThroughLaterThisWeek(endTime)
            }

            return ThroughLaterDate(endTime)
        }

        private fun laterThisWeek(onDate: LocalDate, endDate: LocalDate): Boolean {
            if (onDate.dayOfWeek.isoDayNumber >= endDate.dayOfWeek.isoDayNumber) return false
            val difference = endDate.minus(onDate)
            return difference.years == 0 && difference.months == 0 && difference.days < 7
        }

        private fun alertLocation(
            alert: Alert,
            stopId: String,
            directionId: Int,
            patterns: List<RoutePattern>,
            global: GlobalResponse,
        ): List<Piece> {
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
                    return listOf(WholeRoute(GL_LABEL, RouteType.LIGHT_RAIL))
                }
                affectedBranches.singleOrNull()?.let {
                    val route = global.getRoute(it) ?: return@let
                    return listOf(WholeRoute(route.label, route.type))
                }
            }

            // Check if an informed entity applies to the entire provided route
            routes.singleOrNull()?.let {
                if (matchesWholeRoute(alert, it.id, directionId))
                    return listOf(WholeRoute(it.label, it.type))
            }

            val affectedStops = global.getAlertAffectedStops(alert, routes) ?: return emptyList()

            if (affectedStops.size == 1) {
                return listOf(AtStop(affectedStops.first().name))
            }

            // Map each pattern to its list of stops affected by this alert
            val affectedPatternStops =
                mapPatternsToAffectedStops(alert, stopId, directionId, patterns, routes, global)

            // If every affected stop on the patterns are specified in the informed entities,
            // return the whole route location
            if (matchesAllStopsOnPatterns(affectedPatternStops, global)) {
                routes.singleOrNull()?.let {
                    return listOf(WholeRoute(it.label, it.type))
                }
            }

            // Never show multiple stops for bus
            if (routes.any { !it.isShuttle && it.type == RouteType.BUS }) {
                return emptyList()
            }

            // Compare the first stop list to all the others to determine if all patterns share the
            // same disrupted stops, or if multiple branches are disrupted
            val firstStops =
                affectedPatternStops.values.firstOrNull { it.size > 1 } ?: return emptyList()
            val orderedStops = firstStops.mapNotNull { global.stops[it] }

            if (affectedPatternStops.all { it.value.toSet() == firstStops.toSet() }) {
                return listOf(FromStop(orderedStops.first().name), ToStop(orderedStops.last().name))
            }

            // Determine if every effected stop list starts or ends at the same stop, if they do,
            // the disruption starts on the trunk and ends on multiple branches (or vice versa), if
            // not, return null because we have a more complicated branch to branch disruption.
            fun locationFrom(stop: Stop, first: Boolean = true): List<Piece>? {
                val directions =
                    Direction.getDirectionsForLine(global, stop, affectedPatternStops.keys.toList())

                val (stopName, direction) =
                    if (affectedPatternStops.values.all { it.firstOrNull() == stop.id }) {
                        Pair(stop.name, directions[directionId])
                    } else if (affectedPatternStops.values.all { it.lastOrNull() == stop.id }) {
                        Pair(stop.name, directions[1 - directionId])
                    } else return null

                return if (first) {
                    listOf(FromStop(stopName), ToDirection(direction))
                } else {
                    listOf(FromDirection(direction), ToStop(stopName))
                }
            }

            return locationFrom(orderedStops.first())
                ?: locationFrom(orderedStops.last(), false)
                ?: emptyList()
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

        private fun alertRecurrence(alert: Alert, atTime: EasternTimeInstant): List<Piece> {
            val range = alert.recurrenceRange() ?: return emptyList()
            val serviceDate = atTime.serviceDate
            val lastPeriodEnd = range.end
            val lastServiceDate =
                lastPeriodEnd.serviceDate(EasternTimeInstant.ServiceDateRounding.BACKWARDS)
            if (lastServiceDate == serviceDate) {
                return emptyList()
            }
            val ending: Piece =
                when {
                    !range.endDayKnown -> UntilFurtherNotice
                    serviceDate.plus(DatePeriod(days = 1)) == lastServiceDate -> UntilTomorrow
                    laterThisWeek(serviceDate, lastServiceDate) -> UntilLaterThisWeek(lastPeriodEnd)
                    else -> UntilLaterDate(lastPeriodEnd)
                }
            return if (range.daily) {
                listOf(Daily, ending)
            } else {
                listOf(SomeDays, ending)
            }
        }

        private fun alertUpdated(alert: Alert, atTime: EasternTimeInstant): Piece? =
            when {
                alert.allClear(atTime) -> AllClearRegularService
                else -> null
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
