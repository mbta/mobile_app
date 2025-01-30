package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.toHex
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import kotlinx.serialization.Serializable

@Serializable
data class TripRouteAccents(val colorHex: String, val textColorHex: String, val type: RouteType) {
    constructor(route: Route) : this(route.color, route.textColor, route.type)

    val color: Color
        get() = Color.fromHex(colorHex)

    val textColor: Color
        get() = Color.fromHex(textColorHex)

    companion object {
        val default
            @Composable
            get() =
                TripRouteAccents(
                    colorResource(R.color.halo).toHex(),
                    colorResource(R.color.text).toHex(),
                    RouteType.BUS
                )
    }
}
