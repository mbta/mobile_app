package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.android.component.legacyRouteCard.LineCard
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopsAssociated
import kotlinx.datetime.Instant

@Composable
fun NearbyLineView(
    nearbyLine: StopsAssociated.WithLine,
    pinned: Boolean,
    onPin: (String) -> Unit,
    now: Instant,
    onOpenStopDetails: (String, StopDetailsFilter?) -> Unit,
    showElevatorAccessibility: Boolean = false
) {
    LineCard(nearbyLine.line, nearbyLine.routes, pinned, onPin) {
        for (patternsAtStop in nearbyLine.patternsByStop) {
            NearbyStopView(
                patternsAtStop,
                condenseHeadsignPredictions = nearbyLine.condensePredictions,
                now,
                pinned,
                onOpenStopDetails,
                showElevatorAccessibility
            )
        }
    }
}
