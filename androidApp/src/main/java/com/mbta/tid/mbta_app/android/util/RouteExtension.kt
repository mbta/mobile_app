package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.silverRoutes

val Route.haloColor
    // The halo over route colors shouldn't be responsive to system theme since it needs to
    // contrast with the consistent route color, rather than a light or dark themed background.
    @Composable
    get() =
        if (type == RouteType.BUS && id !in silverRoutes) colorResource(R.color.halo_light)
        else colorResource(R.color.halo_dark)
