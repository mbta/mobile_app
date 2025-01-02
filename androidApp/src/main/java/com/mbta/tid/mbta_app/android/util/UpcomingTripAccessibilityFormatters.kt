package com.mbta.tid.mbta_app.android.util

import android.content.Context
import com.mbta.tid.mbta_app.android.R

/** Helper functions for formatting accessibility text to be read aloud by TalkBack. */
class UpcomingTripAccessibilityFormatters {

    companion object {

        fun arrivingLabel(context: Context, isFirst: Boolean, vehicleType: String): String {
            return if (isFirst)
                context.resources.getString(R.string.vehicle_arriving_first, vehicleType)
            else context.resources.getString(R.string.vehicle_arriving_other)
        }

        fun boardingLabel(context: Context, isFirst: Boolean, vehicleType: String): String {
            return if (isFirst)
                context.resources.getString(R.string.vehicle_boarding_first, vehicleType)
            else context.resources.getString(R.string.vehicle_boarding_other)
        }

        fun cancelledLabel(
            context: Context,
            scheduledTime: String,
            isFirst: Boolean,
            vehicleType: String
        ): String {
            return if (isFirst)
                context.resources.getString(
                    R.string.vehicle_cancelled_first,
                    vehicleType,
                    scheduledTime
                )
            else context.resources.getString(R.string.vehicle_cancelled_other, scheduledTime)
        }

        fun scheduledMinutesLabel(
            context: Context,
            minutes: Int,
            isFirst: Boolean,
            vehicleType: String
        ): String {
            return if (isFirst) {
                context.resources.getString(
                    R.string.vehicle_schedule_minutes_first,
                    vehicleType,
                    minutes
                )
            } else {
                context.resources.getString(R.string.vehicle_schedule_minutes_other, minutes)
            }
        }

        fun scheduledTimeLabel(
            context: Context,
            time: String,
            isFirst: Boolean,
            vehicleType: String
        ): String {
            return if (isFirst) {
                context.resources.getString(R.string.vehicle_schedule_time_first, vehicleType, time)
            } else {
                context.resources.getString(R.string.vehicle_schedule_time_other, time)
            }
        }

        fun predictedMinutesLabel(
            context: Context,
            minutes: Int,
            isFirst: Boolean,
            vehicleType: String
        ): String {
            return if (isFirst) {
                context.resources.getString(
                    R.string.vehicle_prediction_minutes_first,
                    vehicleType,
                    minutes
                )
            } else {
                context.resources.getString(R.string.vehicle_prediction_minutes_other, minutes)
            }
        }

        fun predictedTimeLabel(
            context: Context,
            time: String,
            isFirst: Boolean,
            vehicleType: String
        ): String {
            return if (isFirst) {
                context.resources.getString(
                    R.string.vehicle_prediction_time_first,
                    vehicleType,
                    time
                )
            } else {
                context.resources.getString(R.string.vehicle_prediction_time_other, time)
            }
        }
    }
}
