package com.mbta.tid.mbta_app.android.util

import android.content.res.Resources
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.utils.RouteModeLabelType

fun routeModeLabel(resources: Resources, lineOrRoute: LineOrRoute, isOnly: Boolean = true): String =
    routeModeLabel(resources, lineOrRoute.name, lineOrRoute.type, isOnly)

fun routeModeLabel(
    resources: Resources,
    line: Line?,
    route: Route?,
    isOnly: Boolean = true,
): String = routeModeLabel(resources, line?.longName ?: route?.label, route?.type, isOnly)

fun routeModeLabel(resources: Resources, route: Route, isOnly: Boolean = true): String =
    routeModeLabel(resources, route.label, route.type, isOnly)

fun routeModeLabel(
    resources: Resources,
    name: String?,
    type: RouteType?,
    isOnly: Boolean = true,
): String {
    val label = com.mbta.tid.mbta_app.utils.routeModeLabel(name, type)
    fun localizedType(type: RouteType) = type.typeText(resources, isOnly)
    return when (label) {
        is RouteModeLabelType.NameAndType ->
            resources.getString(R.string.route_mode_label, label.name, localizedType(label.type))
        is RouteModeLabelType.NameOnly -> label.name
        is RouteModeLabelType.TypeOnly -> localizedType(label.type)
        else -> ""
    }
}
