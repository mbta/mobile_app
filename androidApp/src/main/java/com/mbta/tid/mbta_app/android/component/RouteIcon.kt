package com.mbta.tid.mbta_app.android.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType

@Composable fun routeIcon(route: Route) = routeIcon(route.type)

@Composable
fun routeIcon(routeType: RouteType) =
    when (routeType) {
        RouteType.BUS -> Pair(painterResource(id = R.drawable.mode_bus), "Bus")
        RouteType.COMMUTER_RAIL -> Pair(painterResource(id = R.drawable.mode_cr), "Commuter Rail")
        RouteType.FERRY -> Pair(painterResource(id = R.drawable.mode_ferry), "Ferry")
        else -> Pair(painterResource(id = R.drawable.mode_subway), "Subway")
    }
