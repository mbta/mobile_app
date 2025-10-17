package com.mbta.tid.mbta_app.android.map

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.routeIcon
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.Vehicle

@Composable
fun VehiclePuck(vehicle: Vehicle, route: Route, selected: Boolean, onClick: (() -> Unit)?) {
    val routeColor = Color.fromHex(route.color)
    Box(
        modifier =
            Modifier.size(56.dp)
                .clip(CircleShape)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            val haloPulseAnimation = rememberInfiniteTransition(label = "haloPulseAnimation")
            Box(Modifier.background(routeColor.copy(alpha = 0.33f), CircleShape).size(56.dp))
            val strokeAlpha by
                haloPulseAnimation.animateFloat(0.5f, 0.1f, haloPulseSpec(), label = "strokeAlpha")
            val size by
                haloPulseAnimation.animateValue(
                    0.dp,
                    56.dp,
                    Dp.VectorConverter,
                    haloPulseSpec(),
                    label = "size",
                )
            Box(Modifier.size(size).border(1.dp, routeColor.copy(alpha = strokeAlpha), CircleShape))
        }
        Box(
            modifier = Modifier.rotate(45f + (vehicle.bearing?.toFloat() ?: 0f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.vehicle_halo),
                contentDescription = null,
            )
            Image(
                painter = painterResource(id = R.drawable.vehicle_puck),
                contentDescription = null,
                colorFilter = ColorFilter.tint(routeColor),
            )
        }
        val (modeIcon, modeDescription) = routeIcon(route = route)
        Image(
            painter = modeIcon,
            contentDescription = modeDescription,
            colorFilter = ColorFilter.tint(Color.fromHex(route.textColor)),
        )
    }
}

private fun <T> haloPulseSpec() =
    infiniteRepeatable<T>(
        tween(durationMillis = 2000, delayMillis = 2000, easing = EaseOut),
        repeatMode = RepeatMode.Restart,
    )

@Preview
@Composable
private fun VehiclePuckPreview() {
    val objects = ObjectCollectionBuilder("VehiclePuckPreview")
    val route =
        objects.route {
            color = "DA291C"
            textColor = "FFFFFF"
        }
    val vehicle =
        objects.vehicle {
            currentStatus = Vehicle.CurrentStatus.InTransitTo
            bearing = 225.0
        }
    VehiclePuck(vehicle, route, selected = true) {}
}
