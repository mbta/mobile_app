package com.mbta.tid.mbta_app.android.component.legacyRouteCard

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.android.component.PinButton
import com.mbta.tid.mbta_app.model.Route

@Composable
fun LegacyRouteCard(
    route: Route,
    pinned: Boolean,
    onPin: (String) -> Unit,
    content: @Composable () -> Unit
) {
    TransitCard(
        header = {
            RouteHeader(route) { textColor ->
                PinButton(pinned = pinned, color = textColor) { onPin(route.id) }
            }
        },
        content = content
    )
}
