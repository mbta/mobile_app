package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.math.roundToInt
import kotlin.time.DurationUnit

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

    data class Time(val predictionTime: EasternTimeInstant, val headline: Boolean = false) :
        TripInstantDisplay()

    data class TimeWithStatus(
        val predictionTime: EasternTimeInstant,
        val status: String,
        val headline: Boolean = false,
    ) : TripInstantDisplay()

    data class TimeWithSchedule(
        val predictionTime: EasternTimeInstant,
        val scheduledTime: EasternTimeInstant,
        val headline: Boolean = false,
    ) : TripInstantDisplay()

    data class Minutes(val minutes: Int) : TripInstantDisplay()

    data class ScheduleTime(val scheduledTime: EasternTimeInstant, val headline: Boolean = false) :
        TripInstantDisplay()

    data class ScheduleTimeWithStatus(
        val scheduledTime: EasternTimeInstant,
        val status: String,
        val headline: Boolean = false,
    ) : TripInstantDisplay()

    data class ScheduleMinutes(val minutes: Int) : TripInstantDisplay()

    data class Skipped(val scheduledTime: EasternTimeInstant?) : TripInstantDisplay()

    data class Cancelled(val scheduledTime: EasternTimeInstant) : TripInstantDisplay()

    enum class Context {
        NearbyTransit,
        StopDetailsUnfiltered,
        StopDetailsFiltered,
        TripDetails,
    }

    companion object {
        val delayStatuses = setOf("Delay", "Late")

        fun from(
            prediction: Prediction?,
            schedule: Schedule?,
            vehicle: Vehicle?,
            routeType: RouteType?,
            now: EasternTimeInstant,
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
                    val scheduleMinutesRemaining =
                        scheduleTime.minus(now).toDouble(DurationUnit.MINUTES).roundToInt()
                    if (
                        scheduleMinutesRemaining >=
                            SCHEDULE_CLOCK_CUTOFF.toDouble(DurationUnit.MINUTES) || forceAsTime
                    ) {
                        ScheduleTime(scheduleTime, headline = showTimeAsHeadline)
                    } else {
                        ScheduleMinutes(scheduleMinutesRemaining)
                    }
                }
            }
            val predictionTime = prediction.stopTimeAfter(now)
            if (allowTimeWithStatus && prediction.status != null) {
                if (predictionTime != null) {
                    return TimeWithStatus(predictionTime, prediction.status, showTimeAsHeadline)
                }
                if (scheduleStopTime != null) {
                    return ScheduleTimeWithStatus(
                        scheduleStopTime,
                        prediction.status,
                        showTimeAsHeadline,
                    )
                }
                return Overridden(prediction.status)
            }
            if (predictionTime == null || (prediction.departureTime == null && !allowArrivalOnly)) {
                return Hidden
            }
            val timeRemaining = predictionTime.minus(now)
            val minutes = timeRemaining.toDouble(DurationUnit.MINUTES).roundToInt()

            if (forceAsTime) {
                val scheduleTime = schedule?.stopTimeAfter(now)
                val showDelayedSchedule =
                    context == Context.StopDetailsFiltered &&
                        scheduleTime?.let {
                            it.local.minute != predictionTime.local.minute ||
                                it.local.hour != predictionTime.local.hour
                        } == true

                return if (timeRemaining.isNegative()) {
                    Hidden
                } else if (showDelayedSchedule) {
                    TimeWithSchedule(predictionTime, scheduleTime, headline = showTimeAsHeadline)
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
