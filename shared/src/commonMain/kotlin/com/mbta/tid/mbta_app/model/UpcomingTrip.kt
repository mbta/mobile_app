package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.utils.resolveParentId
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
data class UpcomingTrip
@DefaultArgumentInterop.Enabled
constructor(
    val trip: Trip,
    val schedule: Schedule? = null,
    val prediction: Prediction? = null,
    val predictionStop: Stop? = null,
    val vehicle: Vehicle? = null
) : Comparable<UpcomingTrip> {

    constructor(
        trip: Trip,
        prediction: Prediction?,
        predictionStop: Stop? = null
    ) : this(trip, null, prediction, predictionStop, null)

    val time =
        if (
            prediction != null &&
                prediction.scheduleRelationship != Prediction.ScheduleRelationship.Cancelled
        ) {
            prediction.predictionTime
        } else {
            schedule?.scheduleTime
        }

    val stopId: String? = run {
        // don't check that they match since prediction may be physical stop ID and schedule logical
        prediction?.stopId ?: schedule?.stopId
    }

    val stopSequence: Int? = run {
        if (schedule != null && prediction?.stopSequence != null) {
            check(schedule.stopSequence == prediction.stopSequence)
        }
        prediction?.stopSequence ?: schedule?.stopSequence
    }

    val isCancelled: Boolean
        get() =
            schedule?.scheduleTime != null &&
                prediction?.scheduleRelationship == Prediction.ScheduleRelationship.Cancelled

    val trackNumber: String?
        get() = predictionStop?.platformCode

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

    fun format(now: Instant, routeType: RouteType?, context: TripInstantDisplay.Context) =
        TripInstantDisplay.from(prediction, schedule, vehicle, routeType, now, context = context)

    companion object {
        fun <Key> tripsMappedBy(
            stops: Map<String, Stop>,
            schedules: ScheduleResponse?,
            predictions: PredictionsStreamDataResponse?,
            scheduleKey: (Schedule, ScheduleResponse) -> Key?,
            predictionKey: (Prediction, PredictionsStreamDataResponse) -> Key?,
            filterAtTime: Instant
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
                val trips = schedules?.trips.orEmpty() + predictions?.trips.orEmpty()
                val upcomingTripKeys =
                    schedulesMap?.keys.orEmpty().filterNotNull() +
                        predictionsMap?.keys.orEmpty().filterNotNull()
                upcomingTripKeys.associateWith { upcomingTripKey ->
                    val schedulesHere = schedulesMap?.get(upcomingTripKey)
                    val predictionsHere = predictionsMap?.get(upcomingTripKey)
                    tripsFromData(
                        stops,
                        schedulesHere.orEmpty(),
                        predictionsHere.orEmpty(),
                        trips,
                        predictions?.vehicles.orEmpty(),
                        filterAtTime
                    )
                }
            } else {
                null
            }
        }

        /**
         * Gets the list of [UpcomingTrip]s from the given [schedules], [predictions] and
         * [vehicles]. Matches by trip ID, parent stop ID, and stop sequence.
         */
        fun tripsFromData(
            stops: Map<String, Stop>,
            schedules: List<Schedule>,
            predictions: List<Prediction>,
            trips: Map<String, Trip>,
            vehicles: Map<String, Vehicle>,
            filterAtTime: Instant
        ): List<UpcomingTrip> {
            data class UpcomingTripKey(
                val tripId: String,
                val rootStopId: String?,
                val stopSequence: Int?
            ) {
                constructor(
                    schedule: Schedule
                ) : this(
                    schedule.tripId,
                    stops.resolveParentId(schedule.stopId),
                    schedule.stopSequence
                )

                constructor(
                    prediction: Prediction
                ) : this(
                    prediction.tripId,
                    stops.resolveParentId(prediction.stopId),
                    prediction.stopSequence
                )
            }

            val schedulesMap = schedules.associateBy { UpcomingTripKey(it) }
            val predictionsMap = predictions.associateBy { UpcomingTripKey(it) }

            val keys = schedulesMap.keys + predictionsMap.keys

            return keys
                .mapNotNull { key ->
                    val prediction = predictionsMap[key]
                    UpcomingTrip(
                        trips[key.tripId] ?: return@mapNotNull null,
                        schedulesMap[key],
                        prediction,
                        stops[prediction?.stopId],
                        predictionsMap[key]?.let { vehicles[it.vehicleId] }
                    )
                }
                .sorted()
                .filter { upcomingTrip ->
                    if (upcomingTrip.prediction != null) return@filter true
                    val scheduleTime = upcomingTrip.schedule?.scheduleTime ?: return@filter true
                    scheduleTime >= filterAtTime
                }
        }
    }
}
