package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse

class TemporaryTerminalRewriter(
    val nearbyStaticData: NearbyStaticData,
    val predictions: PredictionsStreamDataResponse,
    val globalData: GlobalResponse,
    val alerts: List<Alert>,
    val schedules: ScheduleResponse
) {
    val schedulesByRoute = schedules.schedules.groupBy { it.routeId }
    // predictions that reflect auto-cancelled schedules will throw everything off, so filter out
    // cancellations
    val predictionsByRoute =
        predictions.predictions.values
            .filterNot { it.scheduleRelationship == Prediction.ScheduleRelationship.Cancelled }
            .groupBy { it.routeId }
    val routePatternsByRoute = globalData.routePatterns.values.groupBy { it.routeId }

    fun Schedule.routePattern(): RoutePattern? =
        schedules.trips[tripId]?.let { globalData.routePatterns[it.routePatternId] }

    fun Prediction.routePattern(): RoutePattern? =
        predictions.trips[tripId]?.let { globalData.routePatterns[it.routePatternId] }

    /**
     * Rewriting realtime data for temporary terminals causes what the app displays to diverge from
     * what the API says is real, and so must be done with caution. Specifically, we only want to
     * rewrite
     * 1. subway routes
     * 2. with alerts somewhere on the line
     * 3. where the schedule has non-typical patterns and is missing a typical pattern (requiring
     *    only non-typical patterns misses single-branch RL disruptions, but allowing just any
     *    non-typical pattern hits GL early-morning cross-branch trips)
     * 4. but the predictions all have typical patterns instead of non-typical patterns (meaning
     *    this hasn't been fixed upstream yet)
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
        val scheduledPatterns = routeSchedules.mapNotNullTo(mutableSetOf()) { it.routePattern() }
        val routePatterns = routePatternsByRoute[routeId].orEmpty()
        val scheduleMissingTypical =
            routePatterns
                .filter { it.typicality == RoutePattern.Typicality.Typical }
                .any { it !in scheduledPatterns }
        val scheduleHasNontypical =
            scheduledPatterns.any { it.typicality != RoutePattern.Typicality.Typical }
        val scheduleReplacedTypical = scheduleMissingTypical && scheduleHasNontypical

        val routePredictions = predictionsByRoute[routeId].orEmpty()
        val predictionsAlwaysTypical =
            routePredictions.isNotEmpty() &&
                routePredictions.all {
                    it.routePattern()?.typicality == RoutePattern.Typicality.Typical
                }

        return isSubway && routeHasAlert && scheduleReplacedTypical && predictionsAlwaysTypical
    }
}
