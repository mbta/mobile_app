package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.android.component.UpcomingTripView
import com.mbta.tid.mbta_app.android.component.UpcomingTripViewState
import com.mbta.tid.mbta_app.model.RealtimePatterns

@Composable
fun TripStatus(predictions: RealtimePatterns.Format) {
    when (predictions) {
        is RealtimePatterns.Format.Some ->
            when (val trip = predictions.trips.firstOrNull()) {
                null -> {}
                else -> UpcomingTripView(UpcomingTripViewState.Some(trip.format))
            }
        is RealtimePatterns.Format.Disruption ->
            UpcomingTripView(UpcomingTripViewState.Disruption(predictions.alert.effect))
        is RealtimePatterns.Format.NoTrips ->
            UpcomingTripView(UpcomingTripViewState.NoTrips(predictions.noTripsFormat))
        RealtimePatterns.Format.Loading -> UpcomingTripView(UpcomingTripViewState.Loading)
    }
}
