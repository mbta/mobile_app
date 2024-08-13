package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.android.component.LineCard
import com.mbta.tid.mbta_app.android.component.RouteCard
import com.mbta.tid.mbta_app.android.component.StopDeparturesSummaryList
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import kotlinx.datetime.Instant

@Composable
fun StopDetailsRouteView(
    patternsByStop: PatternsByStop,
    now: Instant,
    pinned: Boolean,
    onPin: (String) -> Unit
) {
    if (patternsByStop.line != null) {
        LineCard(patternsByStop.line!!, patternsByStop.routes, pinned, onPin) {
            StopDeparturesSummaryList(
                patternsByStop,
                condenseHeadsignPredictions = patternsByStop.routes.size > 1,
                now,
                TripInstantDisplay.Context.StopDetailsUnfiltered
            ) {}
        }
    } else {
        val route = patternsByStop.routes.firstOrNull() ?: return
        RouteCard(route, pinned, onPin) {
            StopDeparturesSummaryList(
                patternsByStop,
                false,
                now,
                TripInstantDisplay.Context.StopDetailsUnfiltered
            ) {}
        }
    }
}
