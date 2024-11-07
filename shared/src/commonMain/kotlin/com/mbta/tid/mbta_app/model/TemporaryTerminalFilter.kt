package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse

/**
 * When part of a subway line is closed, should the headsign be the temporary terminal or the
 * permanent terminal? There is no obviously correct answer - temporary terminals directly
 * communicate the boundaries of a disruption, but permanent terminals match in-station wayfinding
 * and are more clearly aligned with directions for riders unfamiliar with the system.
 *
 * The data we receive is inconsistent: depending on numerous factors upstream, schedules may or may
 * not contain temporary terminals, and if they do, predictions may or may not match them. In the
 * medium term, we expect to predictably receive permanent terminals as headsigns, but in the short
 * term, we need some way to avoid confusion in the presence of inconsistent data.
 *
 * Rewriting predictions to have different headsigns and route patterns was a mess when we tried it
 * previously, so we always keep predictions as-is, whether they show permanent terminals or
 * temporary, and if the schedules disagree, we erase the scheduled headsign. Since we don't show
 * scheduled times on subway, the hidden schedule headsign would only ever be shown with
 * "Predictions unavailable", so no actual data will be lost.
 */
class TemporaryTerminalFilter(
    val nearbyStaticData: NearbyStaticData,
    val predictions: PredictionsStreamDataResponse,
    val globalData: GlobalResponse,
    val alerts: List<Alert>,
    val schedules: ScheduleResponse
) {
    private val schedulesByRoute = schedules.schedules.groupBy { it.routeId }
    // predictions that reflect auto-cancelled schedules will throw everything off, so filter out
    // cancellations
    private val predictionsByRoute =
        predictions.predictions.values
            .filterNot { it.scheduleRelationship == Prediction.ScheduleRelationship.Cancelled }
            .groupBy { it.routeId }
    private val routePatternsByRoute = globalData.routePatterns.values.groupBy { it.routeId }

    fun Schedule.routePatternId() = schedules.trips[tripId]?.routePatternId

    fun Prediction.routePatternId() = predictions.trips[tripId]?.routePatternId

    /**
     * Filtering realtime data for temporary terminals causes what the app displays to diverge from
     * what the API says is real, and so must be done with caution. Specifically, we only want to
     * process
     * 1. subway routes
     * 2. with alerts somewhere on the line
     * 3. where the schedule has non-typical patterns and is missing a typical pattern (requiring
     *    only non-typical patterns misses single-branch RL disruptions, but allowing just any
     *    non-typical pattern hits GL early-morning cross-branch trips)
     */
    fun appliesToRoute(route: Route): Boolean {
        val routeId = route.id
        val isSubway = route.type.isSubway()

        val routeHasAlert =
            alerts.any { alert ->
                alert.effect in setOf(Alert.Effect.Suspension, Alert.Effect.Shuttle) &&
                    alert.anyInformedEntity { it.appliesTo(routeId = routeId) }
            }

        val routeSchedules = schedulesByRoute[routeId].orEmpty()
        val scheduledPatterns = routeSchedules.mapNotNullTo(mutableSetOf()) { it.routePatternId() }
        val routePatterns = routePatternsByRoute[routeId].orEmpty()
        val scheduleMissingTypical =
            routePatterns
                .filter { it.typicality == RoutePattern.Typicality.Typical }
                .any { it.id !in scheduledPatterns }
        val scheduleHasNontypical =
            scheduledPatterns.any {
                globalData.routePatterns[it]?.typicality != RoutePattern.Typicality.Typical
            }
        val scheduleReplacedTypical = scheduleMissingTypical && scheduleHasNontypical

        return isSubway && routeHasAlert && scheduleReplacedTypical
    }

    /**
     * Discards scheduled, non-predicted, non-typical headsigns in this
     * [NearbyStaticData.StopPatterns].
     */
    fun filterPatternsAtStop(
        stopPatterns: NearbyStaticData.StopPatterns
    ): NearbyStaticData.StopPatterns {
        return stopPatterns.copy(
            patterns =
                stopPatterns.patterns.filterNot { staticPatterns ->
                    staticPatterns.patterns.all { pattern ->
                        val scheduled =
                            schedulesByRoute[pattern.routeId].orEmpty().any {
                                it.routePatternId() == pattern.id
                            }
                        val predicted =
                            predictionsByRoute[pattern.routeId].orEmpty().any {
                                it.routePatternId() == pattern.id
                            }
                        val typical = pattern.typicality == RoutePattern.Typicality.Typical
                        scheduled && !predicted && !typical
                    }
                }
        )
    }

    /**
     * For each route which passes [appliesToRoute], filters the static data with
     * [filterPatternsAtStop].
     */
    fun filtered(): NearbyStaticData {
        val filteredData =
            nearbyStaticData.data.map { transitWithStops ->
                transitWithStops.allRoutes().fold(transitWithStops) { transit, route ->
                    if (appliesToRoute(route)) {
                        val patternsFiltered =
                            transit.patternsByStop.map { filterPatternsAtStop(it) }
                        transit.copy(patternsByStop = patternsFiltered)
                    } else {
                        transit
                    }
                }
            }

        return nearbyStaticData.copy(data = filteredData)
    }
}
