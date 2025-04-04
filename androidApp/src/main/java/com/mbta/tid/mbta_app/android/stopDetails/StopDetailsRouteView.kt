package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.android.component.legacyRouteCard.LegacyRouteCard
import com.mbta.tid.mbta_app.android.component.legacyRouteCard.LineCard
import com.mbta.tid.mbta_app.android.component.legacyRouteCard.StopDeparturesSummaryList
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import kotlinx.datetime.Instant

@Composable
fun StopDetailsRouteView(
    patternsByStop: PatternsByStop,
    now: Instant,
    pinned: Boolean,
    onPin: (String) -> Unit,
    updateStopFilter: (StopDetailsFilter?) -> Unit
) {
    fun onTappedPatterns(patterns: RealtimePatterns) {
        updateStopFilter(StopDetailsFilter(patternsByStop.routeIdentifier, patterns.directionId()))
    }
    if (patternsByStop.line != null) {
        LineCard(patternsByStop.line!!, patternsByStop.routes, pinned, onPin) {
            StopDeparturesSummaryList(
                patternsByStop,
                condenseHeadsignPredictions = patternsByStop.routes.size > 1,
                now,
                TripInstantDisplay.Context.StopDetailsUnfiltered,
                pinned,
                onClick = ::onTappedPatterns
            )
        }
    } else {
        val route = patternsByStop.routes.firstOrNull() ?: return
        LegacyRouteCard(route, pinned, onPin) {
            StopDeparturesSummaryList(
                patternsByStop,
                false,
                now,
                TripInstantDisplay.Context.StopDetailsUnfiltered,
                pinned,
                onClick = ::onTappedPatterns
            )
        }
    }
}
