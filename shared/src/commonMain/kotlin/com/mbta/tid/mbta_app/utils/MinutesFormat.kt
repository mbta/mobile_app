package com.mbta.tid.mbta_app.utils

public sealed class MinutesFormat {
    public companion object {
        public fun from(minutes: Int): MinutesFormat {
            val hours = minutes.floorDiv(60)
            val remainingMinutes = minutes - (hours * 60)
            return if (hours >= 1)
                if (remainingMinutes == 0) Hour(hours) else HourMinute(hours, remainingMinutes)
            else Minute(minutes)
        }
    }

    public data class Hour internal constructor(val hours: Int) : MinutesFormat()

    public data class HourMinute internal constructor(val hours: Int, val minutes: Int) :
        MinutesFormat()

    public data class Minute internal constructor(val minutes: Int) : MinutesFormat()
}
