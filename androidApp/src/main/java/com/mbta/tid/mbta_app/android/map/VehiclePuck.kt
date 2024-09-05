package com.mbta.tid.mbta_app.android.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.routeIcon
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.Vehicle

@Composable
fun VehiclePuck(
    vehicle: Vehicle,
    route: Route,
) {
    Box(modifier = Modifier.padding(all = 6.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.rotate(45f + (vehicle.bearing?.toFloat() ?: 0f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.vehicle_halo),
                contentDescription = null
            )
            Image(
                painter = painterResource(id = R.drawable.vehicle_puck),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.fromHex(route.color))
            )
        }
        val (modeIcon, modeDescription) = routeIcon(route = route)
        Image(
            painter = modeIcon,
            contentDescription = modeDescription,
            colorFilter = ColorFilter.tint(Color.fromHex(route.textColor))
        )
    }
}
