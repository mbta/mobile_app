package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("trip_specific")
public data class TripSpecificAlertSummary
@DefaultArgumentInterop.Enabled
constructor(
    @SerialName("trip_identity") val tripIdentity: TripIdentity,
    override val effect: Alert.Effect,
    @SerialName("effect_stops") val effectStops: List<String>? = null,
    @SerialName("is_today") val isToday: Boolean = true,
    val cause: Alert.Cause? = null,
    override val recurrence: Recurrence? = null,
) : AlertSummary() {
    @Serializable public sealed interface TripIdentity

    @Serializable
    @SerialName("trip_from")
    public data class TripFrom(
        @SerialName("trip_time") val tripTime: EasternTimeInstant,
        @SerialName("stop_name") val stopName: String,
    ) : TripIdentity

    @Serializable
    @SerialName("trip_to")
    public data class TripTo(
        @SerialName("trip_time") val tripTime: EasternTimeInstant,
        val headsign: String,
    ) : TripIdentity

    @Serializable @SerialName("multiple_trips") public data object MultipleTrips : TripIdentity

    internal companion object {
        fun summary(
            alert: Alert,
            stopId: String,
            directionId: Int,
            patterns: List<RoutePattern>,
            atTime: EasternTimeInstant,
            upcomingTrips: List<UpcomingTrip>?,
            global: GlobalResponse,
            recurrence: Recurrence?,
        ): AlertSummary? {
            val informedTrips =
                upcomingTrips.orEmpty().filter { trip ->
                    alert.anyInformedEntity { it.trip == trip.trip.id }
                }
            // if it was easy to pick the selected trip, that’d be nice, but it isn’t
            val informedTrip = informedTrips.singleOrNull()
            return when (alert.effect) {
                Alert.Effect.Shuttle -> {
                    TripShuttleAlertSummary.summary(
                        alert,
                        stopId,
                        directionId,
                        patterns,
                        atTime,
                        informedTrips,
                        global,
                        recurrence,
                    )
                }
                Alert.Effect.StationClosure -> {
                    val (rawTripIdentity, isToday) =
                        tripIdentityIsToday(stopId, atTime, informedTrips, global) ?: return null
                    val tripIdentity =
                        when (rawTripIdentity) {
                            is TripFrom ->
                                TripTo(
                                    rawTripIdentity.tripTime,
                                    informedTrip?.headsign ?: return null,
                                )
                            else -> rawTripIdentity
                        }
                    val informedStops =
                        alert.informedEntity
                            .mapNotNull { it.stop }
                            .distinct()
                            .mapNotNull { global.getStop(it)?.name }
                            .distinct()
                    TripSpecificAlertSummary(
                        tripIdentity,
                        alert.effect,
                        informedStops,
                        isToday,
                        alert.cause,
                        recurrence,
                    )
                }
                else -> {
                    val (tripIdentity, isToday) =
                        tripIdentityIsToday(stopId, atTime, informedTrips, global) ?: return null
                    TripSpecificAlertSummary(
                        tripIdentity,
                        alert.effect,
                        null,
                        isToday,
                        alert.cause,
                        recurrence,
                    )
                }
            }
        }

        private fun tripIdentityIsToday(
            stopId: String,
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
                    Pair(
                        TripFrom(tripTime, global.getStop(stopId)?.name ?: return null),
                        tripTime.serviceDate == atTime.serviceDate,
                    )
                }
            }
        }
    }
}
