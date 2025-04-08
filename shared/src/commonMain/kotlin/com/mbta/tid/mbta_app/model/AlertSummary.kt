package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.serviceDate
import com.mbta.tid.mbta_app.utils.toBostonTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class AlertSummary(
    val effect: Alert.Effect,
    val location: Location? = null,
    val timeframe: Timeframe? = null
) {
    sealed class Location {
        data class BranchToStop(val direction: Direction, val endStopName: String) : Location()

        data class SingleStop(val stopName: String) : Location()

        data class StopToBranch(val startStopName: String, val direction: Direction) : Location()

        data class SuccessiveStops(val startStopName: String, val endStopName: String) : Location()
    }

    sealed class Timeframe {
        data object EndOfService : Timeframe()

        data object Tomorrow : Timeframe()

        data class LaterDate(val time: Instant) : Timeframe()

        data class ThisWeek(val time: Instant) : Timeframe()

        data class Time(val time: Instant) : Timeframe()
    }

    companion object {
        fun summarizing(
            alert: Alert,
            directionId: Int,
            patterns: List<RoutePattern>,
            atTime: Instant,
            global: GlobalResponse
        ): AlertSummary? {
            if (alert.significance < AlertSignificance.Secondary) return null

            val location = alertLocation(alert, directionId, patterns, global)
            val timeframe = alertTimeframe(alert, atTime)

            if (location == null && timeframe == null) return null
            return AlertSummary(alert.effect, location, timeframe)
        }

        private fun alertTimeframe(alert: Alert, atTime: Instant): Timeframe? {
            val currentPeriod = alert.currentPeriod(atTime) ?: return null
            if (currentPeriod.endingLaterToday) return null
            val endTime = currentPeriod.end ?: return null
            val endDate = currentPeriod.endServiceDate ?: return null

            val serviceDate = atTime.toBostonTime().serviceDate
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
            directionId: Int,
            patterns: List<RoutePattern>,
            global: GlobalResponse
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
                mapPatternsToAffectedStops(alert, directionId, patterns, global)

            // Compare the first stop list to all the others to determine if all patterns share the
            // same disrupted stops, or if multiple branches are disrupted
            val firstStops = affectedPatternStops.values.firstOrNull { it.size > 1 } ?: return null
            val orderedStops = firstStops.mapNotNull { global.stops[it] }

            if (affectedPatternStops.all { it.value.toSet() == firstStops.toSet() }) {
                return Location.SuccessiveStops(orderedStops.first().name, orderedStops.last().name)
            }

            return null
        }

        private fun mapPatternsToAffectedStops(
            alert: Alert,
            directionId: Int,
            patterns: List<RoutePattern>,
            global: GlobalResponse
        ): Map<RoutePattern, List<String>> {
            return patterns
                .mapNotNull { pattern ->
                    if (pattern.directionId != directionId) return@mapNotNull null
                    val trip = global.trips[pattern.representativeTripId] ?: return@mapNotNull null
                    Pair(pattern, trip.stopIds)
                }
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

                    if (stopIdsOnPattern != null) pattern to stopIdsOnPattern else null
                }
                .filter { (_, affectedStopsOnPattern) -> affectedStopsOnPattern.isNotEmpty() }
                .toMap()
        }
    }
}
