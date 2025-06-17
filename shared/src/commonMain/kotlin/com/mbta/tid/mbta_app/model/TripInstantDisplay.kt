package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.utils.toBostonTime
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
        val headline: Boolean = false,
    ) : TripInstantDisplay()

    data class TimeWithSchedule(
        val predictionTime: Instant,
        val scheduledTime: Instant,
        val headline: Boolean = false,
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
            context: Context,
        ): TripInstantDisplay {
            val allowArrivalOnly = context == Context.TripDetails
            val scheduleBasedRouteType =
                routeType == RouteType.COMMUTER_RAIL || routeType == RouteType.FERRY
            val forceAsTime = context == Context.TripDetails || scheduleBasedRouteType
            val allowTimeWithStatus =
                context == Context.StopDetailsFiltered && routeType == RouteType.COMMUTER_RAIL
            if (prediction?.status != null && routeType != RouteType.COMMUTER_RAIL) {
                return Overridden(prediction.status)
            }
            if (prediction?.scheduleRelationship == Prediction.ScheduleRelationship.Skipped) {
                schedule?.stopTime?.let {
                    return Skipped(it)
                }
                return Hidden
            }

            val scheduleStopTime = schedule?.stopTime
            val isScheduleUpcoming = scheduleStopTime?.let { it >= now } ?: false
            if (
                prediction?.scheduleRelationship == Prediction.ScheduleRelationship.Cancelled &&
                    scheduleStopTime != null &&
                    isScheduleUpcoming &&
                    routeType?.isSubway() == false &&
                    context == Context.StopDetailsFiltered
            ) {
                return Cancelled(scheduleStopTime)
            }

            val showTimeAsHeadline = scheduleBasedRouteType && context != Context.TripDetails
            if (prediction == null) {
                val scheduleTime = schedule?.stopTimeAfter(now)
                return if (
                    scheduleTime == null || (schedule.departureTime == null && !allowArrivalOnly)
                ) {
                    Hidden
                } else {
                    val scheduleTimeRemaining = scheduleTime.minus(now)
                    if (scheduleTimeRemaining > SCHEDULE_CLOCK_CUTOFF || forceAsTime) {
                        ScheduleTime(scheduleTime, headline = showTimeAsHeadline)
                    } else {
                        val scheduleMinutes =
                            scheduleTimeRemaining.toDouble(DurationUnit.MINUTES).roundToInt()
                        ScheduleMinutes(scheduleMinutes)
                    }
                }
            }
            val predictionTime = prediction.stopTimeAfter(now)
            if (predictionTime == null || (prediction.departureTime == null && !allowArrivalOnly)) {
                return Hidden
            }
            val timeRemaining = predictionTime.minus(now)
            val minutes = timeRemaining.toDouble(DurationUnit.MINUTES).roundToInt()

            if (forceAsTime) {
                val scheduleTime = schedule?.stopTimeAfter(now)
                val scheduleZoned = scheduleTime?.toBostonTime()
                val predictionZoned = predictionTime.toBostonTime()
                val showDelayedSchedule =
                    context == Context.StopDetailsFiltered &&
                        scheduleZoned?.let {
                            it.minute != predictionZoned.minute || it.hour != predictionZoned.hour
                        } == true

                return if (timeRemaining.isNegative()) {
                    Hidden
                } else if (showDelayedSchedule && scheduleTime != null) {
                    TimeWithSchedule(predictionTime, scheduleTime, headline = showTimeAsHeadline)
                } else if (allowTimeWithStatus && prediction.status != null) {
                    TimeWithStatus(predictionTime, prediction.status, headline = showTimeAsHeadline)
                } else {
                    Time(predictionTime, headline = showTimeAsHeadline)
                }
            }

            /**
             * Because the gap between boarding and arriving is much smaller for bus, having
             * different states doesn’t provide much rider value so we return Now instead
             */
            if (routeType == RouteType.BUS) {
                return if (timeRemaining.isNegative()) {
                    Hidden
                } else if (timeRemaining <= ARRIVAL_CUTOFF) {
                    Now
                } else {
                    Minutes(minutes)
                }
            }
            if (
                vehicle?.currentStatus == Vehicle.CurrentStatus.StoppedAt &&
                    vehicle.stopId == prediction.stopId &&
                    vehicle.tripId == prediction.tripId &&
                    (timeRemaining <= BOARDING_CUTOFF || prediction.hasArrivedButNotDeparted(now))
            ) {
                return Boarding
            }
            if (timeRemaining.isNegative()) {
                return Hidden
            }
            if (timeRemaining <= ARRIVAL_CUTOFF || prediction.hasArrivedButNotDeparted(now)) {
                return Arriving
            }
            if (timeRemaining <= APPROACH_CUTOFF) {
                return Approaching
            }
            return Minutes(minutes)
        }
    }
}
