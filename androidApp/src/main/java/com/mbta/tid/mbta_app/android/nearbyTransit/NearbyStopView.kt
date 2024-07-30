package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.component.HeadsignRowView
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import kotlinx.datetime.Instant

@Composable
fun NearbyStopView(
    patternsAtStop: PatternsByStop,
    now: Instant,
) {
    Text(text = patternsAtStop.stop.name, modifier = Modifier.padding(16.dp))

    for (patterns in patternsAtStop.patterns) {
        when (patterns) {
            is RealtimePatterns.ByHeadsign -> {
                HeadsignRowView(
                    patterns.headsign,
                    patterns.format(now, TripInstantDisplay.Context.NearbyTransit),
                )
            }
            is RealtimePatterns.ByDirection -> {}
        }
        if (patterns != patternsAtStop.patterns.last()) {
            HorizontalDivider()
        }
    }
}
