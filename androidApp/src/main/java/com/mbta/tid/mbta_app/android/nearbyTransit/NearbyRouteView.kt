package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.android.component.RoutePillSection
import com.mbta.tid.mbta_app.model.StopAssociatedRoute
import kotlinx.datetime.Instant

@Composable
fun NearbyRouteView(
    nearbyRoute: StopAssociatedRoute,
    now: Instant,
) {
    RoutePillSection(nearbyRoute.route) {
        for (patternsAtStop in nearbyRoute.patternsByStop) {
            NearbyStopView(patternsAtStop, now)
        }
    }
}