package com.mbta.tid.mbta_app.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Instant.toBostonTime() = this.toLocalDateTime(TimeZone.of("America/New_York"))
