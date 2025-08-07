package com.mbta.tid.mbta_app.android.util

import com.mbta.tid.mbta_app.android.component.UpcomingTripViewState
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat

fun UpcomingFormat.containsWrappableText() =
    when (this) {
        is UpcomingFormat.Some -> this.trips.any { it.format.containsWrappableText() }
        is UpcomingFormat.NoTrips -> true
        is UpcomingFormat.Disruption -> false
        UpcomingFormat.Loading -> false
    }

fun UpcomingTripViewState.containsWrappableText() =
    when (this) {
        is UpcomingTripViewState.Some -> this.trip.containsWrappableText()
        is UpcomingTripViewState.NoTrips -> true
        is UpcomingTripViewState.Disruption -> false
        UpcomingTripViewState.Loading -> false
    }

fun TripInstantDisplay.containsWrappableText() =
    when (this) {
        is TripInstantDisplay.Overridden -> true
        TripInstantDisplay.Hidden -> false
        TripInstantDisplay.Boarding -> false
        TripInstantDisplay.Arriving -> false
        TripInstantDisplay.Approaching -> false
        TripInstantDisplay.Now -> false
        is TripInstantDisplay.Time -> false
        is TripInstantDisplay.TimeWithStatus -> true
        is TripInstantDisplay.TimeWithSchedule -> false
        is TripInstantDisplay.Minutes -> false
        is TripInstantDisplay.ScheduleTime -> false
        is TripInstantDisplay.ScheduleTimeWithStatusColumn -> true
        is TripInstantDisplay.ScheduleTimeWithStatusRow -> true
        is TripInstantDisplay.ScheduleMinutes -> false
        is TripInstantDisplay.Skipped -> false
        is TripInstantDisplay.Cancelled -> false
    }
