package com.mbta.tid.mbta_app.model

import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlinx.datetime.Instant

/**
 * The state in which a prediction/schedule should be shown.
 *
 * Can be localized in the frontend layer, except for `Overridden` which is always English.
 */
sealed class TripInstantDisplay {
    data class Overridden(val text: String) : TripInstantDisplay()

    data object Hidden : TripInstantDisplay()

    data object Boarding : TripInstantDisplay()

    data object Arriving : TripInstantDisplay()

    data object Approaching : TripInstantDisplay()

    data object Now : TripInstantDisplay()

    data class AsTime(val predictionTime: Instant) : TripInstantDisplay()

    data class Schedule(val scheduleTime: Instant) : TripInstantDisplay()

    data class Skipped(val scheduledTime: Instant?) : TripInstantDisplay()

    data class Cancelled(val scheduledTime: Instant) : TripInstantDisplay()

    data class Minutes(val minutes: Int) : TripInstantDisplay()

    enum class Context {
        NearbyTransit,
        StopDetailsUnfiltered,
        StopDetailsFiltered,
        TripDetails,
    }

    companion object {
        fun from(
            prediction: Prediction?,
            schedule: com.mbta.tid.mbta_app.model.Schedule?,
            vehicle: Vehicle?,
            routeType: RouteType?,
            now: Instant,
            context: Context
        ): TripInstantDisplay {
            val allowArrivalOnly = context == Context.TripDetails
            val forceAsTime = context == Context.TripDetails
            prediction?.status?.let {
                return Overridden(it)
            }
            if (prediction?.scheduleRelationship == Prediction.ScheduleRelationship.Skipped) {
                schedule?.scheduleTime?.let {
                    return Skipped(it)
                }
                return Hidden
            }
            if (
                prediction?.scheduleRelationship == Prediction.ScheduleRelationship.Cancelled &&
                    schedule?.scheduleTime != null &&
                    routeType?.isSubway() == false &&
                    context == Context.StopDetailsFiltered
            ) {
                return Cancelled(schedule.scheduleTime)
            }
            val departureTime =
                if (prediction != null) {
                    prediction.departureTime
                } else {
                    schedule?.departureTime
                }
            val arrivalTime =
                if (allowArrivalOnly) {
                    if (prediction != null) {
                        prediction.arrivalTime
                    } else {
                        schedule?.arrivalTime
                    }
                } else {
                    null
                }
            val hasDepartureToDisplay = !(departureTime == null || departureTime < now)
            val hasArrivalToDisplay =
                allowArrivalOnly && !(arrivalTime == null || arrivalTime < now)
            if (!(hasDepartureToDisplay || hasArrivalToDisplay)) {
                return Hidden
            }
            if (prediction == null) {
                val scheduleTime = schedule?.scheduleTime
                return if (scheduleTime == null) {
                    Hidden
                } else {
                    Schedule(scheduleTime)
                }
            }
            // since we checked departureTime or arrivalTime as non-null, we don't have to also
            // check  predictionTime
            val timeRemaining = prediction.predictionTime!!.minus(now)
            val minutes = timeRemaining.toDouble(DurationUnit.MINUTES).roundToInt()
            /**
             * Because the gap between boarding and arriving is much smaller for bus, having
             * different states doesn’t provide much rider value so we return Now instead
             */
            if (routeType == RouteType.BUS && timeRemaining <= ARRIVAL_CUTOFF) {
                return Now
            } else if (routeType == RouteType.BUS) {
                return Minutes(minutes)
            }
            if (
                vehicle?.currentStatus == Vehicle.CurrentStatus.StoppedAt &&
                    vehicle.stopId == prediction.stopId &&
                    vehicle.tripId == prediction.tripId &&
                    timeRemaining <= BOARDING_CUTOFF
            ) {
                return Boarding
            }
            if (timeRemaining <= ARRIVAL_CUTOFF) {
                return Arriving
            }
            if (timeRemaining <= APPROACH_CUTOFF && !forceAsTime) {
                return Approaching
            }
            if (routeType == RouteType.COMMUTER_RAIL || forceAsTime) {
                return AsTime(prediction.predictionTime)
            }
            return Minutes(minutes)
        }
    }
}
