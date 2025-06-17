package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.formatTime
import com.mbta.tid.mbta_app.model.TripInstantDisplay

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
            val min = this.minutes
            if (isFirst) stringResource(R.string.vehicle_schedule_minutes_first, vehicleType, min)
            else stringResource(R.string.vehicle_schedule_minutes_other, min)
        }
        is TripInstantDisplay.ScheduleTime -> {
            val time = formatTime(this.scheduledTime)
            if (isFirst) stringResource(R.string.vehicle_schedule_time_first, vehicleType, time)
            else stringResource(R.string.vehicle_schedule_time_other, time)
        }
        is TripInstantDisplay.Time,
        is TripInstantDisplay.TimeWithSchedule,
        is TripInstantDisplay.TimeWithStatus -> predictedTimeDescription(this, isFirst, vehicleType)
        else -> ""
    }

@Composable
private fun predictedTimeDescription(
    trip: TripInstantDisplay,
    isFirst: Boolean,
    vehicleType: String,
): String {
    val predictionTime =
        formatTime(
            when (trip) {
                is TripInstantDisplay.Time -> trip.predictionTime
                is TripInstantDisplay.TimeWithSchedule -> trip.predictionTime
                is TripInstantDisplay.TimeWithStatus -> trip.predictionTime
                else -> return ""
            }
        )
    return if (isFirst) {
        stringResource(R.string.vehicle_prediction_time_first, vehicleType, predictionTime)
    } else {
        stringResource(R.string.vehicle_prediction_time_other, predictionTime)
    }
}

@Composable
private fun predictedMinutesDescription(
    trip: TripInstantDisplay,
    isFirst: Boolean,
    vehicleType: String,
): String {
    val minutes =
        when (trip) {
            is TripInstantDisplay.Approaching -> 1
            is TripInstantDisplay.Minutes -> trip.minutes
            else -> return ""
        }
    return if (isFirst) {
        stringResource(R.string.vehicle_prediction_minutes_first, vehicleType, minutes)
    } else {
        stringResource(R.string.vehicle_prediction_minutes_other, minutes)
    }
}
