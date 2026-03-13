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
    @SerialName("current_stop_name") val currentStopName: String,
    @SerialName("end_stop_name") val endStopName: String,
    @SerialName("is_today") val isToday: Boolean = true,
    override val recurrence: Recurrence? = null,
) : AlertSummary() {
    override val effect: Alert.Effect = Alert.Effect.Shuttle

    @Serializable public sealed interface TripIdentity

    @Serializable
    @SerialName("single_trip")
    public data class SingleTrip(
        @SerialName("trip_time") val tripTime: EasternTimeInstant,
        @SerialName("route_type") val routeType: RouteType,
    ) : TripIdentity

    @Serializable @SerialName("multiple_trips") public data object MultipleTrips : TripIdentity

    internal companion object {
        fun summary(
            alert: Alert,
            stopId: String,
            directionId: Int,
            patterns: List<RoutePattern>,
            atTime: EasternTimeInstant,
            informedTrips: List<UpcomingTrip>,
            global: GlobalResponse,
            recurrence: Recurrence?,
        ): TripShuttleAlertSummary? {
            val (tripIdentity, isToday) =
                tripIdentityIsToday(patterns, atTime, informedTrips, global) ?: return null
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
                    currentStopName,
                    location.endStopName,
                    isToday,
                    recurrence,
                )
            } else {
                null
            }
        }

        private fun tripIdentityIsToday(
            patterns: List<RoutePattern>,
            atTime: EasternTimeInstant,
            informedTrips: List<UpcomingTrip>,
            global: GlobalResponse,
        ): Pair<TripIdentity, Boolean>? {
            return when (val informedTrip = informedTrips.singleOrNull()) {
                null if informedTrips.isEmpty() -> null
                null ->
                    Pair(
                        MultipleTrips,
                        informedTrips.any {
                            (it.schedule?.departureTime ?: it.schedule?.arrivalTime)?.serviceDate ==
                                atTime.serviceDate
                        },
                    )

                else -> {
                    val tripTime =
                        informedTrip.schedule?.departureTime
                            ?: informedTrip.schedule?.arrivalTime
                            ?: return null
                    val routeType =
                        patterns.firstNotNullOfOrNull { global.getRoute(it.routeId)?.type }
                            ?: return null
                    Pair(
                        SingleTrip(tripTime, routeType),
                        tripTime.serviceDate == atTime.serviceDate,
                    )
                }
            }
        }
    }
}
