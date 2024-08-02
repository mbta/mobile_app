package com.mbta.tid.mbta_app.android.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.Route

@Composable
fun LineHeader(line: Line, routes: List<Route>, rightContent: (@Composable () -> Unit)? = null) {
    val route = routes.first()
    val (modeIcon, modeDescription) = routeIcon(route = route)
    TransitHeader(
        name = line.longName,
        backgroundColor = Color.fromHex(line.color),
        textColor = Color.fromHex(line.textColor),
        modeIcon = modeIcon,
        modeDescription = modeDescription,
        rightContent = rightContent
    )
}
