package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.mbta.tid.mbta_app.android.component.HeadsignRowView
import com.mbta.tid.mbta_app.model.PatternsByStop
import kotlinx.datetime.Instant

@Composable
fun NearbyStopView(
    patternsAtStop: PatternsByStop,
    now: Instant,
) {
    Text(text = patternsAtStop.stop.name, fontWeight = FontWeight.Bold)

    for (patternsByHeadsign in patternsAtStop.patternsByHeadsign) {
        HeadsignRowView(
            patternsByHeadsign.headsign,
            patternsByHeadsign.format(now),
        )
    }
}
