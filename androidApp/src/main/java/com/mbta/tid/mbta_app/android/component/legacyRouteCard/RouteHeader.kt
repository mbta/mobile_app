package com.mbta.tid.mbta_app.android.component.legacyRouteCard

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mbta.tid.mbta_app.android.component.routeCard.TransitHeader
import com.mbta.tid.mbta_app.android.component.routeIcon
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType

@Composable
fun RouteHeader(route: Route, rightContent: (@Composable (textColor: Color) -> Unit)? = null) {
    val routeName =
        when (route.type) {
            RouteType.BUS -> route.shortName
            RouteType.COMMUTER_RAIL -> route.longName.replace("/", " / ")
            else -> route.longName
        }
    val (modeIcon, modeDescription) = routeIcon(route)
    TransitHeader(
        routeName,
        routeType = route.type,
        backgroundColor = Color.fromHex(route.color),
        textColor = Color.fromHex(route.textColor),
        modeIcon = modeIcon,
        modeDescription = modeDescription,
        rightContent
    )
}
