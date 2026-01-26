package com.mbta.tid.mbta_app.android.util

import android.icu.text.DateFormat
import android.icu.util.Calendar
import android.icu.util.GregorianCalendar
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

private val formatWeekdayAbbr by lazy { DateFormat.getInstanceForSkeleton(DateFormat.ABBR_WEEKDAY) }
private val formatWeekday by lazy { DateFormat.getInstanceForSkeleton(DateFormat.WEEKDAY) }

// unfortunately, Calendar.DAY_OF_WEEK uses Sun-Sat instead of the ISO Mon-Sun
// fortunately, converting 7 to 1, 1 to 2, etc is the easiest operation available
val DayOfWeek.numberSundayFirst
    get() = isoDayNumber % 7 + 1

private fun DayOfWeek.calendarOnDay() =
    GregorianCalendar().apply { set(Calendar.DAY_OF_WEEK, numberSundayFirst) }

fun DayOfWeek.formattedAbbr() = formatWeekdayAbbr.format(calendarOnDay())

fun DayOfWeek.formattedFull() = formatWeekday.format(calendarOnDay())
