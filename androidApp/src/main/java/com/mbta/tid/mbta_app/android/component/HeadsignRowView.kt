package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.route
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripInstantDisplay

@Composable
fun HeadsignRowView(
    headsign: String,
    predictions: RealtimePatterns.Format,
    modifier: Modifier = Modifier,
    pillDecoration: PillDecoration? = null,
) {
    PredictionRowView(
        predictions = predictions,
        modifier = modifier,
        pillDecoration = pillDecoration
    ) {
        Text(
            headsign,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview
@Composable
fun HeadsignRowViewPreview() {
    MyApplicationTheme {
        Column {
            HeadsignRowView(
                "Some",
                RealtimePatterns.Format.Some(
                    listOf(
                        RealtimePatterns.Format.Some.FormatWithId(
                            "1",
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Minutes(5)
                        ),
                        RealtimePatterns.Format.Some.FormatWithId(
                            "2",
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Minutes(15)
                        )
                    ),
                    secondaryAlert = null
                )
            )
            HeadsignRowView(
                "Some with Alert",
                RealtimePatterns.Format.Some(
                    listOf(
                        RealtimePatterns.Format.Some.FormatWithId(
                            "1",
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Minutes(5)
                        ),
                        RealtimePatterns.Format.Some.FormatWithId(
                            "2",
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Minutes(15)
                        )
                    ),
                    secondaryAlert = RealtimePatterns.Format.SecondaryAlert("alert-large-bus-issue")
                )
            )
            HeadsignRowView(
                "None",
                RealtimePatterns.Format.NoTrips(
                    RealtimePatterns.NoTripsFormat.PredictionsUnavailable
                )
            )
            HeadsignRowView(
                "None with Alert",
                RealtimePatterns.Format.NoTrips(
                    RealtimePatterns.NoTripsFormat.PredictionsUnavailable,
                    secondaryAlert = RealtimePatterns.Format.SecondaryAlert("alert-large-bus-issue")
                )
            )
            HeadsignRowView(
                "Decorated None with Alert",
                RealtimePatterns.Format.NoTrips(
                    RealtimePatterns.NoTripsFormat.PredictionsUnavailable,
                    secondaryAlert =
                        RealtimePatterns.Format.SecondaryAlert("alert-large-green-issue")
                ),
                pillDecoration =
                    PillDecoration.OnRow(
                        route {
                            id = "Green-E"
                            shortName = "E"
                            color = "00843D"
                            type = RouteType.LIGHT_RAIL
                            textColor = "FFFFFF"
                            longName = "Green Line E"
                        }
                    )
            )
            HeadsignRowView("Loading", RealtimePatterns.Format.Loading)
            HeadsignRowView(
                "No Service",
                RealtimePatterns.Format.Disruption(alert { effect = Alert.Effect.Suspension })
            )
        }
    }
}
