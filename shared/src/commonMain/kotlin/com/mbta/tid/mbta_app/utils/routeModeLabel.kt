package com.mbta.tid.mbta_app.utils

import com.mbta.tid.mbta_app.model.RouteType

public sealed class RouteModeLabelType {
    public data class NameOnly(val name: String) : RouteModeLabelType()

    public data class TypeOnly(val type: RouteType) : RouteModeLabelType()

    public data class NameAndType(val name: String, val type: RouteType) : RouteModeLabelType()

    public data object Empty : RouteModeLabelType()
}

public fun routeModeLabel(name: String?, type: RouteType?): RouteModeLabelType {
    return type?.let {
        if (name == null) {
            RouteModeLabelType.TypeOnly(it)
        } else if (type == RouteType.FERRY) {
            // Route label is redundant for ferry
            RouteModeLabelType.NameOnly(name)
        } else {
            RouteModeLabelType.NameAndType(name, it)
        }
    } ?: name?.let { RouteModeLabelType.NameOnly(it) } ?: RouteModeLabelType.Empty
}
