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
    private val schedulesByRoute = schedules.schedules.groupBy { it.routeId }
    // predictions that reflect auto-cancelled schedules will throw everything off, so filter out
    // cancellations
    val predictionsByRoute =
        predictions.predictions.values
            .filterNot { it.scheduleRelationship == Prediction.ScheduleRelationship.Cancelled }
            .groupBy { it.routeId }
    private val routePatternsByRoute = globalData.routePatterns.values.groupBy { it.routeId }

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

    /**
     * Checks if this route pattern is not typical, points in the same direction as [fullPattern],
     * contains [stopId], and contains a subsequence of [fullPatternStopIds].
     */
    fun RoutePattern.isTruncationOf(
        fullPattern: RoutePattern,
        fullPatternStopIds: List<String>,
        stopId: String
    ): Boolean {
        if (typicality == RoutePattern.Typicality.Typical) return false
        if (directionId != fullPattern.directionId) return false
        val truncatedPatternStopIds =
            globalData.trips[representativeTripId]?.stopIds ?: return false
        return truncatedPatternStopIds.contains(stopId) &&
            fullPatternStopIds.containsSubsequence(truncatedPatternStopIds)
    }

    /**
     * Finds a truncated pattern for [fullPattern] that includes [stopId]. Since at time of writing
     * we only fetch scheduled trips for the remainder of the day, does not require that the
     * truncated pattern actually appear in the schedule unless there are multiple plausible
     * patterns (which could happen if the rating includes both a current disruption and a future
     * disruption on the same line). If there is no plausible truncated pattern, or if there are
     * several, does not truncate [fullPattern].
     */
    fun truncatedPattern(
        fullPattern: RoutePattern,
        fullPatternStopIds: List<String>,
        stopId: String
    ): RoutePattern? {
        // we may be only fetching a subset of the schedule, so we want to consider all the patterns
        // that could be in the schedule
        val plausibleTruncatedPatterns =
            routePatternsByRoute[fullPattern.routeId].orEmpty().filter {
                it.isTruncationOf(fullPattern, fullPatternStopIds, stopId)
            }

        return if (plausibleTruncatedPatterns.size < 2) {
            plausibleTruncatedPatterns.singleOrNull()
        } else {
            // if there are multiple truncated patterns with the right direction and stops,
            // hopefully there's only one that's actually on the schedule
            plausibleTruncatedPatterns.singleOrNull { truncatedPattern ->
                schedulesByRoute[fullPattern.routeId].orEmpty().any {
                    it.routePattern() == truncatedPattern
                }
            }
        }
    }
}

/**
 * Tests whether this list contains the other list in order as a subsequence. Assumes this list does
 * not contain duplicates, because in the specific context where it's used it won't.
 */
private fun <T> List<T>.containsSubsequence(subsequence: List<T>): Boolean {
    if (subsequence.isEmpty()) return true
    val startIndex = this.indexOf(subsequence.first())
    if (startIndex == -1) return false
    if (this.size < startIndex + subsequence.size) return false
    return this.subList(startIndex, startIndex + subsequence.size) == subsequence
}
