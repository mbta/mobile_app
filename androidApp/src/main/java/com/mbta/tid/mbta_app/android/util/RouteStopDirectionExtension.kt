package com.mbta.tid.mbta_app.android.util

import android.content.Context
import com.mbta.tid.mbta_app.android.component.directionNameFormatted
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.GlobalResponse

data class RouteStopDirectionLabels(val route: String, val stop: String, val direction: String)

fun RouteStopDirection.getLabels(
    global: GlobalResponse?,
    context: Context,
): RouteStopDirectionLabels? {
    val lineOrRoute = global?.getLineOrRoute(this.route)
    val stop = global?.getStop(this.stop)

    if (lineOrRoute == null || stop == null) return null

    val routeLabel = lineOrRoute.label(context)

    val directionLabel =
        context.getString(directionNameFormatted(Direction(this.direction, lineOrRoute.sortRoute)))

    return RouteStopDirectionLabels(routeLabel, stop.name, directionLabel)
}
