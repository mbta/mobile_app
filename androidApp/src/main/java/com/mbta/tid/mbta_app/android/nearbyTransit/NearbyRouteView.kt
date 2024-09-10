package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.android.component.RouteCard
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopsAssociated
import kotlinx.datetime.Instant

@Composable
fun NearbyRouteView(
    nearbyRoute: StopsAssociated.WithRoute,
    pinned: Boolean,
    onPin: (String) -> Unit,
    now: Instant,
    onOpenStopDetails: (String, StopDetailsFilter?) -> Unit,
) {
    RouteCard(nearbyRoute.route, pinned, onPin) {
        for (patternsAtStop in nearbyRoute.patternsByStop) {
            NearbyStopView(patternsAtStop, now = now, onOpenStopDetails = onOpenStopDetails)
        }
    }
}
