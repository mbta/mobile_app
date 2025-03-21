package com.mbta.tid.mbta_app.android.component.legacyRouteCard

import androidx.compose.runtime.Composable
import com.mbta.tid.mbta_app.android.component.PinButton
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
        header = {
            LineHeader(line, routes) { textColor ->
                PinButton(pinned = pinned, color = textColor) { onPin(line.id) }
            }
        },
        content
    )
}
