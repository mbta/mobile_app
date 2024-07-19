package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.alert
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.TripInstantDisplay

@Composable
fun HeadsignRowView(
    headsign: String,
    predictions: RealtimePatterns.Format,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 74.dp)
            .background(color = MaterialTheme.colorScheme.background),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier
            .weight(1f)
            .padding(16.dp)) {
            Text(headsign)
        }
        Row(
            modifier = Modifier
                .weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (predictions) {
                is RealtimePatterns.Format.Some ->
                    Column {
                        for (prediction in predictions.trips) {
                            UpcomingTripView(UpcomingTripViewState.Some(prediction.format))
                        }
                    }

                is RealtimePatterns.Format.NoService ->
                    UpcomingTripView(UpcomingTripViewState.NoService(predictions.alert.effect))
                is RealtimePatterns.Format.None -> UpcomingTripView(UpcomingTripViewState.None)
                is RealtimePatterns.Format.Loading -> UpcomingTripView(UpcomingTripViewState.Loading)
            }
            Column(modifier = Modifier.padding(8.dp), ) {
                Icon(painterResource(id = R.drawable.baseline_chevron_right_24), contentDescription = "Arrow Right")
            }
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
                RealtimePatterns.Format.Some(
                    listOf(
                        RealtimePatterns.Format.Some.FormatWithId(
                            "1",
                            TripInstantDisplay.Minutes(5)
                        ),
                        RealtimePatterns.Format.Some.FormatWithId(
                            "2",
                            TripInstantDisplay.Minutes(15)
                        )
                    )
                )
            )
            HeadsignRowView("None", RealtimePatterns.Format.None)
            HeadsignRowView("Loading", RealtimePatterns.Format.Loading)
            HeadsignRowView(
                "No Service",
                RealtimePatterns.Format.NoService(alert { effect = Alert.Effect.Suspension })
            )
        }
    }
}
