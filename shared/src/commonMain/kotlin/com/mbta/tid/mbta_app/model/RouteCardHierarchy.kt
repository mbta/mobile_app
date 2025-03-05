package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration
import kotlinx.datetime.Instant

// type aliases can't be nested :(

typealias ByDirection = Map<Int, RouteCardData.Leaf>

private typealias ByDirectionBuilder = Map<Int, RouteCardData.LeafBuilder>

private typealias ByStopIdBuilder = Map<String, RouteCardData.RouteStopDataBuilder>

private typealias ByLineOrRouteBuilder = Map<String, RouteCardData.Builder>

/**
 * Contain all data for presentation in a route card. A route card is a snapshot of service for a
 * route at a set of stops. It has the general structure: Route (orLine) => Stop(s) => Direction =>
 * Upcoming Trips / reason for absence of upcoming trips
 */
data class RouteCardData(private val lineOrRoute: LineOrRoute, val stopData: List<RouteStopData>) {
    data class RouteStopData(
        val stop: Stop,
        val directions: List<Direction>,
        val data: ByDirection
    )

    data class Leaf(val upcomingTrips: List<UpcomingTrip>?, val alertsHere: List<Alert>?)

    sealed interface LineOrRoute {
        data class Line(
            val line: com.mbta.tid.mbta_app.model.Line,
            val routes: Set<com.mbta.tid.mbta_app.model.Route>
        ) : LineOrRoute

        data class Route(val route: com.mbta.tid.mbta_app.model.Route) : LineOrRoute
    }

    companion object {
        fun routeCardsForStopList(
            stopIds: List<String>,
            globalData: GlobalResponse?,
            sortByDistanceFrom: Position?,
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse?,
            alerts: AlertsStreamDataResponse?,
            filterAtTime: Instant,
            showAllPatternsWhileLoading: Boolean,
            hideNonTypicalPatternsBeyondNext: Duration?,
            filterCancellations: Boolean,
            includeMinorAlerts: Boolean,
            pinnedRoutes: Set<String>
        ): List<RouteCardData>? {

            // if predictions or alerts are still loading, this is the loading state
            if (predictions == null || alerts == null) return null

            // if global data was still loading, there'd be no nearby data, and null handling is
            // annoying
            if (globalData == null) return null

            val cutoffTime = hideNonTypicalPatternsBeyondNext?.let { filterAtTime + it }

            return ListBuilder()
                .addStaticStopsData(stopIds, globalData)
                .addUpcomingTrips(schedules, predictions, filterAtTime)
                .filterIrrelevantData(cutoffTime, showAllPatternsWhileLoading, filterCancellations)
                .addAlerts(alerts, includeMinorAlerts, filterAtTime)
                .build()
                .sort(sortByDistanceFrom, pinnedRoutes)
        }
    }

    class ListBuilder() {
        val data: ByLineOrRouteBuilder = emptyMap()

        fun addStaticStopsData(stopIds: List<String>, globalData: GlobalResponse?): ListBuilder {
            // TODO
            return this
        }

        fun addUpcomingTrips(
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse,
            filterAtTime: Instant
        ): ListBuilder {
            // TODO build upcoming trips
            // transform into map route => stop => direction => upcoming trips
            // merge into data
            return this
        }

        fun addAlerts(
            alerts: AlertsStreamDataResponse?,
            includeMinorAlerts: Boolean,
            filterAtTime: Instant
        ): ListBuilder {
            // TODO
            // transform into map route => stop => direction => alerts
            // break out helper steps for different types of alerts as needed
            // merge into data
            return this
        }

        fun filterIrrelevantData(
            cutoffTime: Instant?,
            showAllPatternsWhileLoading: Boolean,
            filterCancellations: Boolean
        ): ListBuilder {
            // TODO
            return this
        }

        fun build(): List<RouteCardData> {
            return data.map { routeCardBuilder ->
                RouteCardData(
                    routeCardBuilder.value.lineOrRoute,
                    routeCardBuilder.value.stopData.values.map { it.build() }
                )
            }
        }
    }

    data class Builder(val lineOrRoute: LineOrRoute, val stopData: ByStopIdBuilder) {

        fun build(): RouteCardData {
            return RouteCardData(this.lineOrRoute, stopData.values.map { it.build() })
        }

        companion object {
            /**
             * Construct a map of the route/line-ids served by the given stops. Uses the order of
             * the stops in the given list to determine the stop ids that will be included for each
             * route.
             *
             * A stop is only included at a route if it serves any route pattern that is not served
             * by an earlier stop in the list.
             */
            fun fromListOfStops(
                global: GlobalResponse,
                stopIds: List<String>
            ): Map<String, Builder> {
                // TODO - basically what NearbyStaticData constructor does but into the new data
                // types
                return emptyMap()
            }
        }
    }

    data class RouteStopDataBuilder(
        val stop: Stop,
        val directions: List<Direction>,
        val data: ByDirectionBuilder
    ) {
        fun build(): RouteCardData.RouteStopData {
            return RouteCardData.RouteStopData(
                stop,
                directions,
                data.mapValues { it.value.build() }
            )
        }
    }

    data class LeafBuilder(var upcomingTrips: List<UpcomingTrip>?, var alertsHere: List<Alert>?) {

        fun build(): RouteCardData.Leaf {
            // TODO: Once alerts functionality is added, checkNotNull on alerts too
            return RouteCardData.Leaf(checkNotNull(this.upcomingTrips), alertsHere ?: emptyList())
        }
    }
}

fun List<RouteCardData>.sort(
    distanceFrom: Position?,
    pinnedRoutes: Set<String>
): List<RouteCardData> {
    // TODO
    return this
}
