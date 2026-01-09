package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.math.roundToInt
import kotlin.time.DurationUnit

/**
 * The state in which a prediction/schedule should be shown.
 *
 * Can be localized in the frontend layer, except for `Overridden` which is always English.
 */
public sealed class TripInstantDisplay {
    public data class Overridden(val text: String, val last: Boolean) : TripInstantDisplay()

    public data object Hidden : TripInstantDisplay()

    public data class Boarding(val last: Boolean) : TripInstantDisplay()

    public data class Arriving(val last: Boolean) : TripInstantDisplay()

    public data class Approaching(val last: Boolean) : TripInstantDisplay()

    public data class Now(val last: Boolean) : TripInstantDisplay()

    public data class Time(
        val predictionTime: EasternTimeInstant,
        val last: Boolean,
        val headline: Boolean = false,
    ) : TripInstantDisplay()

    public data class TimeWithStatus(
        val predictionTime: EasternTimeInstant,
        val status: String,
        val last: Boolean,
        val headline: Boolean = false,
    ) : TripInstantDisplay()

    public data class TimeWithSchedule(
        val predictionTime: EasternTimeInstant,
        val scheduledTime: EasternTimeInstant,
        val last: Boolean,
        val headline: Boolean = false,
    ) : TripInstantDisplay()

    public data class Minutes(val minutes: Int, val last: Boolean) : TripInstantDisplay()

    public data class ScheduleTime(
        val scheduledTime: EasternTimeInstant,
        val last: Boolean,
        val headline: Boolean = false,
    ) : TripInstantDisplay()

    public data class ScheduleTimeWithStatusColumn(
        val scheduledTime: EasternTimeInstant,
        val status: String,
        val last: Boolean,
        val headline: Boolean = false,
    ) : TripInstantDisplay()

    // This has no last trip boolean because if there is a schedule time with a status and it's the
    // last trip, it will always be shown in column form, even on nearby transit
    public data class ScheduleTimeWithStatusRow(
        val scheduledTime: EasternTimeInstant,
        val status: String,
    ) : TripInstantDisplay()

    public data class ScheduleMinutes(val minutes: Int, val last: Boolean) : TripInstantDisplay()

    public data class Skipped(val scheduledTime: EasternTimeInstant?) : TripInstantDisplay()

    public data class Cancelled(val scheduledTime: EasternTimeInstant) : TripInstantDisplay()

    public enum class Context {
        NearbyTransit,
        StopDetailsUnfiltered,
        StopDetailsFiltered,
        TripDetails,
    }

    public companion object {
        public val delayStatuses: Set<String> = setOf("Delay", "Delayed", "Late")

        internal fun from(
            prediction: Prediction?,
            schedule: Schedule?,
            vehicle: Vehicle?,
            routeType: RouteType?,
            now: EasternTimeInstant,
            context: Context,
            lastTrip: Boolean,
        ): TripInstantDisplay {
            val allowArrivalOnly = context == Context.TripDetails
            val scheduleBasedRouteType =
                routeType == RouteType.COMMUTER_RAIL || routeType == RouteType.FERRY
            val forceAsTime = context == Context.TripDetails || scheduleBasedRouteType
            val showTimeAsHeadline = scheduleBasedRouteType && context != Context.TripDetails
            val predictionTime = prediction?.stopTimeAfter(now)
            val scheduleTime = schedule?.stopTimeAfter(now)
            if (prediction?.status != null) {
                if (routeType == RouteType.COMMUTER_RAIL) {
                    when {
                        predictionTime != null && context == Context.StopDetailsFiltered ->
                            return TimeWithStatus(
                                predictionTime,
                                prediction.status,
                                lastTrip,
                                showTimeAsHeadline,
                            )
                        predictionTime != null ->
                            return Time(predictionTime, lastTrip, showTimeAsHeadline)
                        scheduleTime != null &&
                            (context == Context.StopDetailsFiltered || lastTrip) ->
                            return ScheduleTimeWithStatusColumn(
                                scheduleTime,
                                prediction.status,
                                lastTrip,
                                showTimeAsHeadline,
                            )
                        scheduleTime != null && scheduleTime < now ->
                            return ScheduleTimeWithStatusRow(scheduleTime, prediction.status)
                        scheduleTime != null ->
                            return ScheduleTime(scheduleTime, lastTrip, showTimeAsHeadline)
                    }
                }
                return Overridden(prediction.status, lastTrip)
            }
            if (prediction?.scheduleRelationship == Prediction.ScheduleRelationship.Skipped) {
                schedule?.stopTime?.let {
                    return Skipped(it)
                }
                return Hidden
            }

            val isScheduleUpcoming = scheduleTime?.let { it >= now } ?: false
            if (
                prediction?.scheduleRelationship == Prediction.ScheduleRelationship.Cancelled &&
                    scheduleTime != null &&
                    isScheduleUpcoming &&
                    routeType?.isSubway() == false &&
                    context == Context.StopDetailsFiltered
            ) {
                return Cancelled(scheduleTime)
            }

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
                        ScheduleTime(scheduleTime, lastTrip, headline = showTimeAsHeadline)
                    } else {
                        ScheduleMinutes(scheduleMinutesRemaining, lastTrip)
                    }
                }
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
                    TimeWithSchedule(
                        predictionTime,
                        scheduleTime,
                        lastTrip,
                        headline = showTimeAsHeadline,
                    )
                } else {
                    Time(predictionTime, lastTrip, headline = showTimeAsHeadline)
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
                    Now(lastTrip)
                } else {
                    Minutes(minutes, lastTrip)
                }
            }
            if (
                vehicle?.currentStatus == Vehicle.CurrentStatus.StoppedAt &&
                    vehicle.stopId == prediction.stopId &&
                    vehicle.tripId == prediction.tripId &&
                    (timeRemaining <= BOARDING_CUTOFF || prediction.hasArrivedButNotDeparted(now))
            ) {
                return Boarding(lastTrip)
            }
            if (timeRemaining.isNegative()) {
                return Hidden
            }
            if (timeRemaining <= ARRIVAL_CUTOFF || prediction.hasArrivedButNotDeparted(now)) {
                return Arriving(lastTrip)
            }
            if (timeRemaining <= APPROACH_CUTOFF) {
                return Approaching(lastTrip)
            }
            return Minutes(minutes, lastTrip)
        }
    }
}
