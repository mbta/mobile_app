package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.android.component.RoutePillSection
import com.mbta.tid.mbta_app.model.StopsAssociated
import kotlinx.datetime.Instant

@Composable
fun NearbyRouteView(
    nearbyRoute: StopsAssociated.WithRoute,
    now: Instant,
) {
    RoutePillSection(nearbyRoute.route) {
        for (patternsAtStop in nearbyRoute.patternsByStop) {
            NearbyStopView(patternsAtStop, now)
        }
    }
}
