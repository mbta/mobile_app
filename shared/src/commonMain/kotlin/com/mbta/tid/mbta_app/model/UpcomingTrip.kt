package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.resolveParentId

/**
 * Conceptually, an [UpcomingTrip] represents a trip's future arrival and departure at a stop.
 *
 * The time may not be in the future, depending on when stale predictions are removed and how long
 * ago the schedule was fetched. The trip may not arrive at a stop if that stop is the first and may
 * not depart from a stop if that stop is the last.
 *
 * The trip might also neither arrive nor depart if the stop is skipped or the trip is dropped. For
 * this reason, a prediction that exists but has null times should overwrite scheduled times.
 */
public data class UpcomingTrip
@DefaultArgumentInterop.Enabled
constructor(
    val trip: Trip,
    val schedule: Schedule? = null,
    val prediction: Prediction? = null,
    // The prediction stop is the stop associated with the stopId contained in the prediction,
    // it can be a child stop with specific boarding information, like the track number
    val predictionStop: Stop? = null,
    val vehicle: Vehicle? = null,
) : Comparable<UpcomingTrip> {

    public constructor(
        trip: Trip,
        prediction: Prediction?,
        predictionStop: Stop? = null,
    ) : this(trip, null, prediction, predictionStop, null)

    val id: String = "${trip.id}-${prediction?.stopSequence ?: schedule?.stopSequence}"

    internal val time =
        if (
            prediction != null &&
                prediction.scheduleRelationship != Prediction.ScheduleRelationship.Cancelled &&
                !(prediction.stopTime == null && prediction.status != null)
        ) {
            prediction.stopTime
        } else {
            schedule?.stopTime
        }

    internal val stopId: String? = run {
        // don't check that they match since prediction may be physical stop ID and schedule logical
        prediction?.stopId ?: schedule?.stopId
    }

    val stopSequence: Int? = run {
        // don’t check that they match since cancelled trip predictions may include extra stops
        schedule?.stopSequence ?: prediction?.stopSequence
    }

    val headsign: String
        get() = schedule?.stopHeadsign ?: trip.headsign

    val isCancelled: Boolean
        get() =
            schedule?.stopTime != null &&
                prediction?.scheduleRelationship == Prediction.ScheduleRelationship.Cancelled

    internal val trackNumber: String? =
        if (predictionStop?.shouldShowTrackNumber == true) predictionStop.platformCode else null

    /** Checks if a trip has a time */
    internal fun isUpcoming() = time != null

    /**
     * Checks if a trip exists in the near future, or the recent past if the vehicle has not yet
     * left this stop or a custom status string is still set.
     */
    internal fun isUpcomingWithin(
        currentTime: EasternTimeInstant,
        cutoffTime: EasternTimeInstant,
    ): Boolean =
        time != null &&
            time < cutoffTime &&
            (time >= currentTime ||
                (prediction != null &&
                    (prediction.stopId == vehicle?.stopId || prediction.status != null)))

    override fun compareTo(other: UpcomingTrip): Int =
        nullsLast<EasternTimeInstant>().compare(time, other.time)

    /**
     * Checks whether this upcoming trip will depart its station or only arrive there.
     *
     * If a trip will neither arrive nor depart (e.g. trips with no schedule that have been
     * cancelled), this function will return `null`. Returning `true` would hide headsigns with no
     * schedule and predictions exclusively for dropped trips, which may happen during suspensions
     * and would be incorrect. Returning `false` would show headsigns with added trips even if those
     * trips have been cancelled, which would be incorrect.
     */
    internal fun isArrivalOnly(): Boolean? {
        val hasArrival =
            if (schedule != null) {
                schedule.dropOffType != Schedule.StopEdgeType.Unavailable
            } else {
                prediction?.arrivalTime != null
            }
        val hasDeparture =
            if (schedule != null) {
                schedule.pickUpType != Schedule.StopEdgeType.Unavailable
            } else {
                prediction?.departureTime != null
            }
        return if (!hasArrival && !hasDeparture) {
            null
        } else !hasDeparture
    }

    internal fun display(
        now: EasternTimeInstant,
        routeType: RouteType?,
        context: TripInstantDisplay.Context,
        lastTrip: Boolean,
    ) = TripInstantDisplay.from(prediction, schedule, vehicle, routeType, now, context, lastTrip)

    internal fun format(
        now: EasternTimeInstant,
        route: Route,
        context: TripInstantDisplay.Context,
        lastTrip: Boolean,
    ) =
        format(
            now,
            route.type,
            context,
            route.type.isSubway() || route.id in silverRoutes,
            lastTrip,
        )

    internal fun format(
        now: EasternTimeInstant,
        routeType: RouteType,
        context: TripInstantDisplay.Context,
        hideSchedule: Boolean,
        lastTrip: Boolean,
    ): UpcomingFormat.Some.FormattedTrip? {
        return UpcomingFormat.Some.FormattedTrip(this, routeType, now, context, lastTrip)
            .takeUnless {
                it.format is TripInstantDisplay.Hidden ||
                    it.format is TripInstantDisplay.Skipped ||
                    // API best practices call for hiding scheduled times on subway
                    (hideSchedule &&
                        (it.format is TripInstantDisplay.ScheduleTime ||
                            it.format is TripInstantDisplay.ScheduleMinutes))
            }
    }

    internal companion object {
        /**
         * Gets the list of [UpcomingTrip]s from the given [schedules], [predictions] and
         * [vehicles]. Matches by trip ID, parent stop ID, and stop sequence.
         */
        fun tripsFromData(
            stops: Map<String, Stop>,
            schedules: List<Schedule>,
            predictions: List<Prediction>,
            trips: Map<String, Trip>,
            vehicles: Map<String, Vehicle>,
            filterAtTime: EasternTimeInstant,
        ): List<UpcomingTrip> {
            data class UpcomingTripKey(
                val tripId: String,
                val rootStopId: String?,
                val stopSequence: Int?,
            ) {
                constructor(
                    schedule: Schedule
                ) : this(
                    schedule.tripId,
                    stops.resolveParentId(schedule.stopId),
                    schedule.stopSequence,
                )

                constructor(
                    prediction: Prediction
                ) : this(
                    prediction.tripId,
                    stops.resolveParentId(prediction.stopId),
                    prediction.stopSequence,
                )

                fun dropStopSequenceIfCancelled(cancelledTrips: Set<String>): UpcomingTripKey =
                    if (tripId in cancelledTrips) this.copy(stopSequence = null) else this
            }

            // apparently, one of the differences between Skipped and Cancelled is that Cancelled
            // only ever applies to the entire trip. predictions for cancelled bus trips sometimes
            // have bad stop sequence values for reasons, so we want to ignore the stop sequence for
            // trips with cancelled predictions
            val cancelledTrips =
                predictions
                    .mapNotNull { prediction ->
                        prediction.tripId.takeIf {
                            prediction.scheduleRelationship ==
                                Prediction.ScheduleRelationship.Cancelled
                        }
                    }
                    .toSet()
            val schedulesMap =
                schedules.associateBy {
                    UpcomingTripKey(it).dropStopSequenceIfCancelled(cancelledTrips)
                }
            val predictionsMap =
                predictions.associateBy {
                    UpcomingTripKey(it).dropStopSequenceIfCancelled(cancelledTrips)
                }

            val keys = schedulesMap.keys + predictionsMap.keys

            return keys
                .mapNotNull { key ->
                    val prediction = predictionsMap[key]
                    UpcomingTrip(
                        trips[key.tripId] ?: return@mapNotNull null,
                        schedulesMap[key],
                        prediction,
                        stops[prediction?.stopId],
                        predictionsMap[key]?.let { vehicles[it.vehicleId] },
                    )
                }
                .sorted()
                .filter { upcomingTrip ->
                    if (upcomingTrip.prediction != null) return@filter true
                    val scheduleTime = upcomingTrip.schedule?.stopTime ?: return@filter true
                    scheduleTime >= filterAtTime
                }
        }

        fun formatUpcomingTrip(
            now: EasternTimeInstant,
            upcomingTrip: UpcomingTrip,
            routeType: RouteType,
            context: TripInstantDisplay.Context,
            lastTrip: Boolean,
        ) =
            formatUpcomingTrip(
                now,
                upcomingTrip,
                routeType,
                context,
                routeType.isSubway(),
                lastTrip,
            )

        fun formatUpcomingTrip(
            now: EasternTimeInstant,
            upcomingTrip: UpcomingTrip,
            routeType: RouteType,
            context: TripInstantDisplay.Context,
            isSubway: Boolean,
            lastTrip: Boolean,
        ): UpcomingFormat.Some.FormattedTrip? {
            return UpcomingFormat.Some.FormattedTrip(
                    upcomingTrip,
                    routeType,
                    now,
                    context,
                    lastTrip,
                )
                .takeUnless {
                    it.format is TripInstantDisplay.Hidden ||
                        it.format is TripInstantDisplay.Skipped ||
                        // API best practices call for hiding scheduled times on subway
                        // Unless it's the last trip, only on filtered stop details
                        (isSubway &&
                            (!lastTrip ||
                                context != TripInstantDisplay.Context.StopDetailsFiltered) &&
                            (it.format is TripInstantDisplay.ScheduleTime ||
                                it.format is TripInstantDisplay.ScheduleMinutes))
                }
        }
    }
}

internal fun List<UpcomingTrip>.isArrivalOnly(): Boolean {
    /**
     * Checks if this upcoming trips end at this stop, i.e. all trips are arrival-only.
     *
     * Criteria:
     * - At least one trip is scheduled as arrival-only
     * - No trips are scheduled or predicted with a departure
     */
    // Intermediate variable set because kotlin can't smart cast properties with open getters
    return this.mapTo(mutableSetOf()) { it.isArrivalOnly() }
        .let { upcomingTripsArrivalOnly ->
            upcomingTripsArrivalOnly.contains(true) && !upcomingTripsArrivalOnly.contains(false)
        }
}

/**
 * Associate each upcoming trip with its format. Omits any upcoming trip that shouldn't be shown.
 */
internal fun List<UpcomingTrip>.withFormat(
    now: EasternTimeInstant,
    route: Route,
    context: TripInstantDisplay.Context,
): List<UpcomingFormat.Some.FormattedTrip> {
    val formattedTrips =
        this.mapNotNull {
            val last =
                (route.type.isSubway() && it.prediction?.lastTrip == true) ||
                    (!route.type.isSubway() && this.last() == it)
            it.format(now, route, context, last)
        }

    val lastNonCancelledTrip =
        formattedTrips.lastOrNull { it.format !is TripInstantDisplay.Cancelled }
    // If no trips are tagged as the last trip, this is subway and we shouldn't change anything
    if (formattedTrips.none { it.lastTrip } || lastNonCancelledTrip?.lastTrip ?: true) {
        return formattedTrips
    }

    // If the last non-cancelled trip is not flagged as the last trip, we need to set the last
    // trip flag on the cancelled trip to false, and set it to true on the actual last trip
    return formattedTrips.mapNotNull {
        if (it == lastNonCancelledTrip) {
            return@mapNotNull it.trip.format(now, route, context, true)
        } else if (it.lastTrip) {
            return@mapNotNull it.trip.format(now, route, context, false)
        } else {
            return@mapNotNull it
        }
    }
}
