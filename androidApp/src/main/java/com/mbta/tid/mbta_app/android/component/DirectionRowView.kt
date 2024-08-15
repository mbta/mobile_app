package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingTrip
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock

@Composable
fun DirectionRowView(
    direction: Direction,
    predictions: RealtimePatterns.Format,
    modifier: Modifier = Modifier,
    pillDecoration: PillDecoration? = null
) {
    PredictionRowView(
        predictions = predictions,
        modifier = modifier,
        pillDecoration = pillDecoration
    ) {
        DirectionLabel(direction)
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
                    RealtimePatterns.Format.Some(
                        trips =
                            listOf(
                                RealtimePatterns.Format.Some.FormatWithId(
                                    trip = UpcomingTrip(trip = trip1, prediction = prediction1),
                                    now = now,
                                    context = TripInstantDisplay.Context.NearbyTransit
                                ),
                                RealtimePatterns.Format.Some.FormatWithId(
                                    trip = UpcomingTrip(trip = trip2, prediction = prediction2),
                                    now = now,
                                    context = TripInstantDisplay.Context.NearbyTransit
                                ),
                            )
                    )
            )
            DirectionRowView(
                direction = Direction(name = "North", destination = "None", id = 0),
                predictions = RealtimePatterns.Format.None
            )
            DirectionRowView(
                direction = Direction(name = "South", destination = "Loading", id = 1),
                predictions = RealtimePatterns.Format.Loading
            )
            DirectionRowView(
                direction = Direction(name = "East", destination = "No Service", id = 1),
                predictions =
                    RealtimePatterns.Format.NoService(
                        alert =
                            ObjectCollectionBuilder.Single.alert {
                                effect = Alert.Effect.Suspension
                            }
                    )
            )
        }
    }
}
