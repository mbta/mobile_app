package com.mbta.tid.mbta_app.model.routeDetailsPage

import kotlinx.serialization.Serializable

@Serializable
sealed class RoutePickerPath {
    @Serializable data object Root : RoutePickerPath()

    @Serializable data object Bus : RoutePickerPath()

    @Serializable data object Silver : RoutePickerPath()

    @Serializable data object CommuterRail : RoutePickerPath()

    @Serializable data object Ferry : RoutePickerPath()
}
