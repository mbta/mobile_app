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

    data class Time(val predictionTime: Instant, val headline: Boolean = false) :
        TripInstantDisplay()

    data class TimeWithStatus(
        val predictionTime: Instant,
        val status: String,
        val headline: Boolean = false
    ) : TripInstantDisplay()

    data class Minutes(val minutes: Int) : TripInstantDisplay()

    data class ScheduleTime(val scheduledTime: Instant, val headline: Boolean = false) :
        TripInstantDisplay()

    data class ScheduleMinutes(val minutes: Int) : TripInstantDisplay()

    data class Skipped(val scheduledTime: Instant?) : TripInstantDisplay()

    data class Cancelled(val scheduledTime: Instant) : TripInstantDisplay()

    enum class Context {
        NearbyTransit,
        StopDetailsUnfiltered,
        StopDetailsFiltered,
        TripDetails,
    }

    companion object {
        fun from(
            prediction: Prediction?,
            schedule: Schedule?,
            vehicle: Vehicle?,
            routeType: RouteType?,
            now: Instant,
            context: Context
        ): TripInstantDisplay {
            val allowArrivalOnly = context == Context.TripDetails
            val forceAsTime = context == Context.TripDetails
            if (prediction?.status != null && routeType != RouteType.COMMUTER_RAIL) {
                return Overridden(prediction.status)
            }
            if (prediction?.scheduleRelationship == Prediction.ScheduleRelationship.Skipped) {
                schedule?.scheduleTime?.let {
                    return Skipped(it)
                }
                return Hidden
            }

            val isScheduleUpcoming = schedule?.scheduleTime?.let { it >= now } ?: false
            if (
                prediction?.scheduleRelationship == Prediction.ScheduleRelationship.Cancelled &&
                    schedule?.scheduleTime != null &&
                    isScheduleUpcoming &&
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

            val scheduleBasedRouteType =
                routeType == RouteType.COMMUTER_RAIL || routeType == RouteType.FERRY
            if (prediction == null) {
                val scheduleTime = schedule?.scheduleTime
                return if (scheduleTime == null) {
                    Hidden
                } else {
                    val scheduleTimeRemaining = scheduleTime.minus(now)
                    if (
                        scheduleTimeRemaining > SCHEDULE_CLOCK_CUTOFF ||
                            scheduleBasedRouteType ||
                            forceAsTime
                    ) {
                        ScheduleTime(
                            scheduleTime,
                            headline = scheduleBasedRouteType && !forceAsTime
                        )
                    } else {
                        val scheduleMinutes =
                            scheduleTimeRemaining.toDouble(DurationUnit.MINUTES).roundToInt()
                        ScheduleMinutes(scheduleMinutes)
                    }
                }
            }
            // since we checked departureTime or arrivalTime as non-null, we don't have to also
            // check  predictionTime
            val timeRemaining = prediction.predictionTime!!.minus(now)
            val minutes = timeRemaining.toDouble(DurationUnit.MINUTES).roundToInt()

            fun Time.potentiallyWithStatus(): TripInstantDisplay {
                val status = prediction.status ?: return this
                return when (routeType) {
                    RouteType.COMMUTER_RAIL ->
                        TimeWithStatus(this.predictionTime, status, this.headline)
                    else -> this
                }
            }

            if (forceAsTime) {
                return Time(prediction.predictionTime).potentiallyWithStatus()
            }

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
            if (timeRemaining <= APPROACH_CUTOFF) {
                return Approaching
            }
            if (scheduleBasedRouteType) {
                return Time(prediction.predictionTime, headline = true).potentiallyWithStatus()
            }
            return Minutes(minutes)
        }
    }
}
