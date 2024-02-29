package com.mbta.tid.mbta_app.util

import kotlinx.datetime.Instant
import kotlinx.datetime.toNSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterShortStyle

object TimeFormatter {
    val short = NSDateFormatter()

    init {
        short.timeStyle = NSDateFormatterShortStyle
    }
}

actual fun formatShortTime(instant: Instant): String {
    val date = instant.toNSDate()
    return TimeFormatter.short.stringFromDate(date)
}
