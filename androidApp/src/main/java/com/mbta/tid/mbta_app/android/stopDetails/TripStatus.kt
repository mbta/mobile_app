package com.mbta.tid.mbta_app.android.stopDetails

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.mbta.tid.mbta_app.android.component.DebugView
import com.mbta.tid.mbta_app.android.component.UpcomingTripView
import com.mbta.tid.mbta_app.android.component.UpcomingTripViewState
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.UpcomingFormat
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

@SuppressLint("DefaultLocale")
@Composable
fun TripStatus(predictions: UpcomingFormat) {
    when (predictions) {
        is UpcomingFormat.Some ->
            when (val trip = predictions.trips.firstOrNull()) {
                null -> {}
                else ->
                    Column {
                        UpcomingTripView(
                            UpcomingTripViewState.Some(trip.format),
                            routeType = trip.routeType,
                            hideDisruptionIcon = true,
                        )
                        trip.debugTime?.let {
                            DebugView(
                                withSocketInfo = false,
                                content = {
                                    val now by timer(updateInterval = 1.seconds)
                                    val diff = it.minus(now)
                                    val countdown = diff.toComponents { minutes, seconds, _ ->
                                        String.format("%02dm %02ds", abs(minutes), abs(seconds))
                                    }
                                    Text(text = "${if (it < now) "-" else ""}${countdown}")
                                },
                            )
                        }
                    }
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
