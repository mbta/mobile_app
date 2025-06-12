package com.mbta.tid.mbta_app.android.routePicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath.Bus
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath.CommuterRail
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath.Ferry
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath.Root
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath.Silver

val RoutePickerPath.backgroundColor
    @Composable
    get() =
        when (this) {
            is Root -> colorResource(R.color.fill2)
            is Bus -> colorResource(R.color.mode_bus)
            is Silver -> colorResource(R.color.mode_silver)
            is CommuterRail -> colorResource(R.color.mode_commuter_rail)
            is Ferry -> colorResource(R.color.mode_ferry)
        }
