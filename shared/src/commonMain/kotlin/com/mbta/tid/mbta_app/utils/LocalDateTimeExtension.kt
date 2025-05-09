package com.mbta.tid.mbta_app.utils

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus

enum class ServiceDateRounding {
    FORWARDS,
    BACKWARDS,
}

val LocalDateTime.serviceDate: LocalDate
    get() = if (this.hour >= 3) this.date else this.date.minus(DatePeriod(days = 1))

// Service end times will be set to 3:00 in Alerts UI, which means that a LocalDateTime representing
// the end of service on one date will think that it belongs to the next service day, this allows
// you to specify if you want to round forward or backward in that case.
fun LocalDateTime.serviceDate(rounding: ServiceDateRounding): LocalDate {
    return if (rounding == ServiceDateRounding.BACKWARDS && hour == 3 && minute == 0)
        serviceDate.minus(DatePeriod(days = 1))
    else serviceDate
}
