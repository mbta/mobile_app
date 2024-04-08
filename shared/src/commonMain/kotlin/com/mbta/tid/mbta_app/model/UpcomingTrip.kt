package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlinx.datetime.Instant

/**
 * Conceptually, an [UpcomingTrip] represents a trip's future arrival and departure at a stop.
 *
 * The time may not be in the future, depending on when stale predictions are removed and how long
 * ago the schedule was fetched. The trip may not arrive at a stop if that stop is the first and may
 * not depart from a stop if that stop is the last.
 *
 * The trip might also neither arrive nor depart if the stop is skipped or the trip is dropped. For
 * this reason, a prediction that exists but has null times should overwrite scheduled times.
 */
data class UpcomingTrip(
    val schedule: Schedule?,
    val prediction: Prediction?,
    val vehicle: Vehicle?
) : Comparable<UpcomingTrip> {
    constructor(schedule: Schedule) : this(schedule, null, null)

    constructor(schedule: Schedule, prediction: Prediction) : this(schedule, prediction, null)

    constructor(prediction: Prediction) : this(null, prediction, null)

    constructor(prediction: Prediction, vehicle: Vehicle) : this(null, prediction, vehicle)

    val time =
        if (prediction != null) {
            prediction.predictionTime
        } else {
            schedule?.scheduleTime
        }
    /** The [Prediction.tripId] of the [prediction], or the [Schedule.tripId] of the [schedule]. */
    val id = checkNotNull(prediction?.tripId ?: schedule?.tripId)

    override fun compareTo(other: UpcomingTrip) = nullsLast<Instant>().compare(time, other.time)

    /**
     * Checks whether this upcoming trip will depart its station or only arrive there.
     *
     * If a trip will neither arrive nor depart (e.g. trips with no schedule that have been
     * cancelled), this function will return `null`. Returning `true` would hide headsigns with no
     * schedule and predictions exclusively for dropped trips, which may happen during suspensions
     * and would be incorrect. Returning `false` would show headsigns with added trips even if those
     * trips have been cancelled, which would be incorrect.
     */
    fun isArrivalOnly(): Boolean? {
        val hasArrival =
            if (schedule != null) {
                schedule.dropOffType != Schedule.StopEdgeType.Unavailable
            } else {
                prediction?.arrivalTime != null
            }
        val hasDeparture =
            if (schedule != null) {
                schedule.pickUpType != Schedule.StopEdgeType.Unavailable
            } else {
                prediction?.departureTime != null
            }
        return if (!hasArrival && !hasDeparture) {
            null
        } else !hasDeparture
    }

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

        data class Schedule(val scheduleTime: Instant) : Format()

        data class Minutes(val minutes: Int) : Format()
    }

    fun format(now: Instant): Format {
        prediction?.status?.let {
            return Format.Overridden(it)
        }
        val departureTime =
            if (prediction != null) {
                prediction.departureTime
            } else {
                schedule?.departureTime
            }
        if (departureTime == null || departureTime < now) {
            return Format.Hidden
        }
        if (prediction == null) {
            val scheduleTime = schedule?.scheduleTime
            return if (scheduleTime == null) {
                Format.Hidden
            } else {
                Format.Schedule(scheduleTime)
            }
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
        fun <Key> tripsMappedBy(
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse?,
            scheduleKey: (Schedule, ScheduleResponse) -> Key,
            predictionKey: (Prediction, PredictionsStreamDataResponse) -> Key
        ): Map<Key, List<UpcomingTrip>>? {
            val schedulesMap =
                schedules?.let { scheduleData ->
                    scheduleData.schedules.groupBy { schedule ->
                        scheduleKey(schedule, scheduleData)
                    }
                }
            val predictionsMap =
                predictions?.let { predictionData ->
                    predictionData.predictions.values.groupBy { prediction ->
                        predictionKey(prediction, predictionData)
                    }
                }
            return if (schedulesMap != null || predictionsMap != null) {
                ((schedulesMap?.keys ?: emptySet()) + (predictionsMap?.keys ?: emptySet()))
                    .associateWith { upcomingTripKey ->
                        val schedulesHere = schedulesMap?.get(upcomingTripKey)
                        val predictionsHere = predictionsMap?.get(upcomingTripKey)
                        tripsFromData(
                            schedulesHere ?: emptyList(),
                            predictionsHere ?: emptyList(),
                            predictions?.vehicles ?: emptyMap()
                        )
                    }
            } else {
                null
            }
        }

        /**
         * Gets the list of [UpcomingTrip]s from the given [schedules], [predictions] and
         * [vehicles]. Matches by trip ID, stop ID, and stop sequence.
         */
        fun tripsFromData(
            schedules: List<Schedule>,
            predictions: List<Prediction>,
            vehicles: Map<String, Vehicle>
        ): List<UpcomingTrip> {
            data class UpcomingTripKey(
                val tripId: String?,
                val stopId: String?,
                val stopSequence: Int?
            ) {
                constructor(
                    schedule: Schedule
                ) : this(schedule.tripId, schedule.stopId, schedule.stopSequence)

                constructor(
                    prediction: Prediction
                ) : this(prediction.tripId, prediction.stopId, prediction.stopSequence)
            }

            val schedulesMap = schedules.associateBy { UpcomingTripKey(it) }
            val predictionsMap = predictions.associateBy { UpcomingTripKey(it) }

            val keys = schedulesMap.keys + predictionsMap.keys

            return keys
                .map { key ->
                    UpcomingTrip(
                        schedulesMap[key],
                        predictionsMap[key],
                        predictionsMap[key]?.let { vehicles[it.vehicleId] }
                    )
                }
                .sorted()
        }
    }
}
