package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.route
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.trip
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.UpcomingTrip

@Composable
fun HeadsignRowView(
    headsign: String,
    predictions: UpcomingFormat,
    modifier: Modifier = Modifier,
    pillDecoration: PillDecoration? = null,
) {
    PredictionRowView(
        predictions = predictions,
        modifier = modifier,
        pillDecoration = pillDecoration,
    ) {
        Text(headsign, style = Typography.bodySemibold, modifier = Modifier.placeholderIfLoading())
    }
}

@Preview
@Composable
fun HeadsignRowViewPreview() {
    MyApplicationTheme {
        Column(Modifier.background(MaterialTheme.colorScheme.background)) {
            HeadsignRowView(
                "Some",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            UpcomingTrip(trip { id = "1" }),
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Minutes(5, false),
                            lastTrip = false,
                        ),
                        UpcomingFormat.Some.FormattedTrip(
                            UpcomingTrip(trip { id = "2" }),
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Minutes(15, false),
                            lastTrip = false,
                        ),
                    ),
                    secondaryAlert = null,
                ),
            )
            HeadsignRowView(
                "Some with Alert",
                UpcomingFormat.Some(
                    listOf(
                        UpcomingFormat.Some.FormattedTrip(
                            UpcomingTrip(trip { id = "1" }),
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Minutes(5, false),
                            lastTrip = false,
                        ),
                        UpcomingFormat.Some.FormattedTrip(
                            UpcomingTrip(trip { id = "2" }),
                            RouteType.LIGHT_RAIL,
                            TripInstantDisplay.Minutes(15, false),
                            lastTrip = false,
                        ),
                    ),
                    secondaryAlert = UpcomingFormat.SecondaryAlert("alert-large-bus-issue"),
                ),
            )
            HeadsignRowView(
                "None",
                UpcomingFormat.NoTrips(UpcomingFormat.NoTripsFormat.PredictionsUnavailable),
            )
            HeadsignRowView(
                "None with Alert",
                UpcomingFormat.NoTrips(
                    UpcomingFormat.NoTripsFormat.PredictionsUnavailable,
                    secondaryAlert = UpcomingFormat.SecondaryAlert("alert-large-bus-issue"),
                ),
            )
            HeadsignRowView(
                "Decorated None with Alert",
                UpcomingFormat.NoTrips(
                    UpcomingFormat.NoTripsFormat.PredictionsUnavailable,
                    secondaryAlert = UpcomingFormat.SecondaryAlert("alert-large-green-issue"),
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
                    ),
            )
            HeadsignRowView("Loading", UpcomingFormat.Loading)
            HeadsignRowView(
                "No Service",
                UpcomingFormat.Disruption(
                    alert { effect = Alert.Effect.Suspension },
                    MapStopRoute.ORANGE,
                ),
            )
        }
    }
}
