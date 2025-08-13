package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal val ARRIVAL_CUTOFF = 30.seconds
internal val APPROACH_CUTOFF = 60.seconds
internal val BOARDING_CUTOFF = 90.seconds
internal val SCHEDULE_CLOCK_CUTOFF = 60.minutes

@Serializable
public data class Prediction
internal constructor(
    override val id: String,
    @SerialName("arrival_time") override val arrivalTime: EasternTimeInstant?,
    @SerialName("departure_time") override val departureTime: EasternTimeInstant?,
    @SerialName("direction_id") val directionId: Int,
    val revenue: Boolean,
    @SerialName("schedule_relationship") val scheduleRelationship: ScheduleRelationship,
    val status: String?,
    @SerialName("stop_sequence") val stopSequence: Int,
    @SerialName("route_id") val routeId: String,
    @SerialName("stop_id") val stopId: String,
    @SerialName("trip_id") val tripId: String,
    @SerialName("vehicle_id") val vehicleId: String?,
) : BackendObject, TripStopTime {
    @Serializable
    public enum class ScheduleRelationship {
        @SerialName("added") Added,
        @SerialName("cancelled") Cancelled,
        @SerialName("no_data") NoData,
        @SerialName("skipped") Skipped,
        @SerialName("unscheduled") Unscheduled,
        @SerialName("scheduled") Scheduled,
    }
}
