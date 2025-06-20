package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.android.component.UpcomingTripView
import com.mbta.tid.mbta_app.android.component.UpcomingTripViewState
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.model.UpcomingFormat

@Composable
fun TripStatus(predictions: UpcomingFormat) {
    when (predictions) {
        is UpcomingFormat.Some ->
            when (val trip = predictions.trips.firstOrNull()) {
                null -> {}
                else ->
                    UpcomingTripView(
                        UpcomingTripViewState.Some(trip.format),
                        routeType = trip.routeType,
                    )
            }
        is UpcomingFormat.Disruption ->
            UpcomingTripView(
                UpcomingTripViewState.Disruption(
                    FormattedAlert(predictions.alert),
                    iconName = predictions.iconName,
                )
            )
        is UpcomingFormat.NoTrips ->
            UpcomingTripView(UpcomingTripViewState.NoTrips(predictions.noTripsFormat))
        UpcomingFormat.Loading -> UpcomingTripView(UpcomingTripViewState.Loading)
    }
}
