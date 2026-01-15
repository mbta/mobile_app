package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.MinutesFormat

@Composable
fun TripInstantDisplay.contentDescription(isFirst: Boolean, vehicleType: String): String =
    when (this) {
        is TripInstantDisplay.Approaching,
        is TripInstantDisplay.Minutes -> predictedMinutesDescription(this, isFirst, vehicleType)
        is TripInstantDisplay.Arriving,
        is TripInstantDisplay.Now -> {
            val lastTrip =
                when (this) {
                    is TripInstantDisplay.Arriving -> this.last
                    is TripInstantDisplay.Now -> this.last
                    else -> false
                }
            val description =
                if (isFirst) stringResource(R.string.vehicle_arriving_first, vehicleType)
                else stringResource(R.string.vehicle_arriving_other)
            withLastTripSuffix(description, lastTrip)
        }
        is TripInstantDisplay.Boarding -> {
            val description =
                if (isFirst) stringResource(R.string.vehicle_boarding_first, vehicleType)
                else stringResource(R.string.vehicle_boarding_other)
            withLastTripSuffix(description, this.last)
        }
        is TripInstantDisplay.Cancelled -> {
            val time = this.scheduledTime.formattedTime()
            val description =
                if (isFirst) stringResource(R.string.vehicle_cancelled_first, vehicleType, time)
                else stringResource(R.string.vehicle_cancelled_other, time)
            description
        }
        is TripInstantDisplay.ScheduleMinutes ->
            scheduledMinutesDescription(this, isFirst, vehicleType)
        is TripInstantDisplay.ScheduleTime -> scheduledTimeDescription(this, isFirst, vehicleType)
        is TripInstantDisplay.Time,
        is TripInstantDisplay.TimeWithStatus -> predictedTimeDescription(this, isFirst, vehicleType)
        is TripInstantDisplay.TimeWithSchedule ->
            predictedWithScheduleDescription(this, isFirst, vehicleType)
        is TripInstantDisplay.ScheduleTimeWithStatusColumn,
        is TripInstantDisplay.ScheduleTimeWithStatusRow ->
            scheduledTimeDescription(this, isFirst, vehicleType)
        is TripInstantDisplay.Overridden -> withLastTripSuffix(this.text, this.last)
        TripInstantDisplay.Hidden,
        is TripInstantDisplay.Skipped -> ""
    }

@Composable
private fun delayDescription(
    scheduledInstant: EasternTimeInstant,
    isFirst: Boolean,
    vehicleType: String,
): String {
    val scheduledTime = scheduledInstant.formattedTime()
    return if (isFirst)
        stringResource(
            R.string.vehicle_prediction_schedule_status_delay_first,
            scheduledTime,
            vehicleType,
        )
    else
        stringResource(
            R.string.vehicle_prediction_schedule_status_delay_other,
            scheduledTime,
            vehicleType,
        )
}

@Composable
private fun predictedMinutesDescription(
    trip: TripInstantDisplay,
    isFirst: Boolean,
    vehicleType: String,
): String {
    val format =
        MinutesFormat.from(
            when (trip) {
                is TripInstantDisplay.Approaching -> 1
                is TripInstantDisplay.Minutes -> trip.minutes
                else -> return ""
            }
        )
    val lastTrip =
        when (trip) {
            is TripInstantDisplay.Approaching -> trip.last
            is TripInstantDisplay.Minutes -> trip.last
            else -> false
        }
    val description =
        if (isFirst)
            when (format) {
                is MinutesFormat.Hour ->
                    stringResource(
                        R.string.vehicle_prediction_hours_first,
                        vehicleType,
                        format.hours,
                    )
                is MinutesFormat.HourMinute ->
                    stringResource(
                        R.string.vehicle_prediction_hours_minutes_first,
                        vehicleType,
                        format.hours,
                        format.minutes,
                    )
                is MinutesFormat.Minute ->
                    stringResource(
                        R.string.vehicle_prediction_minutes_first,
                        vehicleType,
                        format.minutes,
                    )
            }
        else
            when (format) {
                is MinutesFormat.Hour ->
                    stringResource(R.string.vehicle_prediction_hours_other, format.hours)
                is MinutesFormat.HourMinute ->
                    stringResource(
                        R.string.vehicle_prediction_hours_minutes_other,
                        format.hours,
                        format.minutes,
                    )
                is MinutesFormat.Minute ->
                    stringResource(R.string.vehicle_prediction_minutes_other, format.minutes)
            }
    return withLastTripSuffix(description, lastTrip)
}

@Composable
private fun predictedTimeDescription(
    trip: TripInstantDisplay,
    isFirst: Boolean,
    vehicleType: String,
): String {
    val lastTrip =
        when (trip) {
            is TripInstantDisplay.Time -> trip.last
            is TripInstantDisplay.TimeWithStatus -> trip.last
            else -> false
        }
    if (
        trip is TripInstantDisplay.TimeWithStatus && trip.status in TripInstantDisplay.delayStatuses
    )
        return withLastTripSuffix(
            delayDescription(trip.predictionTime, isFirst, vehicleType),
            lastTrip,
        )

    val predictionTime =
        when (trip) {
            is TripInstantDisplay.Time -> trip.predictionTime
            is TripInstantDisplay.TimeWithStatus -> trip.predictionTime
            else -> return ""
        }.formattedTime()
    val timeString =
        if (isFirst)
            stringResource(R.string.vehicle_prediction_time_first, vehicleType, predictionTime)
        else stringResource(R.string.vehicle_prediction_time_other, predictionTime)
    val description =
        if (trip is TripInstantDisplay.TimeWithStatus) "$timeString, ${trip.status}" else timeString
    return withLastTripSuffix(description, lastTrip)
}

@Composable
private fun predictedWithScheduleDescription(
    trip: TripInstantDisplay.TimeWithSchedule,
    isFirst: Boolean,
    vehicleType: String,
): String {
    val scheduleStatus =
        if (trip.predictionTime >= trip.scheduledTime)
            delayDescription(trip.scheduledTime, isFirst, vehicleType)
        else {
            val scheduledTime = trip.scheduledTime.formattedTime()
            if (isFirst)
                stringResource(
                    R.string.vehicle_prediction_schedule_status_early_first,
                    scheduledTime,
                    vehicleType,
                )
            else
                stringResource(
                    R.string.vehicle_prediction_schedule_status_early_other,
                    scheduledTime,
                    vehicleType,
                )
        }
    val predictionTime = trip.predictionTime.formattedTime()
    val actualArrival = stringResource(R.string.vehicle_prediction_actual_arrival, predictionTime)
    return withLastTripSuffix("$scheduleStatus, $actualArrival", trip.last)
}

@Composable
private fun scheduledMinutesDescription(
    trip: TripInstantDisplay.ScheduleMinutes,
    isFirst: Boolean,
    vehicleType: String,
): String {
    val description =
        when (val format = MinutesFormat.from(trip.minutes)) {
            is MinutesFormat.Hour ->
                if (isFirst)
                    stringResource(R.string.vehicle_schedule_hours_first, vehicleType, format.hours)
                else stringResource(R.string.vehicle_schedule_hours_other, format.hours)
            is MinutesFormat.HourMinute ->
                if (isFirst)
                    stringResource(
                        R.string.vehicle_schedule_hours_minutes_first,
                        vehicleType,
                        format.hours,
                        format.minutes,
                    )
                else
                    stringResource(
                        R.string.vehicle_schedule_hours_minutes_other,
                        format.hours,
                        format.minutes,
                    )
            is MinutesFormat.Minute ->
                if (isFirst)
                    stringResource(
                        R.string.vehicle_schedule_minutes_first,
                        vehicleType,
                        format.minutes,
                    )
                else stringResource(R.string.vehicle_schedule_minutes_other, format.minutes)
        }
    return withLastTripSuffix(description, trip.last)
}

@Composable
private fun scheduledTimeDescription(
    trip: TripInstantDisplay,
    isFirst: Boolean,
    vehicleType: String,
): String {
    val scheduledTime =
        when (trip) {
            is TripInstantDisplay.ScheduleTime -> trip.scheduledTime
            is TripInstantDisplay.ScheduleTimeWithStatusColumn -> trip.scheduledTime
            is TripInstantDisplay.ScheduleTimeWithStatusRow -> trip.scheduledTime
            else -> return ""
        }
    val status =
        when (trip) {
            is TripInstantDisplay.ScheduleTimeWithStatusColumn -> trip.status
            is TripInstantDisplay.ScheduleTimeWithStatusRow -> trip.status
            else -> null
        }
    val lastTrip =
        when (trip) {
            is TripInstantDisplay.ScheduleTime -> trip.last
            is TripInstantDisplay.ScheduleTimeWithStatusColumn -> trip.last
            else -> false
        }
    if (status in TripInstantDisplay.delayStatuses)
        return withLastTripSuffix(delayDescription(scheduledTime, isFirst, vehicleType), lastTrip)

    val timeString =
        if (isFirst)
            stringResource(
                R.string.vehicle_schedule_time_first,
                vehicleType,
                scheduledTime.formattedTime(),
            )
        else stringResource(R.string.vehicle_schedule_time_other, scheduledTime.formattedTime())
    val description = if (status != null) "$timeString, $status" else timeString
    return withLastTripSuffix(description, lastTrip)
}

@Composable
private fun withLastTripSuffix(description: String, last: Boolean): String {
    return if (last) "$description, ${stringResource(R.string.last_trip)}" else description
}
