package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
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
    Text(text = patternsAtStop.stop.name, fontWeight = FontWeight.Bold)

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
    }
}
