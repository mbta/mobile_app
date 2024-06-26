package com.mbta.tid.mbta_app.model

import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi

sealed class StopsAssociated() {
    abstract val id: String

    fun distanceFrom(position: Position): Double =
        when (this) {
            is WithRoute -> this.distance(position)
            is WithLine -> this.distance(position)
        }

    fun isEmpty(): Boolean =
        when (this) {
            is WithRoute -> this.patternsByStop.isEmpty()
            is WithLine -> this.patternsByStop.isEmpty()
        }

    fun sortRoute(): Route =
        when (this) {
            is WithRoute -> this.route
            is WithLine -> this.routes.min()
        }

    /**
     * @property patternsByStop A list of route patterns grouped by the station or stop that they
     *   serve.
     */
    data class WithRoute(val route: Route, val patternsByStop: List<PatternsByStop>) :
        StopsAssociated() {
        override val id: String = route.id

        @OptIn(ExperimentalTurfApi::class)
        fun distance(position: Position): Double =
            io.github.dellisd.spatialk.turf.distance(position, patternsByStop.first().position)
    }

    data class WithLine(
        val line: Line,
        val routes: List<Route>,
        val patternsByStop: List<PatternsByStop>,
    ) : StopsAssociated() {
        override val id: String = line.id

        val condensePredictions: Boolean = patternsByStop.size > 1 || singleRoute()

        private fun singleRoute(): Boolean {
            if (patternsByStop.isEmpty()) {
                return false
            }
            val upcoming = patternsByStop.first().allUpcomingTrips()
            if (upcoming.isEmpty()) {
                return false
            }
            val matchRoute = upcoming.first().trip.routeId
            return !upcoming.all { it.trip.routeId == matchRoute }
        }

        @OptIn(ExperimentalTurfApi::class)
        fun distance(position: Position): Double =
            io.github.dellisd.spatialk.turf.distance(position, patternsByStop.first().position)
    }
}
