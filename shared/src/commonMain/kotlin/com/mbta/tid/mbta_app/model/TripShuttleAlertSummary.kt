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
    @SerialName("trip_identity") val tripIdentity: TripIdentity,
    @SerialName("start_stop_name") val startStopName: String,
    @SerialName("end_stop_name") val endStopName: String,
    override val recurrence: Recurrence? = null,
) : AlertSummary() {
    override val effect: Alert.Effect = Alert.Effect.Shuttle

    @Serializable public sealed interface TripIdentity

    @Serializable
    @SerialName("single_trip")
    public data class SingleTrip(
        @SerialName("trip_time") val tripTime: EasternTimeInstant,
        @SerialName("route_type") val routeType: RouteType,
        @SerialName("from_stop_name") val fromStopName: String?,
    ) : TripIdentity

    @Serializable
    @SerialName("this_trip")
    public data class ThisTrip(@SerialName("route_type") val routeType: RouteType) : TripIdentity

    @Serializable @SerialName("multiple_trips") public data object MultipleTrips : TripIdentity

    internal companion object {
        fun summary(
            alert: Alert,
            stopId: String,
            directionId: Int,
            patterns: List<RoutePattern>,
            informedTrips: List<UpcomingTrip>,
            global: GlobalResponse,
            recurrence: Recurrence?,
        ): TripShuttleAlertSummary? {
            val tripIdentity = tripIdentity(patterns, informedTrips, global) ?: return null
            val currentStopName = global.getStop(stopId)?.name
            val location =
                alertLocation(
                    alert,
                    stopId,
                    directionId,
                    patterns.filter { pattern ->
                        informedTrips.any { it.trip.routePatternId == pattern.id }
                    },
                    global,
                )
            return if (currentStopName != null && location is Location.SuccessiveStops) {
                TripShuttleAlertSummary(
                    tripIdentity,
                    if (location.downstream == false) currentStopName else location.startStopName,
                    location.endStopName,
                    recurrence,
                )
            } else {
                null
            }
        }

        private fun tripIdentity(
            patterns: List<RoutePattern>,
            informedTrips: List<UpcomingTrip>,
            global: GlobalResponse,
        ): TripIdentity? {
            return when (informedTrips.singleOrNull()) {
                null if informedTrips.isEmpty() -> null
                null -> MultipleTrips

                else -> {
                    val routeType =
                        patterns.firstNotNullOfOrNull { global.getRoute(it.routeId)?.type }
                            ?: return null
                    ThisTrip(routeType)
                }
            }
        }
    }
}
