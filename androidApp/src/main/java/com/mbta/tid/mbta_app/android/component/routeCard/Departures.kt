package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DirectionLabel
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.HeadsignRowView
import com.mbta.tid.mbta_app.android.component.PredictionRowView
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import kotlinx.datetime.Instant
import org.koin.compose.koinInject

@Composable
fun Departures(
    stopData: RouteCardData.RouteStopData,
    cardData: RouteCardData,
    now: Instant,
    analytics: Analytics = koinInject(),
    onClick: (RouteCardData.Leaf) -> Unit,
) {
    val localContext = LocalContext.current

    stopData.data.withIndex().forEach { (index, leaf) ->
        fun analyticsTappedDeparture(predictions: UpcomingFormat) {
            val noTrips =
                when (predictions) {
                    is UpcomingFormat.NoTrips -> predictions.noTripsFormat
                    else -> null
                }
            analytics.tappedDeparture(
                cardData.lineOrRoute.id,
                stopData.stop.id,
                // TODO: Handle pinning
                false,
                leaf.alertsHere.isNotEmpty(),
                cardData.lineOrRoute.type,
                noTrips
            )
        }

        val formatted =
            leaf.format(
                now,
                cardData.lineOrRoute.sortRoute,
                3,
                when (cardData.context) {
                    RouteCardData.Context.NearbyTransit -> TripInstantDisplay.Context.NearbyTransit
                    RouteCardData.Context.StopDetailsFiltered ->
                        TripInstantDisplay.Context.StopDetailsFiltered
                    RouteCardData.Context.StopDetailsUnfiltered ->
                        TripInstantDisplay.Context.StopDetailsUnfiltered
                }
            )
        val direction = stopData.directions.first { it.id == leaf.directionId }

        val clickModifier =
            Modifier.clickable(
                onClickLabel = localContext.getString(R.string.open_for_more_arrivals),
                onClick = {
                    onClick(leaf)
                    analyticsTappedDeparture(formatted)
                }
            )

        Column(
            modifier = clickModifier,
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            when (formatted) {
                is UpcomingFormat.Some -> {
                    DirectionLabel(
                        direction,
                        Modifier.padding(start = 8.dp, top = 8.dp),
                        showDestination = false
                    )
                    formatted.trips.forEach {
                        HeadsignRowView(
                            it.trip.trip.headsign,
                            UpcomingFormat.Some(it, formatted.secondaryAlert),
                            pillDecoration = null
                        )
                    }
                }
                else ->
                    PredictionRowView(predictions = formatted, pillDecoration = null) {
                        DirectionLabel(direction)
                    }
            }
            if (index < stopData.data.lastIndex) {
                HaloSeparator()
            }
        }
    }
}
