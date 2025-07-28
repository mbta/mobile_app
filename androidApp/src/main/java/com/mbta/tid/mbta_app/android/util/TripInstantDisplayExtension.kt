package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.formatTime
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.utils.MinutesFormat
import kotlin.time.Instant

@Composable
fun TripInstantDisplay.contentDescription(isFirst: Boolean, vehicleType: String): String =
    when (this) {
        is TripInstantDisplay.Approaching,
        is TripInstantDisplay.Minutes -> predictedMinutesDescription(this, isFirst, vehicleType)
        is TripInstantDisplay.Arriving,
        is TripInstantDisplay.Now ->
            if (isFirst) stringResource(R.string.vehicle_arriving_first, vehicleType)
            else stringResource(R.string.vehicle_arriving_other)
        is TripInstantDisplay.Boarding ->
            if (isFirst) stringResource(R.string.vehicle_boarding_first, vehicleType)
            else stringResource(R.string.vehicle_boarding_other)
        is TripInstantDisplay.Cancelled -> {
            val time = formatTime(this.scheduledTime)
            if (isFirst) stringResource(R.string.vehicle_cancelled_first, vehicleType, time)
            else stringResource(R.string.vehicle_cancelled_other, time)
        }
        is TripInstantDisplay.ScheduleMinutes -> {
            val format = MinutesFormat.from(this.minutes)
            when (format) {
                is MinutesFormat.Hour ->
                    if (isFirst)
                        stringResource(
                            R.string.vehicle_schedule_hours_first,
                            vehicleType,
                            format.hours,
                        )
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
        }
        is TripInstantDisplay.ScheduleTime -> {
            val time = formatTime(this.scheduledTime)
            if (isFirst) stringResource(R.string.vehicle_schedule_time_first, vehicleType, time)
            else stringResource(R.string.vehicle_schedule_time_other, time)
        }
        is TripInstantDisplay.Time,
        is TripInstantDisplay.TimeWithStatus -> predictedTimeDescription(this, isFirst, vehicleType)
        is TripInstantDisplay.TimeWithSchedule ->
            predictedWithScheduleDescription(this, isFirst, vehicleType)
        else -> ""
    }

@Composable
private fun delayDescription(
    scheduledInstant: Instant,
    isFirst: Boolean,
    vehicleType: String,
): String {
    val scheduledTime = formatTime(scheduledInstant)
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
    return if (isFirst)
        when (format) {
            is MinutesFormat.Hour ->
                stringResource(R.string.vehicle_prediction_hours_first, vehicleType, format.hours)
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
}

@Composable
private fun predictedTimeDescription(
    trip: TripInstantDisplay,
    isFirst: Boolean,
    vehicleType: String,
): String {
    if (
        trip is TripInstantDisplay.TimeWithStatus && trip.status in TripInstantDisplay.delayStatuses
    )
        return delayDescription(trip.predictionTime, isFirst, vehicleType)

    val predictionTime =
        formatTime(
            when (trip) {
                is TripInstantDisplay.Time -> trip.predictionTime
                is TripInstantDisplay.TimeWithStatus -> trip.predictionTime
                else -> return ""
            }
        )
    val timeString =
        if (isFirst)
            stringResource(R.string.vehicle_prediction_time_first, vehicleType, predictionTime)
        else stringResource(R.string.vehicle_prediction_time_other, predictionTime)
    return if (trip is TripInstantDisplay.TimeWithStatus) "$timeString, ${trip.status}"
    else timeString
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
            val scheduledTime = formatTime(trip.scheduledTime)
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
    val predictionTime = formatTime(trip.predictionTime)
    val actualArrival = stringResource(R.string.vehicle_prediction_actual_arrival, predictionTime)
    return "$scheduleStatus, $actualArrival"
}
