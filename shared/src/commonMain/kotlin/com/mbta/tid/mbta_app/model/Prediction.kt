package com.mbta.tid.mbta_app.model

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

val ARRIVAL_CUTOFF = 30.seconds
val APPROACH_CUTOFF = 60.seconds
val BOARDING_CUTOFF = 90.seconds
val DISTANT_FUTURE_CUTOFF = 60.minutes

@Serializable
data class Prediction(
    override val id: String,
    @SerialName("arrival_time") val arrivalTime: Instant?,
    @SerialName("departure_time") val departureTime: Instant?,
    @SerialName("direction_id") val directionId: Int,
    val revenue: Boolean,
    @SerialName("schedule_relationship") val scheduleRelationship: ScheduleRelationship,
    val status: String?,
    @SerialName("stop_sequence") val stopSequence: Int?,
    @SerialName("route_id") val routeId: String,
    @SerialName("stop_id") val stopId: String,
    @SerialName("trip_id") val tripId: String,
    @SerialName("vehicle_id") val vehicleId: String?,
) : Comparable<Prediction>, BackendObject {
    val predictionTime = arrivalTime ?: departureTime

    @Serializable
    enum class ScheduleRelationship {
        @SerialName("added") Added,
        @SerialName("cancelled") Cancelled,
        @SerialName("no_data") NoData,
        @SerialName("skipped") Skipped,
        @SerialName("unscheduled") Unscheduled,
        @SerialName("scheduled") Scheduled
    }

    override fun compareTo(other: Prediction): Int =
        nullsLast<Instant>().compare(predictionTime, other.predictionTime)
}
