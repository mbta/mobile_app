package com.mbta.tid.mbta_app.utils

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus

val LocalDateTime.serviceDate: LocalDate
    get() = if (this.hour >= 3) this.date else this.date.minus(DatePeriod(days = 1))
