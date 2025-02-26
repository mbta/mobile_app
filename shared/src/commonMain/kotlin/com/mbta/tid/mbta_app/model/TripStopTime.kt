package com.mbta.tid.mbta_app.model

import kotlinx.datetime.Instant

interface TripStopTime : Comparable<TripStopTime> {
    val arrivalTime: Instant?
    val departureTime: Instant?

    val stopTime
        get() = arrivalTime ?: departureTime

    fun stopTimeAfter(now: Instant) = arrivalTime?.takeUnless { it < now } ?: departureTime

    override fun compareTo(other: TripStopTime): Int =
        nullsLast<Instant>().compare(stopTime, other.stopTime)
}
