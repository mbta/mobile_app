package com.mbta.tid.mbta_app.android.component

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.Route

@Composable
fun LineCard(
    line: Line,
    routes: List<Route>,
    pinned: Boolean,
    onPin: (String) -> Unit,
    content: @Composable () -> Unit
) {
    TransitCard(
        header = { LineHeader(line, routes) { PinButton(pinned = pinned) { onPin(line.id) } } },
        content
    )
}
