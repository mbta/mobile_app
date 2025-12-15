package com.mbta.tid.mbta_app.android.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType

@Composable fun routeIcon(route: Route) = routeIcon(route.type)

@Composable
fun routeIcon(routeType: RouteType) =
    when (routeType) {
        RouteType.BUS -> painterResource(id = R.drawable.mode_bus)
        RouteType.COMMUTER_RAIL -> painterResource(id = R.drawable.mode_cr)
        RouteType.FERRY -> painterResource(id = R.drawable.mode_ferry)
        else -> painterResource(id = R.drawable.mode_subway)
    }

@Composable
fun routeSlashIcon(routeType: RouteType): Painter {
    return when (routeType) {
        RouteType.BUS -> painterResource(id = R.drawable.mode_bus_slash)
        RouteType.COMMUTER_RAIL -> painterResource(id = R.drawable.mode_cr_slash)
        RouteType.FERRY -> painterResource(id = R.drawable.mode_ferry_slash)
        else -> painterResource(id = R.drawable.mode_subway_slash)
    }
}
