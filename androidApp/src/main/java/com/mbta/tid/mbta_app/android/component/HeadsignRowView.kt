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
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.TripInstantDisplay

@Composable
fun HeadsignRowView(
    headsign: String,
    predictions: RealtimePatterns.Format,
    modifier: Modifier = Modifier
) {
    PredictionRowView(predictions = predictions, modifier = modifier) {
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
