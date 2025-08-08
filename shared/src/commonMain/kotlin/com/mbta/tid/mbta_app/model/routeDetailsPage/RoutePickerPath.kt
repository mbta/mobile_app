package com.mbta.tid.mbta_app.model.routeDetailsPage

import com.mbta.tid.mbta_app.model.RouteType
import kotlinx.serialization.Serializable

@Serializable
public sealed class RoutePickerPath {
    @Serializable public data object Root : RoutePickerPath()

    @Serializable public data object Bus : RoutePickerPath()

    @Serializable public data object Silver : RoutePickerPath()

    @Serializable public data object CommuterRail : RoutePickerPath()

    @Serializable public data object Ferry : RoutePickerPath()

    public val RoutePickerPath.routeType: RouteType
        get() =
            when (this) {
                is Root -> RouteType.HEAVY_RAIL
                is Bus -> RouteType.BUS
                is Silver -> RouteType.BUS
                is CommuterRail -> RouteType.COMMUTER_RAIL
                is Ferry -> RouteType.FERRY
            }
}
