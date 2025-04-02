package com.mbta.tid.mbta_app.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun Instant.toBostonTime() = this.toLocalDateTime(TimeZone.of("America/New_York"))

fun LocalDateTime.fromBostonTime() = this.toInstant(TimeZone.of("America/New_York"))
