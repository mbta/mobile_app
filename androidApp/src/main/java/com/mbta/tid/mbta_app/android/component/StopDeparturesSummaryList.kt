package com.mbta.tid.mbta_app.android.component

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import kotlinx.datetime.Instant

@Composable
fun StopDeparturesSummaryList(
    patternsAtStop: PatternsByStop,
    condenseHeadsignPredictions: Boolean,
    now: Instant,
    context: TripInstantDisplay.Context,
) {
    for (patterns in patternsAtStop.patterns) {
        when (patterns) {
            is RealtimePatterns.ByHeadsign -> {
                HeadsignRowView(
                    patterns.headsign,
                    patterns.format(now, if (condenseHeadsignPredictions) 1 else 2, context),
                    pillDecoration =
                        if (patternsAtStop.line != null) PillDecoration.OnRow(patterns.route)
                        else null
                )
            }
            is RealtimePatterns.ByDirection -> {
                DirectionRowView(
                    patterns.direction,
                    patterns.format(now, context),
                    pillDecoration = PillDecoration.OnPrediction(patterns.routesByTrip)
                )
            }
        }
        if (patterns != patternsAtStop.patterns.last()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surface)
        }
    }
}