package com.mbta.tid.mbta_app.android.util

import android.content.res.Resources
import com.mbta.tid.mbta_app.android.component.directionNameFormatted
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.GlobalResponse

data class RouteStopDirectionLabels(val route: String, val stop: String, val direction: String)

fun RouteStopDirection.getLabels(
    global: GlobalResponse?,
    resources: Resources,
): RouteStopDirectionLabels? {
    val lineOrRoute = global?.getLineOrRoute(this.route)
    val stop = global?.getStop(this.stop)

    if (lineOrRoute == null || stop == null) return null

    val routeLabel = lineOrRoute.labelWithModeIfBus(resources)

    val directionLabel =
        resources.getString(
            directionNameFormatted(Direction(this.direction, lineOrRoute.sortRoute))
        )

    return RouteStopDirectionLabels(routeLabel, stop.name, directionLabel)
}
