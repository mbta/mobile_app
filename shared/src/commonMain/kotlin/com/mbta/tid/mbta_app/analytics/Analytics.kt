package com.mbta.tid.mbta_app.analytics

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.response.PushNotificationPayload
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
import kotlin.time.Clock

public abstract class Analytics {
    protected abstract fun logEvent(name: String, parameters: Map<String, String>)

    protected abstract fun setUserProperty(name: String, value: String)

    internal var lastTrackedScreen: AnalyticsScreen? = null
        private set

    private fun logEvent(name: String, vararg parameters: Pair<String, String>) {
        val paramsMap = mutableMapOf(*parameters)
        logEvent(name, paramsMap)
    }

    public fun favoritesUpdated(
        updatedFavorites: Map<RouteStopDirection, Boolean>,
        context: EditFavoritesContext,
        defaultDirection: Int,
    ) {

        updatedFavorites.entries
            .groupBy { Pair(it.key.route, it.key.stop) }
            .forEach { (_routeStop, routeStopFavorites) ->
                routeStopFavorites.forEach { (rsd, isFavorited) ->
                    logEvent(
                        "updated_favorites",
                        "action" to if (isFavorited) "add" else "remove",
                        "route_id" to rsd.route.idText,
                        "stop_id" to rsd.stop,
                        "direction_id" to "${rsd.direction}",
                        "is_default_direction" to "${rsd.direction == defaultDirection}",
                        "context" to context.name,
                        "updated_both_directions_at_once" to "${routeStopFavorites.size == 2}",
                    )
                }
            }
    }

    public fun notificationsPermissionDenied() {
        logEvent("notifications_permission_denied")
    }

    public fun notificationsPermissionGranted() {
        logEvent("notifications_permission_granted")
    }

    public fun notificationsWindowSet(window: FavoriteSettings.Notifications.Window, index: Int) {
        logEvent(
            "notifications_window_set",
            "index" to index.toString(),
            "start" to window.startTime.toString(),
            "end" to window.endTime.toString(),
            "days" to window.daysOfWeek.sorted().joinToString(),
        )
    }

    public fun notificationReceived(payload: PushNotificationPayload) {
        logEvent(
            "notification_received",
            "route_id" to payload.subscriptions.joinToString { it.route.idText },
            "stop_id" to payload.subscriptions.joinToString { it.stop },
            "direction_id" to payload.subscriptions.joinToString { it.direction.toString() },
            "alert_effect" to payload.summary.effect.toString(),
            "alert_id" to payload.alertId,
            "notification_type" to payload.notificationType.name,
            "latency_seconds" to (Clock.System.now() - payload.sentAt).inWholeSeconds.toString(),
        )
    }

    public fun notificationClicked(
        payload: PushNotificationPayload,
        stillActive: PushNotificationPayload.StillActive,
    ) {
        logEvent(
            "notification_clicked",
            "route_id" to payload.subscriptions.joinToString { it.route.idText },
            "stop_id" to payload.subscriptions.joinToString { it.stop },
            "direction_id" to payload.subscriptions.joinToString { it.direction.toString() },
            "alert_effect" to payload.summary.effect.toString(),
            "alert_id" to payload.alertId,
            "notification_type" to payload.notificationType.name,
            "latency_seconds" to (Clock.System.now() - payload.sentAt).inWholeSeconds.toString(),
            "still_active" to stillActive.name,
        )
    }

    public fun performedSearch(query: String) {
        logEvent("search", "query" to query)
    }

    public fun performedRouteFilter(query: String) {
        logEvent("route_filter", "query" to query)
    }

    public fun recordSession(colorScheme: AnalyticsColorScheme) {
        setUserProperty("color_scheme", colorScheme.recordedValue)
    }

    public fun recordSession(locationAccess: AnalyticsLocationAccess) {
        setUserProperty("location_access", locationAccess.recordedValue)
    }

    public fun recordSession(favoritesCount: Int) {
        setUserProperty("favorites_count", "$favoritesCount")
    }

    @OptIn(ExperimentalObjCName::class)
    @ObjCName(swiftName = "recordSession")
    public fun recordSessionStationAccessibility(stationAccessibility: Boolean) {
        setUserProperty("station_accessibility_on", stationAccessibility.toString())
    }

    @OptIn(ExperimentalObjCName::class)
    @ObjCName(swiftName = "recordSession")
    public fun recordSessionHideMaps(hideMaps: Boolean) {
        setUserProperty("hide_maps_on", hideMaps.toString())
    }

    @OptIn(ExperimentalObjCName::class)
    @ObjCName(swiftName = "recordSession")
    public fun recordSessionVoiceOver(voiceOver: Boolean) {
        setUserProperty("screen_reader_on", voiceOver.toString())
    }

    public fun refetchedNearbyTransit() {
        logEvent("refetched_nearby_transit")
    }

    public fun tappedAffectedStops(routeId: LineOrRoute.Id?, stopId: String, alertId: String) {
        logEvent(
            "tapped_affected_stops",
            "route_id" to (routeId?.idText ?: ""),
            "stop_id" to stopId,
            "alert_id" to alertId,
        )
    }

    @DefaultArgumentInterop.Enabled
    public fun tappedAlertDetails(
        routeId: LineOrRoute.Id?,
        stopId: String,
        alertId: String,
        elevator: Boolean = false,
    ) {
        logEvent(
            "tapped_alert_details",
            "route_id" to (routeId?.idText ?: ""),
            "stop_id" to stopId,
            "alertId" to alertId,
            "elevator" to elevator.toString(),
        )
    }

    public fun tappedAlertDetailsLegacy(routeId: String, stopId: String, alertId: String) {
        logEvent(
            "tapped_alert_details",
            "route_id" to routeId,
            "stop_id" to stopId,
            "alertId" to alertId,
        )
    }

    public fun tappedDeparture(
        routeId: LineOrRoute.Id,
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
                is UpcomingFormat.NoTripsFormat.SubwayEarlyMorning -> "subway early AM"
                UpcomingFormat.NoTripsFormat.NoSchedulesToday -> "no service today"
                UpcomingFormat.NoTripsFormat.PredictionsUnavailable -> "predictions unavailable"
                UpcomingFormat.NoTripsFormat.ServiceEndedToday -> "service ended"
                null -> ""
            }
        logEvent(
            "tapped_departure",
            "route_id" to routeId.idText,
            "stop_id" to stopId,
            "pinned" to pinned.toString(),
            "alert" to alert.toString(),
            "mode" to mode,
            "no_trips" to noTrips,
            "context" to (lastTrackedScreen?.pageName ?: "unknown"),
        )
    }

    public fun tappedDownstreamStop(
        routeId: Route.Id?,
        stopId: String,
        tripId: String,
        connectingRouteId: String?,
    ) {
        logEvent(
            "tapped_downstream_stop",
            "route_id" to (routeId?.idText ?: ""),
            "stop_id" to stopId,
            "trip_id" to tripId,
            "connecting_route_id" to (connectingRouteId ?: ""),
        )
    }

    public fun tappedOnStop(stopId: String) {
        logEvent("tapped_on_stop", "stop_id" to stopId)
    }

    public fun tappedRouteFilter(routeId: LineOrRoute.Id, stopId: String) {
        logEvent("tapped_route_filter", "route_id" to routeId.idText, "stop_id" to stopId)
    }

    public fun tappedRouteFilterLegacy(routeId: String, stopId: String) {
        logEvent("tapped_route_filter", "route_id" to routeId, "stop_id" to stopId)
    }

    public fun tappedTripPlanner(routeId: LineOrRoute.Id?, stopId: String, alertId: String) {
        logEvent(
            "tapped_trip_planner",
            "route_id" to (routeId?.idText ?: ""),
            "stop_id" to stopId,
            "alert_id" to alertId,
        )
    }

    public fun tappedVehicle(routeId: LineOrRoute.Id) {
        logEvent("tapped_vehicle", "route_id" to routeId.idText)
    }

    public fun toggledPinnedRoute(pinned: Boolean, routeId: String) {
        logEvent(if (pinned) "pin_route" else "unpin_route", "route_id" to routeId)
    }

    public fun track(screen: AnalyticsScreen) {
        lastTrackedScreen = screen
        logEvent(ANALYTICS_EVENT_SCREEN_VIEW, ANALYTICS_PARAMETER_SCREEN_NAME to screen.pageName)
    }

    internal companion object {
        const val ANALYTICS_EVENT_SCREEN_VIEW = "screen_view"
        const val ANALYTICS_PARAMETER_SCREEN_NAME = "screen_name"
    }
}
