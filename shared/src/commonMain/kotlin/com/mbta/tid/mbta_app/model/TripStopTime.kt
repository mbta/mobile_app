package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.utils.EasternTimeInstant

internal interface TripStopTime : Comparable<TripStopTime> {
    val arrivalTime: EasternTimeInstant?
    val departureTime: EasternTimeInstant?

    val stopTime
        get() = arrivalTime ?: departureTime

    fun stopTimeAfter(now: EasternTimeInstant) =
        arrivalTime?.takeUnless { it < now } ?: departureTime

    /**
     * Is the current time between the arrival & departure times for this prediction (inclusive)?
     */
    fun hasArrivedButNotDeparted(now: EasternTimeInstant): Boolean {
        return (arrivalTime?.let { it <= now } ?: false) &&
            (departureTime?.let { it >= now } ?: false)
    }

    override fun compareTo(other: TripStopTime): Int =
        nullsLast<EasternTimeInstant>().compare(stopTime, other.stopTime)
}
