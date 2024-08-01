package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.component.PinButton
import com.mbta.tid.mbta_app.android.component.RouteCardHeader
import com.mbta.tid.mbta_app.model.StopsAssociated
import kotlinx.datetime.Instant

@Composable
fun NearbyRouteView(
    nearbyRoute: StopsAssociated.WithRoute,
    pinned: Boolean,
    onPin: (String) -> Unit,
    now: Instant,
) {
    Card(
        modifier =
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .border(
                    BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline),
                    shape = MaterialTheme.shapes.medium
                ),
        shape = MaterialTheme.shapes.medium
    ) {
        RouteCardHeader(
            nearbyRoute.route,
            rightContent = { PinButton(pinned = pinned) { onPin(nearbyRoute.id) } }
        ) {
            for (patternsAtStop in nearbyRoute.patternsByStop) {
                NearbyStopView(patternsAtStop, now)
            }
        }
    }
}
