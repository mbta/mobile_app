package com.mbta.tid.mbta_app.android.util

import android.icu.text.DateFormat
import android.icu.util.GregorianCalendar
import android.icu.util.TimeZone
import com.mbta.tid.mbta_app.utils.EasternTimeInstant

private val easternTime by lazy { TimeZone.getTimeZone("America/New_York") }

private fun DateFormat.inEasternTime() = this.apply { timeZone = easternTime }

private val formatHourMinute by lazy {
    DateFormat.getInstanceForSkeleton(DateFormat.HOUR_MINUTE).inEasternTime()
}
private val formatMonthDay by lazy {
    DateFormat.getInstanceForSkeleton(DateFormat.MONTH_DAY).inEasternTime()
}
private val formatShortDateShortTime by lazy {
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).inEasternTime()
}
private val formatWeekday by lazy {
    DateFormat.getInstanceForSkeleton(DateFormat.WEEKDAY).inEasternTime()
}
private val formatWeekdayAndDate by lazy {
    DateFormat.getInstanceForSkeleton(DateFormat.WEEKDAY + DateFormat.ABBR_MONTH + DateFormat.DAY)
        .inEasternTime()
}

private fun EasternTimeInstant.formatWith(format: DateFormat): String {
    val calendar =
        GregorianCalendar(
            local.year,
            local.month.ordinal,
            local.day,
            local.hour,
            local.minute,
            local.second,
        )
    calendar.timeZone = easternTime
    return format.format(calendar)
}

/** Formats the time component in the form of "10:00 AM" */
fun EasternTimeInstant.formattedTime(): String = this.formatWith(formatHourMinute)

/** Formats the date and time components in the form of “7/29/25 9:18 AM” */
fun EasternTimeInstant.formattedShortDateShortTime(): String =
    this.formatWith(formatShortDateShortTime)

/** Converts the [EasternTimeInstant] to its service day and formats it in the form of "Monday" */
fun EasternTimeInstant.formattedServiceDay(): String =
    this.coerceInServiceDay().formatWith(formatWeekday)

/**
 * Converts the [EasternTimeInstant] to its service day and formats it in the form of "Monday, Jan
 * 1"
 */
fun EasternTimeInstant.formattedServiceDayAndDate(): String =
    this.coerceInServiceDay().formatWith(formatWeekdayAndDate)

/**
 * Converts the [EasternTimeInstant] to its service day and formats it in the form of "January 1"
 */
fun EasternTimeInstant.formattedServiceDate(): String =
    this.coerceInServiceDay().formatWith(formatMonthDay)
