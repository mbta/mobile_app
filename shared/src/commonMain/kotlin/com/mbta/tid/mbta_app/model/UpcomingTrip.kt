package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlinx.datetime.Instant

data class UpcomingTrip
@DefaultArgumentInterop.Enabled
constructor(val prediction: Prediction, val vehicle: Vehicle? = null) : Comparable<UpcomingTrip> {
    val time = prediction.predictionTime
    val id = checkNotNull(prediction.tripId)

    override fun compareTo(other: UpcomingTrip) = nullsLast<Instant>().compare(time, other.time)

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

        data class DistantFuture(val predictionTime: Instant) : Format()

        data class Minutes(val minutes: Int) : Format()
    }

    fun format(now: Instant): Format {
        prediction.status?.let {
            return Format.Overridden(it)
        }
        val departureTime = prediction.departureTime
        if (departureTime == null || departureTime < now) {
            return Format.Hidden
        }
        // since we checked departureTime as non-null, we don't have to also check predictionTime
        val timeRemaining = prediction.predictionTime!!.minus(now)
        if (
            vehicle?.currentStatus == Vehicle.CurrentStatus.StoppedAt &&
                vehicle.stopId == prediction.stopId &&
                vehicle.tripId == prediction.tripId
        ) {
            return Format.Boarding
        }
        if (timeRemaining <= ARRIVAL_CUTOFF) {
            return Format.Arriving
        }
        if (timeRemaining <= APPROACH_CUTOFF) {
            return Format.Approaching
        }
        if (timeRemaining > DISTANT_FUTURE_CUTOFF) {
            return Format.DistantFuture(prediction.predictionTime)
        }
        val minutes = timeRemaining.toDouble(DurationUnit.MINUTES).roundToInt()
        return Format.Minutes(minutes)
    }

    companion object {
        /** Gets the list of [UpcomingTrip]s from the given [predictions] and [vehicles]. */
        fun tripsFromData(
            predictions: List<Prediction>,
            vehicles: Map<String, Vehicle>
        ): List<UpcomingTrip> {
            return predictions.map { prediction ->
                UpcomingTrip(prediction, vehicles[prediction.vehicleId])
            }
        }
    }
}
