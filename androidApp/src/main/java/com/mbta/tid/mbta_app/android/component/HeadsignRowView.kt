package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.alert
import com.mbta.tid.mbta_app.model.PatternsByHeadsign
import com.mbta.tid.mbta_app.model.UpcomingTrip

@Composable
fun HeadsignRowView(
    headsign: String,
    predictions: PatternsByHeadsign.Format,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(headsign)
        when (predictions) {
            is PatternsByHeadsign.Format.Some ->
                Row {
                    for (prediction in predictions.trips) {
                        UpcomingTripView(UpcomingTripViewState.Some(prediction.format))
                    }
                }
            is PatternsByHeadsign.Format.NoService ->
                UpcomingTripView(UpcomingTripViewState.NoService(predictions.alert.effect))
            is PatternsByHeadsign.Format.None -> UpcomingTripView(UpcomingTripViewState.None)
            is PatternsByHeadsign.Format.Loading -> UpcomingTripView(UpcomingTripViewState.Loading)
        }
    }
}

@Preview
@Composable
fun HeadsignRowViewPreview() {
    MyApplicationTheme {
        Column {
            HeadsignRowView(
                "Some",
                PatternsByHeadsign.Format.Some(
                    listOf(
                        PatternsByHeadsign.Format.Some.FormatWithId(
                            "1",
                            UpcomingTrip.Format.Minutes(5)
                        ),
                        PatternsByHeadsign.Format.Some.FormatWithId(
                            "2",
                            UpcomingTrip.Format.Minutes(15)
                        )
                    )
                )
            )
            HeadsignRowView("None", PatternsByHeadsign.Format.None)
            HeadsignRowView("Loading", PatternsByHeadsign.Format.Loading)
            HeadsignRowView(
                "No Service",
                PatternsByHeadsign.Format.NoService(alert { effect = Alert.Effect.Suspension })
            )
        }
    }
}