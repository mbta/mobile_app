package com.mbta.tid.mbta_app.model

import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi

sealed class StopsAssociated() {
    abstract val id: String
    abstract val patternsByStop: List<PatternsByStop>

    val hasSchedulesToday
        get() = run {
            this.patternsByStop.any { byStop ->
                byStop.patterns.any { patterns ->
                    patterns.hasSchedulesToday ||
                        patterns.hasMajorAlerts ||
                        patterns.upcomingTrips?.isNotEmpty() == true
                }
            }
        }

    fun distanceFrom(position: Position): Double = this.distance(position)

    fun isEmpty(): Boolean = this.patternsByStop.isEmpty()

    fun sortRoute(): Route =
        when (this) {
            is WithRoute -> this.route
            is WithLine -> this.routes.min()
        }

    /**
     * @property patternsByStop A list of route patterns grouped by the station or stop that they
     *   serve.
     */
    data class WithRoute(val route: Route, override val patternsByStop: List<PatternsByStop>) :
        StopsAssociated() {
        override val id: String = route.id
    }

    data class WithLine(
        val line: Line,
        val routes: List<Route>,
        override val patternsByStop: List<PatternsByStop>,
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
    }

    @OptIn(ExperimentalTurfApi::class)
    fun distance(position: Position): Double =
        io.github.dellisd.spatialk.turf.distance(position, patternsByStop.first().position)
}
