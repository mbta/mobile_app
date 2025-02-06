package com.mbta.tid.mbta_app.android.component

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.model.RealtimePatterns

@Composable
fun TripStatus(predictions: RealtimePatterns.Format) {
    when (predictions) {
        is RealtimePatterns.Format.Some -> {
            if (predictions.trips.isNotEmpty()) {
                UpcomingTripView(
                    state = UpcomingTripViewState.Some(predictions.trips.first().format)
                )
            }
        }
        is RealtimePatterns.Format.Disruption -> {
            UpcomingTripView(state = UpcomingTripViewState.Disruption(predictions.alert.effect))
        }
        is RealtimePatterns.Format.NoTrips -> {
            UpcomingTripView(state = UpcomingTripViewState.NoTrips(predictions.noTripsFormat))
        }
        is RealtimePatterns.Format.Loading -> {
            UpcomingTripView(state = UpcomingTripViewState.Loading)
        }
    }
}
