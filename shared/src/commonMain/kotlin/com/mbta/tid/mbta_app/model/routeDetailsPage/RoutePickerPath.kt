package com.mbta.tid.mbta_app.model.routeDetailsPage

import com.mbta.tid.mbta_app.model.RouteType
import kotlinx.serialization.Serializable

@Serializable
sealed class RoutePickerPath {
    @Serializable data object Root : RoutePickerPath()

    @Serializable data object Bus : RoutePickerPath()

    @Serializable data object Silver : RoutePickerPath()

    @Serializable data object CommuterRail : RoutePickerPath()

    @Serializable data object Ferry : RoutePickerPath()

    val RoutePickerPath.routeType
        get() =
            when (this) {
                is Root -> RouteType.HEAVY_RAIL
                is Bus -> RouteType.BUS
                is Silver -> RouteType.BUS
                is CommuterRail -> RouteType.COMMUTER_RAIL
                is Ferry -> RouteType.FERRY
            }
}
