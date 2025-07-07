package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.UpcomingTrip
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@Composable
fun DirectionRowView(
    direction: Direction,
    predictions: UpcomingFormat,
    modifier: Modifier = Modifier,
    pillDecoration: PillDecoration? = null,
) {
    PredictionRowView(
        predictions = predictions,
        modifier = modifier,
        pillDecoration = pillDecoration,
    ) {
        DirectionLabel(direction, pillDecoration = pillDecoration)
    }
}

@Preview
@Composable
private fun DirectionRowViewPreview() {
    MyApplicationTheme {
        Column {
            val now = Clock.System.now()
            val objects = ObjectCollectionBuilder()
            val trip1 = objects.trip()
            val prediction1 =
                objects.prediction {
                    trip = trip1
                    departureTime = now + 5.minutes
                }
            val trip2 = objects.trip()
            val prediction2 =
                objects.prediction {
                    trip = trip2
                    departureTime = now + 12.minutes
                }
            DirectionRowView(
                Direction("West", "Some", 0),
                predictions =
                    UpcomingFormat.Some(
                        trips =
                            listOf(
                                UpcomingFormat.Some.FormattedTrip(
                                    trip = UpcomingTrip(trip = trip1, prediction = prediction1),
                                    routeType = RouteType.LIGHT_RAIL,
                                    now = now,
                                    context = TripInstantDisplay.Context.NearbyTransit,
                                ),
                                UpcomingFormat.Some.FormattedTrip(
                                    trip = UpcomingTrip(trip = trip2, prediction = prediction2),
                                    routeType = RouteType.LIGHT_RAIL,
                                    now = now,
                                    context = TripInstantDisplay.Context.NearbyTransit,
                                ),
                            ),
                        secondaryAlert = null,
                    ),
            )
            DirectionRowView(
                direction = Direction(name = "North", destination = "None", id = 0),
                predictions =
                    UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.PredictionsUnavailable),
            )
            DirectionRowView(
                direction = Direction(name = "South", destination = "Loading", id = 1),
                predictions = UpcomingFormat.Loading,
            )
            DirectionRowView(
                direction = Direction(name = "East", destination = "No Service", id = 1),
                predictions =
                    UpcomingFormat.Disruption(
                        alert =
                            ObjectCollectionBuilder.Single.alert {
                                effect = Alert.Effect.Suspension
                            },
                        mapStopRoute = MapStopRoute.GREEN,
                    ),
            )
        }
    }
}
