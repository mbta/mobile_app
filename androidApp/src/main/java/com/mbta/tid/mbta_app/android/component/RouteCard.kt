package com.mbta.tid.mbta_app.android.component

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.model.Route

@Composable
fun RouteCard(
    route: Route,
    pinned: Boolean,
    onPin: (String) -> Unit,
    content: @Composable () -> Unit
) {
    TransitCard(
        header = { RouteHeader(route) { PinButton(pinned = pinned) { onPin(route.id) } } },
        content = content
    )
}
