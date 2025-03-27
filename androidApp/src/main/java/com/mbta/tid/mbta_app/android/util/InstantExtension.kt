package com.mbta.tid.mbta_app.android.util

import android.icu.text.DateFormat
import java.util.Date
import kotlinx.datetime.Instant

/** Converts this [Instant] into a [Date]. For use with [android.icu.text.DateFormat]. */
fun Instant.toJavaDate() = Date(toEpochMilliseconds())

fun Instant.dateFormat(skeleton: String): String =
    DateFormat.getInstanceForSkeleton(skeleton).format(this.toJavaDate())

fun Instant.dateFormat(dateStyle: Int, timeStyle: Int): String =
    DateFormat.getDateTimeInstance(dateStyle, timeStyle).format(this.toJavaDate())

fun Instant.formattedTime(): String = this.dateFormat(DateFormat.HOUR_MINUTE)

fun Instant.formattedDay(): String = this.dateFormat(DateFormat.WEEKDAY)

fun Instant.formattedDayAndDate(): String =
    this.dateFormat(DateFormat.WEEKDAY + DateFormat.ABBR_MONTH + DateFormat.DAY)

fun Instant.formattedDate(): String = this.dateFormat(DateFormat.MONTH_DAY)
