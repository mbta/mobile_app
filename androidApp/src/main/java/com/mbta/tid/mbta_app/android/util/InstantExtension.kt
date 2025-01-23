package com.mbta.tid.mbta_app.android.util

import java.util.Date
import kotlinx.datetime.Instant

/** Converts this [Instant] into a [Date]. For use with [android.icu.text.DateFormat]. */
fun Instant.toJavaDate() = Date(toEpochMilliseconds())
