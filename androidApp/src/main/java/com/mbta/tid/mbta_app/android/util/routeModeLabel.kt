package com.mbta.tid.mbta_app.android.util

import android.content.Context
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.utils.RouteModeLabelType

fun routeModeLabel(
    context: Context,
    lineOrRoute: RouteCardData.LineOrRoute,
    isOnly: Boolean = true,
): String = routeModeLabel(context, lineOrRoute.name, lineOrRoute.type, isOnly)

fun routeModeLabel(context: Context, line: Line?, route: Route?, isOnly: Boolean = true): String =
    routeModeLabel(context, line?.longName ?: route?.label, route?.type, isOnly)

fun routeModeLabel(context: Context, route: Route, isOnly: Boolean = true): String =
    routeModeLabel(context, route.label, route.type, isOnly)

fun routeModeLabel(
    context: Context,
    name: String?,
    type: RouteType?,
    isOnly: Boolean = true,
): String {
    val label = com.mbta.tid.mbta_app.utils.routeModeLabel(name, type)
    fun localizedType(type: RouteType) = type.typeText(context, isOnly)
    return when (label) {
        is RouteModeLabelType.NameAndType ->
            context.getString(R.string.route_mode_label, label.name, localizedType(label.type))
        is RouteModeLabelType.NameOnly -> label.name
        is RouteModeLabelType.TypeOnly -> localizedType(label.type)
        else -> ""
    }
}
