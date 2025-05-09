package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.generated.drawableByName
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.containsWrappableText
import com.mbta.tid.mbta_app.android.util.modifiers.DestinationPredictionBalance
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.alert
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.UpcomingTrip

sealed interface PillDecoration {
    data class OnRow(val route: Route) : PillDecoration

    data class OnPrediction(val routesByTrip: Map<String, Route>) : PillDecoration
}

@Composable
fun PredictionRowView(
    predictions: UpcomingFormat,
    modifier: Modifier = Modifier,
    pillDecoration: PillDecoration? = null,
    destination: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pillDecoration is PillDecoration.OnRow) {
            RoutePill(
                pillDecoration.route,
                line = null,
                RoutePillType.Flex,
                modifier =
                    if (predictions.secondaryAlert == null) Modifier.padding(end = 8.dp)
                    else Modifier,
            )
        }
        predictions.secondaryAlert?.let { secondaryAlert ->
            Image(
                painterResource(drawableByName(secondaryAlert.iconName)),
                stringResource(R.string.alert),
                modifier = Modifier.placeholderIfLoading().padding(end = 8.dp),
            )
        }

        Column(modifier = DestinationPredictionBalance.destinationWidth()) { destination() }
        Row(
            modifier =
                DestinationPredictionBalance.predictionWidth(predictions.containsWrappableText()),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProvideTextStyle(value = Typography.callout) {
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    when (predictions) {
                        is UpcomingFormat.Some ->
                            predictions.trips.mapIndexed { index, prediction ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    UpcomingTripView(
                                        UpcomingTripViewState.Some(prediction.format),
                                        modifier = Modifier.weight(1f, fill = false),
                                        isFirst = index == 0,
                                        isOnly = index == 0 && predictions.trips.count() == 1,
                                    )
                                    if (pillDecoration is PillDecoration.OnPrediction) {
                                        val route: Route? =
                                            pillDecoration.routesByTrip[prediction.id]

                                        if (route != null) {
                                            RoutePill(
                                                route,
                                                null,
                                                RoutePillType.Flex,
                                                modifier =
                                                    Modifier.wrapContentHeight(
                                                            Alignment.CenterVertically
                                                        )
                                                        .scale(0.75f),
                                            )
                                        }
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
                            UpcomingTripView(
                                (UpcomingTripViewState.NoTrips(predictions.noTripsFormat))
                            )
                        is UpcomingFormat.Loading -> UpcomingTripView(UpcomingTripViewState.Loading)
                    }
                }
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

    MyApplicationTheme {
        Column(Modifier.background(MaterialTheme.colorScheme.background)) {
            PredictionRowView(
                predictions =
                    UpcomingFormat.Some(
                        listOf(
                            UpcomingFormat.Some.FormattedTrip(
                                UpcomingTrip(trip),
                                RouteType.LIGHT_RAIL,
                                TripInstantDisplay.Boarding,
                            )
                        ),
                        null,
                    ),
                pillDecoration = PillDecoration.OnPrediction(mapOf(trip.id to greenB)),
            ) {
                Text("Longer Destination than That")
            }

            PredictionRowView(
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            UpcomingTrip(trip),
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Overridden("Stopped 10 stops away"),
                        )
                    ),
                    null,
                ),
                pillDecoration = PillDecoration.OnRow(greenB),
            ) {
                Text("Destination")
            }

            PredictionRowView(
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            UpcomingTrip(trip),
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Overridden("Stopped 10 stops away"),
                        )
                    ),
                    null,
                ),
                pillDecoration = PillDecoration.OnPrediction(mapOf(trip.id to greenB)),
            ) {
                Text("Destination")
            }

            PredictionRowView(
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            UpcomingTrip(trip),
                            RouteType.BUS,
                            TripInstantDisplay.ScheduleMinutes(6),
                        ),
                        UpcomingFormat.Some.FormattedTrip(
                            UpcomingTrip(trip),
                            RouteType.BUS,
                            TripInstantDisplay.ScheduleMinutes(15),
                        ),
                    ),
                    null,
                )
            ) {
                Text("Destination")
            }

            PredictionRowView(
                UpcomingFormat.Disruption(
                    alert { effect = Alert.Effect.Detour },
                    MapStopRoute.GREEN,
                )
            ) {
                Text("Destination")
            }
        }
    }
}
