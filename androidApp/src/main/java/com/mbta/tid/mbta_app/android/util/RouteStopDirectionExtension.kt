package com.mbta.tid.mbta_app.android.util

import android.content.Context
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.directionNameFormatted
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.GlobalResponse

data class RouteStopDirectionLabels(val route: String, val stop: String, val direction: String)

fun RouteStopDirection.getLabels(
    global: GlobalResponse?,
    context: Context,
): RouteStopDirectionLabels? {
    val lineOrRoute = global?.getLineOrRoute(this.route)
    val stop = global?.getStop(this.stop)

    if (lineOrRoute == null || stop == null) return null

    val routeLabel =
        if (lineOrRoute.type == RouteType.BUS)
            context.getString(R.string.bus_label, lineOrRoute.name)
        else lineOrRoute.name

    val directionLabel =
        context.getString(directionNameFormatted(Direction(this.direction, lineOrRoute.sortRoute)))

    return RouteStopDirectionLabels(routeLabel, stop.name, directionLabel)
}
