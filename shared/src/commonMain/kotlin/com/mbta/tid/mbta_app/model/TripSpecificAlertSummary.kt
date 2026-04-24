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
    @SerialName("this_trip")
    public data class ThisTrip(@SerialName("route_type") val routeType: RouteType) : TripIdentity

    @Serializable
    @SerialName("trip_from")
    public data class TripFrom(
        @SerialName("trip_time") val tripTime: EasternTimeInstant,
        @SerialName("route_type") val routeType: RouteType,
        @SerialName("stop_name") val stopName: String,
    ) : TripIdentity

    @Serializable
    @SerialName("trip_to")
    public data class TripTo(
        @SerialName("trip_time") val tripTime: EasternTimeInstant,
        @SerialName("route_type") val routeType: RouteType,
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
                        informedTrips,
                        global,
                        recurrence,
                    )
                }
                Alert.Effect.StationClosure,
                Alert.Effect.StopClosure,
                Alert.Effect.DockClosure -> {
                    val routeType =
                        patterns.firstNotNullOfOrNull { global.getRoute(it.routeId)?.type }
                            ?: return null
                    val (rawTripIdentity, isToday) =
                        tripIdentityIsToday(atTime, routeType, informedTrips) ?: return null
                    val tripIdentity =
                        when (rawTripIdentity) {
                            is TripFrom ->
                                TripTo(
                                    rawTripIdentity.tripTime,
                                    routeType,
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
                    val routeType =
                        patterns.firstNotNullOfOrNull { global.getRoute(it.routeId)?.type }
                            ?: return null
                    val (tripIdentity, isToday) =
                        tripIdentityIsToday(atTime, routeType, informedTrips) ?: return null
                    val suspensionEffectStops =
                        if (alert.effect == Alert.Effect.Suspension) {
                            val location =
                                alertLocation(alert, stopId, directionId, patterns, global)
                            when (location) {
                                is Location.SingleStop ->
                                    if (location.downstream == true) location.stopName else null
                                is Location.SuccessiveStops ->
                                    if (location.downstream == true) location.startStopName
                                    else null
                                is Location.StopToDirection ->
                                    if (location.downstream == true) location.startStopName
                                    else null
                                else -> null
                            }?.let { listOf(it) }
                        } else null
                    TripSpecificAlertSummary(
                        tripIdentity,
                        alert.effect,
                        suspensionEffectStops,
                        isToday,
                        alert.cause,
                        recurrence,
                    )
                }
            }
        }

        private fun tripIdentityIsToday(
            atTime: EasternTimeInstant,
            routeType: RouteType,
            informedTrips: List<UpcomingTrip>,
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
                    Pair(ThisTrip(routeType), tripTime.serviceDate == atTime.serviceDate)
                }
            }
        }
    }
}
