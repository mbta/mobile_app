package com.mbta.tid.mbta_app.utils

sealed class MinutesFormat {
    companion object {
        fun from(minutes: Int): MinutesFormat {
            val hours = minutes.floorDiv(60)
            val remainingMinutes = minutes - (hours * 60)
            return if (hours >= 1)
                if (remainingMinutes == 0) Hour(hours) else HourMinute(hours, remainingMinutes)
            else Minute(minutes)
        }
    }

    data class Hour(val hours: Int) : MinutesFormat()

    data class HourMinute(val hours: Int, val minutes: Int) : MinutesFormat()

    data class Minute(val minutes: Int) : MinutesFormat()
}
