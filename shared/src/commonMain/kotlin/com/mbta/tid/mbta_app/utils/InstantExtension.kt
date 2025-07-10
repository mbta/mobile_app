package com.mbta.tid.mbta_app.utils

import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun Instant.toBostonTime() = this.toLocalDateTime(TimeZone.of("America/New_York"))

fun LocalDateTime.fromBostonTime() = this.toInstant(TimeZone.of("America/New_York"))

/** The time component becomes irrelevant after this coercion and should be ignored */
fun Instant.coerceInServiceDay(): Instant {
    val localTime = this.toBostonTime()
    val serviceDate = localTime.serviceDate
    if (localTime.date == serviceDate) return this
    return this.minus(24.hours)
}
