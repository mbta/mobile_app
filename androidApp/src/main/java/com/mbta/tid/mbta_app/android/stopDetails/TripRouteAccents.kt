package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType

data class TripRouteAccents(val color: Color, val textColor: Color, val type: RouteType) {
    constructor(
        route: Route
    ) : this(Color.fromHex(route.color), Color.fromHex(route.textColor), route.type)

    companion object {
        val default
            @Composable
            get() =
                TripRouteAccents(
                    colorResource(R.color.halo),
                    colorResource(R.color.text),
                    RouteType.BUS
                )
    }
}
