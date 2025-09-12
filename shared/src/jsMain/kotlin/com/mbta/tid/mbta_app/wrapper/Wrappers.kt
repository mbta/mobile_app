package com.mbta.tid.mbta_app.wrapper

import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteBranchSegment
import com.mbta.tid.mbta_app.model.RouteDetailsStopList
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.TileData
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.js.Date
import kotlin.time.toJSDate

internal fun List<Direction>.wrapped(): List<State.Direction> = map {
    State.Direction(it.name, it.destination, it.id)
}

internal fun List<TileData>.wrapped(): List<State.Tile> = map {
    State.Tile(it.id, it.upcoming.trip.id, it.headsign, it.formatted.wrapped())
}

internal fun UpcomingFormat.wrapped(): State.UpcomingFormat? =
    when (this) {
        is UpcomingFormat.Disruption ->
            State.UpcomingFormat.Disruption(effect = this.alert.effect.name)
        UpcomingFormat.Loading -> null
        is UpcomingFormat.NoTrips -> null
        is UpcomingFormat.Some -> this.trips.singleOrNull()?.format?.wrapped()
    }

internal fun TripInstantDisplay.wrapped(): State.UpcomingFormat? =
    when (this) {
        TripInstantDisplay.Approaching -> State.UpcomingFormat.Approaching
        TripInstantDisplay.Arriving -> State.UpcomingFormat.Arriving
        TripInstantDisplay.Boarding -> State.UpcomingFormat.Boarding
        is TripInstantDisplay.Cancelled ->
            State.UpcomingFormat.Cancelled(this.scheduledTime.wrapped())
        TripInstantDisplay.Hidden -> State.UpcomingFormat.Hidden
        is TripInstantDisplay.Minutes -> State.UpcomingFormat.Minutes(this.minutes)
        TripInstantDisplay.Now -> State.UpcomingFormat.Now
        is TripInstantDisplay.Overridden -> State.UpcomingFormat.Overridden(this.text)
        is TripInstantDisplay.ScheduleMinutes -> State.UpcomingFormat.ScheduleMinutes(this.minutes)
        is TripInstantDisplay.ScheduleTime ->
            State.UpcomingFormat.ScheduleTime(this.scheduledTime.wrapped())
        is TripInstantDisplay.ScheduleTimeWithStatusColumn ->
            State.UpcomingFormat.ScheduleTimeWithStatus(this.scheduledTime.wrapped(), this.status)
        is TripInstantDisplay.ScheduleTimeWithStatusRow ->
            State.UpcomingFormat.ScheduleTimeWithStatus(this.scheduledTime.wrapped(), this.status)
        is TripInstantDisplay.Skipped -> State.UpcomingFormat.Skipped(this.scheduledTime?.wrapped())
        is TripInstantDisplay.Time -> State.UpcomingFormat.Time(this.predictionTime.wrapped())
        is TripInstantDisplay.TimeWithSchedule ->
            State.UpcomingFormat.TimeWithSchedule(
                this.predictionTime.wrapped(),
                this.scheduledTime.wrapped(),
            )
        is TripInstantDisplay.TimeWithStatus ->
            State.UpcomingFormat.TimeWithStatus(this.predictionTime.wrapped(), this.status)
    }

internal fun EasternTimeInstant.wrapped(): Date =
    this.instantWhichIPromiseNotToDisplayInTheWrongTimeZone.toJSDate()

internal fun Vehicle.wrapped(global: GlobalResponse?): State.Vehicle =
    State.Vehicle(
        this.stopId,
        global?.getStop(this.stopId)?.name,
        when (this.currentStatus) {
            Vehicle.CurrentStatus.IncomingAt -> State.Vehicle.Status.IncomingAt
            Vehicle.CurrentStatus.StoppedAt -> State.Vehicle.Status.StoppedAt
            Vehicle.CurrentStatus.InTransitTo -> State.Vehicle.Status.InTransitTo
        },
    )

internal fun TripDetailsStopList.TargetSplit.wrapped(
    trip: Trip,
    now: EasternTimeInstant,
    route: Route,
): State.StopList =
    State.StopList(
        this.firstStop?.wrapped(trip, now, route),
        this.collapsedStops?.mapNotNull { it.wrapped(trip, now, route) },
        this.targetStop?.wrapped(trip, now, route),
        this.followingStops.mapNotNull { it.wrapped(trip, now, route) },
    )

internal fun TripDetailsStopList.Entry.wrapped(
    trip: Trip,
    now: EasternTimeInstant,
    route: Route,
): State.StopList.Entry? =
    State.StopList.Entry(
        this.prediction?.id ?: this.schedule?.id ?: "${this.stop.id}-${this.stopSequence}",
        this.stop.id,
        this.stop.name,
        this.format(trip, now, route)?.wrapped(),
    )

internal fun RouteDetailsStopList.wrapped(route: Route?) =
    RouteDetails.State(route?.color ?: "ba75c7", this.segments.map { it.wrapped() })

internal fun RouteDetailsStopList.Segment.wrapped() =
    RouteDetails.State.Segment(
        this.stops.map { it.wrapped() },
        this.isTypical,
        if (!this.isTypical) this.twistedConnections()?.map { it.wrapped() } else null,
    )

internal fun RouteDetailsStopList.Entry.wrapped() =
    RouteDetails.State.Stop(
        this.stop.name,
        this.stopLane.wrapped(),
        this.stickConnections.map { it.wrapped() },
    )

internal fun Pair<RouteBranchSegment.StickConnection, Boolean>.wrapped() =
    RouteDetails.State.TwistedConnection(this.first.wrapped(), this.second)

internal fun RouteBranchSegment.StickConnection.wrapped() = RouteDetails.State.StickConnection(this)

internal fun RouteBranchSegment.Lane.wrapped() =
    when (this) {
        RouteBranchSegment.Lane.Left -> RouteDetails.State.Lane.Left
        RouteBranchSegment.Lane.Center -> RouteDetails.State.Lane.Center
        RouteBranchSegment.Lane.Right -> RouteDetails.State.Lane.Right
    }
