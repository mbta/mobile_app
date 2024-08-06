package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DirectionRowView
import com.mbta.tid.mbta_app.android.component.HeadsignRowView
import com.mbta.tid.mbta_app.android.component.PillDecoration
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import kotlinx.datetime.Instant

@Composable
fun NearbyStopView(
    patternsAtStop: PatternsByStop,
    condenseHeadsignPredictions: Boolean = false,
    now: Instant,
) {
    Row(modifier = Modifier.background(colorResource(id = R.color.fill2)).fillMaxWidth()) {
        Text(
            text = patternsAtStop.stop.name,
            modifier = Modifier.padding(top = 11.dp, bottom = 11.dp, start = 16.dp, end = 8.dp),
            style = MaterialTheme.typography.headlineSmall
        )
    }

    for (patterns in patternsAtStop.patterns) {
        when (patterns) {
            is RealtimePatterns.ByHeadsign -> {
                HeadsignRowView(
                    patterns.headsign,
                    patterns.format(
                        now,
                        if (condenseHeadsignPredictions) 1 else 2,
                        TripInstantDisplay.Context.NearbyTransit
                    ),
                    pillDecoration =
                        if (patternsAtStop.line != null) PillDecoration.OnRow(patterns.route)
                        else null
                )
            }
            is RealtimePatterns.ByDirection -> {
                DirectionRowView(
                    patterns.direction,
                    patterns.format(now, TripInstantDisplay.Context.NearbyTransit),
                    pillDecoration = PillDecoration.OnPrediction(patterns.routesByTrip)
                )
            }
        }
        if (patterns != patternsAtStop.patterns.last()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surface)
        }
    }
}
