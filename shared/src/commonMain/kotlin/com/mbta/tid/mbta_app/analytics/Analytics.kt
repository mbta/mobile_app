package com.mbta.tid.mbta_app.analytics

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.UpcomingFormat
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

abstract class Analytics {
    protected abstract fun logEvent(name: String, parameters: Map<String, String>)

    protected abstract fun setUserProperty(name: String, value: String)

    private fun logEvent(name: String, vararg parameters: Pair<String, String>) {
        val paramsMap = mutableMapOf(*parameters)
        logEvent(name, paramsMap)
    }

    fun performedSearch(query: String) {
        logEvent("search", "query" to query)
    }

    fun performedRouteFilter(query: String) {
        logEvent("route_filter", "query" to query)
    }

    fun recordSession(colorScheme: AnalyticsColorScheme) {
        setUserProperty("color_scheme", colorScheme.recordedValue)
    }

    fun recordSession(locationAccess: AnalyticsLocationAccess) {
        setUserProperty("location_access", locationAccess.recordedValue)
    }

    @OptIn(ExperimentalObjCName::class)
    @ObjCName(swiftName = "recordSession")
    fun recordSessionHideMaps(hideMaps: Boolean) {
        setUserProperty("hide_maps_on", hideMaps.toString())
    }

    @OptIn(ExperimentalObjCName::class)
    @ObjCName(swiftName = "recordSession")
    fun recordSessionVoiceOver(voiceOver: Boolean) {
        setUserProperty("screen_reader_on", voiceOver.toString())
    }

    fun refetchedNearbyTransit() {
        logEvent("refetched_nearby_transit")
    }

    fun tappedAffectedStops(routeId: String, stopId: String, alertId: String) {
        logEvent(
            "tapped_affected_stops",
            "route_id" to routeId,
            "stop_id" to stopId,
            "alert_id" to alertId,
        )
    }

    @DefaultArgumentInterop.Enabled
    fun tappedAlertDetails(
        routeId: String,
        stopId: String,
        alertId: String,
        elevator: Boolean = false,
    ) {
        logEvent(
            "tapped_alert_details",
            "route_id" to routeId,
            "stop_id" to stopId,
            "alertId" to alertId,
            "elevator" to elevator.toString(),
        )
    }

    fun tappedAlertDetailsLegacy(routeId: String, stopId: String, alertId: String) {
        logEvent(
            "tapped_alert_details",
            "route_id" to routeId,
            "stop_id" to stopId,
            "alertId" to alertId,
        )
    }

    fun tappedDeparture(
        routeId: String,
        stopId: String,
        pinned: Boolean,
        alert: Boolean,
        routeType: RouteType,
        noTrips: UpcomingFormat.NoTripsFormat?,
    ) {
        val mode =
            when (routeType) {
                RouteType.BUS -> "bus"
                RouteType.COMMUTER_RAIL -> "commuter rail"
                RouteType.FERRY -> "ferry"
                RouteType.HEAVY_RAIL -> "subway"
                RouteType.LIGHT_RAIL -> "subway"
            }
        val noTrips =
            when (noTrips) {
                UpcomingFormat.NoTripsFormat.NoSchedulesToday -> "no service today"
                UpcomingFormat.NoTripsFormat.PredictionsUnavailable -> "predictions unavailable"
                UpcomingFormat.NoTripsFormat.ServiceEndedToday -> "service ended"
                null -> ""
            }
        logEvent(
            "tapped_departure",
            "route_id" to routeId,
            "stop_id" to stopId,
            "pinned" to pinned.toString(),
            "alert" to alert.toString(),
            "mode" to mode,
            "no_trips" to noTrips,
        )
    }

    fun tappedDownstreamStop(
        routeId: String,
        stopId: String,
        tripId: String,
        connectingRouteId: String?,
    ) {
        logEvent(
            "tapped_downstream_stop",
            "route_id" to routeId,
            "stop_id" to stopId,
            "trip_id" to tripId,
            "connecting_route_id" to (connectingRouteId ?: ""),
        )
    }

    fun tappedOnStop(stopId: String) {
        logEvent("tapped_on_stop", "stop_id" to stopId)
    }

    fun tappedRouteFilter(routeId: String, stopId: String) {
        logEvent("tapped_route_filter", "route_id" to routeId, "stop_id" to stopId)
    }

    fun tappedRouteFilterLegacy(routeId: String, stopId: String) {
        logEvent("tapped_route_filter", "route_id" to routeId, "stop_id" to stopId)
    }

    fun tappedTripPlanner(routeId: String, stopId: String, alertId: String) {
        logEvent(
            "tapped_trip_planner",
            "route_id" to routeId,
            "stop_id" to stopId,
            "alert_id" to alertId,
        )
    }

    fun tappedVehicle(routeId: String) {
        logEvent("tapped_vehicle", "route_id" to routeId)
    }

    fun toggledPinnedRoute(pinned: Boolean, routeId: String) {
        logEvent(if (pinned) "pin_route" else "unpin_route", "route_id" to routeId)
    }

    fun track(screen: AnalyticsScreen) {
        logEvent(ANALYTICS_EVENT_SCREEN_VIEW, ANALYTICS_PARAMETER_SCREEN_NAME to screen.pageName)
    }

    companion object {
        const val ANALYTICS_EVENT_SCREEN_VIEW = "screen_view"
        const val ANALYTICS_PARAMETER_SCREEN_NAME = "screen_name"
    }
}
