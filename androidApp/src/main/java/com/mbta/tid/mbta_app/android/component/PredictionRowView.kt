package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.generated.drawableByName
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.Route

sealed interface PillDecoration {
    data class OnRow(val route: Route) : PillDecoration

    data class OnPrediction(val routesByTrip: Map<String, Route>) : PillDecoration
}

@Composable
fun PredictionRowView(
    predictions: RealtimePatterns.Format,
    modifier: Modifier = Modifier,
    pillDecoration: PillDecoration? = null,
    destination: @Composable () -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(8.dp)
            .background(color = MaterialTheme.colorScheme.background),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        predictions.secondaryAlert?.let { secondaryAlert ->
            Image(
                painterResource(drawableByName(secondaryAlert.iconName)),
                secondaryAlert.alertEffect.name
            )
        }
        if (pillDecoration is PillDecoration.OnRow) {
            RoutePill(pillDecoration.route, line = null, RoutePillType.Flex)
        }

        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) { destination() }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (predictions) {
                    is RealtimePatterns.Format.Some ->
                        for (prediction in predictions.trips) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UpcomingTripView(UpcomingTripViewState.Some(prediction.format))
                                if (pillDecoration is PillDecoration.OnPrediction) {
                                    val route = pillDecoration.routesByTrip.getValue(prediction.id)
                                    RoutePill(
                                        route,
                                        null,
                                        RoutePillType.Flex,
                                        modifier = Modifier.scale(0.75f).padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                    is RealtimePatterns.Format.NoService ->
                        UpcomingTripView(UpcomingTripViewState.NoService(predictions.alert.effect))
                    is RealtimePatterns.Format.None -> UpcomingTripView(UpcomingTripViewState.None)
                    is RealtimePatterns.Format.Loading ->
                        UpcomingTripView(UpcomingTripViewState.Loading)
                }
            }

            Column(
                modifier = Modifier.padding(8.dp).widthIn(max = 8.dp),
            ) {
                Icon(
                    painterResource(id = R.drawable.baseline_chevron_right_24),
                    contentDescription = "Arrow Right",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
