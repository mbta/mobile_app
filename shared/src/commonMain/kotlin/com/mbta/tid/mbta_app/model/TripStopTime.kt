package com.mbta.tid.mbta_app.model

import kotlin.time.Instant

interface TripStopTime : Comparable<TripStopTime> {
    val arrivalTime: Instant?
    val departureTime: Instant?

    val stopTime
        get() = arrivalTime ?: departureTime

    fun stopTimeAfter(now: Instant) = arrivalTime?.takeUnless { it < now } ?: departureTime

    /**
     * Is the current time between the arrival & departure times for this prediction (inclusive)?
     */
    fun hasArrivedButNotDeparted(now: Instant): Boolean {
        return (arrivalTime?.let { it <= now } ?: false) &&
            (departureTime?.let { it >= now } ?: false)
    }

    override fun compareTo(other: TripStopTime): Int =
        nullsLast<Instant>().compare(stopTime, other.stopTime)
}
