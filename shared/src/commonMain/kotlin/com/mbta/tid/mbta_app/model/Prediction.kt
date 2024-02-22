package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.GetReferenceIdSerializer
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Prediction(
    val id: String,
    @SerialName("arrival_time") val arrivalTime: Instant?,
    @SerialName("departure_time") val departureTime: Instant?,
    @SerialName("direction_id") val directionId: Int,
    val revenue: Boolean,
    @SerialName("schedule_relationship") val scheduleRelationship: ScheduleRelationship,
    val status: String?,
    @SerialName("stop_sequence") val stopSequence: Int?,
    @Serializable(with = GetReferenceIdSerializer::class) @SerialName("stop") val stopId: String?,
    val trip: Trip,
    val vehicle: Vehicle?
) : Comparable<Prediction> {
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
        nullsLast<Instant>()
            .compare(arrivalTime ?: departureTime, other.arrivalTime ?: other.departureTime)

    /**
     * The state in which a prediction should be shown.
     *
     * Can be localized in the frontend layer, except for `Overridden` which is always English.
     */
    sealed class Format {
        data class Overridden(val text: String) : Format()

        data object Hidden : Format()

        data object Boarding : Format()

        data object Arriving : Format()

        data object Approaching : Format()

        data object DistantFuture : Format()

        data class Minutes(val minutes: Int) : Format()
    }

    fun format(now: Instant): Format {
        if (status != null) {
            return Format.Overridden(status)
        }
        if (departureTime == null || departureTime < now) {
            return Format.Hidden
        }
        // since we checked departureTime as non-null, we don't have to also check predictionTime
        val timeRemaining = predictionTime!!.minus(now)
        if (
            vehicle?.currentStatus == Vehicle.CurrentStatus.StoppedAt &&
                vehicle.stopId == stopId &&
                vehicle.tripId == trip.id
        ) {
            return Format.Boarding
        }
        if (timeRemaining <= 30.seconds) {
            return Format.Arriving
        }
        if (timeRemaining <= 60.seconds) {
            return Format.Approaching
        }
        if (timeRemaining > 20.minutes) {
            return Format.DistantFuture
        }
        val minutes = timeRemaining.toDouble(DurationUnit.MINUTES).roundToInt()
        return Format.Minutes(minutes)
    }
}
