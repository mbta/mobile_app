package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.utils.EasternTimeInstant

public interface TripStopTime : Comparable<TripStopTime> {
    public val arrivalTime: EasternTimeInstant?
    public val departureTime: EasternTimeInstant?

    public val stopTime: EasternTimeInstant?
        get() = arrivalTime ?: departureTime

    public fun stopTimeAfter(now: EasternTimeInstant): EasternTimeInstant? =
        arrivalTime?.takeUnless { it < now } ?: departureTime

    /**
     * Is the current time between the arrival & departure times for this prediction (inclusive)?
     */
    public fun hasArrivedButNotDeparted(now: EasternTimeInstant): Boolean {
        return (arrivalTime?.let { it <= now } ?: false) &&
            (departureTime?.let { it >= now } ?: false)
    }

    override fun compareTo(other: TripStopTime): Int =
        nullsLast<EasternTimeInstant>().compare(stopTime, other.stopTime)
}
