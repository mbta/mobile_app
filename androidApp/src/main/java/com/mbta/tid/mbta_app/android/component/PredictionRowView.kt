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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.generated.drawableByName
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripInstantDisplay

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
                stringResource(R.string.alert),
                modifier = Modifier.placeholderIfLoading()
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
            ProvideTextStyle(value = Typography.callout) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when (predictions) {
                        is RealtimePatterns.Format.Some ->
                            predictions.trips.mapIndexed { index, prediction ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UpcomingTripView(
                                        UpcomingTripViewState.Some(prediction.format),
                                        modifier = Modifier.weight(1f, fill = false),
                                        isFirst = index == 0,
                                        isOnly = index == 0 && predictions.trips.count() == 1
                                    )
                                    if (pillDecoration is PillDecoration.OnPrediction) {
                                        val route =
                                            pillDecoration.routesByTrip.getValue(prediction.id)
                                        RoutePill(
                                            route,
                                            null,
                                            RoutePillType.Flex,
                                            modifier =
                                                Modifier.wrapContentHeight(
                                                        Alignment.CenterVertically
                                                    )
                                                    .scale(0.75f)
                                        )
                                    }
                                }
                            }
                        is RealtimePatterns.Format.Disruption ->
                            UpcomingTripView(
                                UpcomingTripViewState.Disruption(
                                    predictions.alert.effect,
                                    iconName = predictions.iconName
                                )
                            )
                        is RealtimePatterns.Format.NoTrips ->
                            UpcomingTripView(
                                (UpcomingTripViewState.NoTrips(predictions.noTripsFormat))
                            )
                        is RealtimePatterns.Format.Loading ->
                            UpcomingTripView(UpcomingTripViewState.Loading)
                    }
                }
                //
            }

            Column(
                modifier = Modifier.padding(8.dp).widthIn(max = 8.dp),
            ) {
                Icon(
                    painterResource(id = R.drawable.baseline_chevron_right_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Preview
@Composable
private fun PredictionRowViewPreview() {
    val objects = ObjectCollectionBuilder()
    val green = objects.line()
    val greenB =
        objects.route {
            lineId = green.id
            color = "00843D"
            textColor = "FFFFFF"
            shortName = "B"
            longName = "Green Line B"
            type = RouteType.LIGHT_RAIL
        }
    val trip = objects.trip()

    Column {
        PredictionRowView(
            predictions =
                RealtimePatterns.Format.Some(
                    listOf(
                        RealtimePatterns.Format.Some.FormatWithId(
                            trip.id,
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Boarding
                        )
                    ),
                    null
                ),
            pillDecoration = PillDecoration.OnPrediction(mapOf(trip.id to greenB))
        ) {
            Text("Destination")
        }

        PredictionRowView(
            RealtimePatterns.Format.Some(
                listOf(
                    RealtimePatterns.Format.Some.FormatWithId(
                        trip.id,
                        RouteType.LIGHT_RAIL,
                        TripInstantDisplay.Overridden("Stopped 10 stops away")
                    )
                ),
                null
            ),
            pillDecoration = PillDecoration.OnRow(greenB)
        ) {
            Text("Destination")
        }

        PredictionRowView(
            RealtimePatterns.Format.Some(
                listOf(
                    RealtimePatterns.Format.Some.FormatWithId(
                        trip.id,
                        RouteType.LIGHT_RAIL,
                        TripInstantDisplay.Overridden("Stopped 10 stops away")
                    )
                ),
                null
            ),
            pillDecoration = PillDecoration.OnPrediction(mapOf(trip.id to greenB))
        ) {
            Text("Destination")
        }

        PredictionRowView(
            RealtimePatterns.Format.Some(
                listOf(
                    RealtimePatterns.Format.Some.FormatWithId(
                        "a",
                        RouteType.BUS,
                        TripInstantDisplay.ScheduleMinutes(6)
                    ),
                    RealtimePatterns.Format.Some.FormatWithId(
                        "b",
                        RouteType.BUS,
                        TripInstantDisplay.ScheduleMinutes(15)
                    ),
                ),
                null
            )
        ) {
            Text("Destination")
        }
    }
}
