package com.mbta.tid.mbta_app.routes

import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import io.ktor.http.Url

public sealed class DeepLinkState {
    public data object None : DeepLinkState()

    public data class Alert(val alertId: String, val routeId: String?, val stopId: String?) :
        DeepLinkState()

    public data class Stop(
        val stopId: String,
        val routeId: String?,
        val directionId: Int?,
        val tripId: String?,
    ) : DeepLinkState() {
        val sheetRoute: SheetRoutes.StopDetails =
            SheetRoutes.StopDetails(
                stopId,
                directionId?.let { direction ->
                    routeId?.let { route ->
                        StopDetailsFilter(LineOrRoute.Id.fromString(route), direction, false)
                    }
                },
                tripId?.let { TripDetailsFilter(it, null, null, false) },
            )
    }

    public companion object {
        private val alertKeys = listOf("a", "alert")
        private val directionKeys = listOf("d", "direction")
        private val routeKeys = listOf("r", "route")
        private val stopKeys = listOf("s", "stop")
        private val tripKeys = listOf("t", "trip")

        public fun from(url: String): DeepLinkState? {
            val url = Url(url)
            val firstSegment = url.segments.firstOrNull()
            return when (firstSegment) {
                in alertKeys -> parseAlertSegments(url)
                in stopKeys -> parseStopSegments(url)
                null -> None
                else -> {
                    println("Unhandled deep link URI $url")
                    null
                }
            }
        }

        private fun parseAlertSegments(url: Url): Alert? {
            val alertId = segmentAfter(url, alertKeys)
            val routeId = segmentAfter(url, routeKeys)
            val stopId = segmentAfter(url, stopKeys)
            return alertId?.let { Alert(it, routeId, stopId) }
        }

        private fun parseStopSegments(url: Url): Stop? {
            val stopId = segmentAfter(url, stopKeys)
            val routeId = segmentAfter(url, routeKeys)
            val directionId = segmentAfter(url, directionKeys)?.toIntOrNull()?.coerceIn(0, 1)
            val tripId = segmentAfter(url, tripKeys)
            return stopId?.let { Stop(it, routeId, directionId, tripId) }
        }

        private fun segmentAfter(url: Url, keys: List<String>): String? {
            for (key in keys) {
                val index = url.segments.indexOfFirst { it.lowercase() == key }
                if (index >= 0) {
                    return url.segments.getOrNull(index + 1)
                }
            }
            return null
        }
    }
}
