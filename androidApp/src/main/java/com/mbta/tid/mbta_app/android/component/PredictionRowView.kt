package com.mbta.tid.mbta_app.android.component

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.RealtimePatterns

@Composable
fun PredictionRowView(
    predictions: RealtimePatterns.Format,
    modifier: Modifier = Modifier,
    destination: @Composable () -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 74.dp)
            .background(color = MaterialTheme.colorScheme.background),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(16.dp)) { destination() }
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
                            UpcomingTripView(UpcomingTripViewState.Some(prediction.format))
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
