package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("trip_shuttle")
public data class TripShuttleAlertSummary
@DefaultArgumentInterop.Enabled
constructor(
    @SerialName("trip_time") val tripTime: EasternTimeInstant,
    @SerialName("route_type") val routeType: RouteType,
    @SerialName("current_stop_name") val currentStopName: String,
    @SerialName("end_stop_name") val endStopName: String,
    @SerialName("is_today") val isToday: Boolean = true,
    override val recurrence: Recurrence? = null,
) : AlertSummary() {
    override val effect: Alert.Effect = Alert.Effect.Shuttle

    internal companion object {
        fun summary(
            alert: Alert,
            stopId: String,
            directionId: Int,
            patterns: List<RoutePattern>,
            atTime: EasternTimeInstant,
            informedTrip: UpcomingTrip?,
            global: GlobalResponse,
            recurrence: Recurrence?,
        ): TripShuttleAlertSummary? {
            val tripTime = informedTrip?.time
            val routeType = patterns.firstNotNullOfOrNull { global.getRoute(it.routeId)?.type }
            val currentStopName = global.getStop(stopId)?.name
            val location =
                alertLocation(
                    alert,
                    stopId,
                    directionId,
                    patterns.filter { it.id == informedTrip?.trip?.routePatternId },
                    global,
                )
            return if (
                tripTime != null &&
                    routeType != null &&
                    currentStopName != null &&
                    location is Location.SuccessiveStops
            ) {
                TripShuttleAlertSummary(
                    tripTime,
                    routeType,
                    currentStopName,
                    location.endStopName,
                    isToday = tripTime.serviceDate == atTime.serviceDate,
                    recurrence,
                )
            } else {
                null
            }
        }
    }
}
