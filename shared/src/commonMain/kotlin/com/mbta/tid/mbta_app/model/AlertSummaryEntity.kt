package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class AlertSummaryEntity(
    @SerialName("route_id") val routeId: Route.Id?,
    @SerialName("stop_id") val stopId: String?,
    @SerialName("trip_id") val tripId: String?,
    @SerialName("direction_id") val directionId: Int?,
    val summary: String?,
) {
    internal companion object {
        fun matching(
            summaries: List<AlertSummaryEntity>,
            routeId: Matcher<Route.Id>,
            stopId: Matcher<String>,
            directionId: Matcher<Int>,
            tripId: Matcher<String>,
        ): AlertSummaryEntity? {
            fun <T : Any> matches(expected: Matcher<T>, actual: T?): Boolean =
                actual == null || expected.matches(actual)

            return summaries.find {
                matches(routeId, it.routeId) &&
                    matches(stopId, it.stopId) &&
                    matches(directionId, it.directionId) &&
                    matches(tripId, it.tripId)
            }
        }
    }
}
