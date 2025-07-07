package com.mbta.tid.mbta_app.android.util

import android.icu.text.DateFormat
import com.mbta.tid.mbta_app.utils.coerceInServiceDay
import java.util.Date
import kotlin.time.Instant

/** Converts this [Instant] into a [Date]. For use with [android.icu.text.DateFormat]. */
fun Instant.toJavaDate() = Date(toEpochMilliseconds())

/** Formats the [Instant] using [DateFormat.getInstanceForSkeleton] */
fun Instant.dateFormat(skeleton: String): String =
    DateFormat.getInstanceForSkeleton(skeleton).format(this.toJavaDate())

/** Formats the [Instant] using [DateFormat.getDateTimeInstance] */
fun Instant.dateFormat(dateStyle: Int, timeStyle: Int): String =
    DateFormat.getDateTimeInstance(dateStyle, timeStyle).format(this.toJavaDate())

/** Formats the time component in the form of "10:00 AM" */
fun Instant.formattedTime(): String = this.dateFormat(DateFormat.HOUR_MINUTE)

/** Converts the [Instant] to its service day and formats it in the form of "Monday" */
fun Instant.formattedServiceDay(): String = this.coerceInServiceDay().dateFormat(DateFormat.WEEKDAY)

/** Converts the [Instant] to its service day and formats it in the form of "Monday, Jan 1" */
fun Instant.formattedServiceDayAndDate(): String =
    this.coerceInServiceDay()
        .dateFormat(DateFormat.WEEKDAY + DateFormat.ABBR_MONTH + DateFormat.DAY)

/** Converts the [Instant] to its service day and formats it in the form of "January 1" */
fun Instant.formattedServiceDate(): String =
    this.coerceInServiceDay().dateFormat(DateFormat.MONTH_DAY)
