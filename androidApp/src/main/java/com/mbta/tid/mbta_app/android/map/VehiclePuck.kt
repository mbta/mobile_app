package com.mbta.tid.mbta_app.android.map

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addSvg
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.routeIcon
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VehiclePuck(
    vehicle: Vehicle,
    routeAccents: TripRouteAccents,
    selected: Boolean,
    onClick: (() -> Unit)?,
    enlargeIfDecorated: Boolean,
) {
    val routeColor = routeAccents.color
    val textColor = routeAccents.textColor
    val routeType = routeAccents.type
    val scale = if (vehicle.decoration != null && enlargeIfDecorated) 1.5f else 1f
    Box(
        modifier =
            Modifier.clip(CircleShape)
                .scale(scale)
                .size(56.dp * scale)
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
            if (vehicle.decoration == Vehicle.Decoration.Pride) {
                val puckPath = Path()
                puckPath.addSvg(
                    "m-0,14c-0,-2.908 -0,-10.308 -0,-13.001 -0,-0.552 0.445,-0.999 0.997,-0.999 2.608,-0 9.66,-0 13.003,-0 7.732,-0 14,6.268 14,14 0,7.732 -6.268,14 -14,14 -7.732,0 -14,-6.268 -14,-14z"
                )
                Box(
                    Modifier.size(28.dp).drawWithCache {
                        onDrawWithContent {
                            scale(density, Offset(0f, 0f)) {
                                // to get the gradient to line up, we need to un-rotate to get the
                                // start and end
                                // this trigonometry may not quite accurately describe itself, but
                                // it does work
                                val theta = (225 - (vehicle.bearing ?: 0.0)) * PI / 180.0
                                val northX = (cos(theta) + 1) / 2 * size.width / density
                                val southX = (cos(theta + PI) + 1) / 2 * size.width / density
                                val northY = (sin(theta) + 1) / 2 * size.height / density
                                val southY = (sin(theta + PI) + 1) / 2 * size.height / density

                                drawPath(
                                    puckPath,
                                    Brush.linearGradient(
                                        0f to Color.fromHex("7BD1EC"),
                                        0.1f to Color.fromHex("7BD1EC"),
                                        0.1f to Color.fromHex("F7AFC5"),
                                        0.2f to Color.fromHex("F7AFC5"),
                                        0.2f to Color.fromHex("623A16"),
                                        0.3f to Color.fromHex("623A16"),
                                        0.3f to Color.fromHex("000000"),
                                        0.4f to Color.fromHex("000000"),
                                        0.4f to Color.fromHex("DA291C"),
                                        0.5f to Color.fromHex("DA291C"),
                                        0.5f to Color.fromHex("ED8B00"),
                                        0.6f to Color.fromHex("ED8B00"),
                                        0.6f to Color.fromHex("FFC72C"),
                                        0.7f to Color.fromHex("FFC72C"),
                                        0.7f to Color.fromHex("00843D"),
                                        0.8f to Color.fromHex("00843D"),
                                        0.8f to Color.fromHex("003DA5"),
                                        0.9f to Color.fromHex("003DA5"),
                                        0.9f to Color.fromHex("80276C"),
                                        1f to Color.fromHex("80276C"),
                                        start = Offset(northX.toFloat(), northY.toFloat()),
                                        end = Offset(southX.toFloat(), southY.toFloat()),
                                    ),
                                )
                            }
                        }
                    }
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.vehicle_puck),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(routeColor),
                )
            }
        }
        val (modeIcon, colorFilter) =
            when {
                vehicle.decoration == Vehicle.Decoration.GooglyEyes &&
                    routeType == RouteType.COMMUTER_RAIL ->
                    Pair(painterResource(R.drawable.vehicle_cr_eyes), null)
                vehicle.decoration == Vehicle.Decoration.GooglyEyes && routeType.isSubway() ->
                    Pair(painterResource(R.drawable.vehicle_subway_eyes), null)
                vehicle.decoration == Vehicle.Decoration.WinterHoliday && routeType.isSubway() ->
                    Pair(painterResource(R.drawable.vehicle_subway_winter_holiday), null)
                vehicle.decoration == Vehicle.Decoration.Pride ->
                    Pair(routeIcon(routeType), ColorFilter.tint(Color.White))
                else -> Pair(routeIcon(routeType), ColorFilter.tint(textColor))
            }
        Image(painter = modeIcon, contentDescription = null, colorFilter = colorFilter)
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
            decoration = Vehicle.Decoration.Pride
        }
    VehiclePuck(
        vehicle,
        TripRouteAccents(route),
        selected = true,
        onClick = null,
        enlargeIfDecorated = true,
    )
}

@Preview
@Composable
private fun DecorationPreview() {
    val objects = TestData.clone()
    objects.route {
        color = "003DA5"
        type = RouteType.HEAVY_RAIL
        textColor = "FFFFFF"
        sortOrder = 10040
    }
    objects.route {
        color = "008EAA"
        type = RouteType.FERRY
        textColor = "FFFFFF"
        sortOrder = 30002
    }
    val routeAccents = objects.routes.values.sorted().map { TripRouteAccents(it) }.distinct()
    val maxInRow = 4
    val angleAnimation = rememberInfiniteTransition()
    val angleOffset by
        angleAnimation.animateFloat(
            0f,
            360f,
            infiniteRepeatable(tween(durationMillis = 5000, easing = LinearEasing)),
        )
    @Composable
    fun DemoRow(decoration: Vehicle.Decoration, modes: Set<RouteType> = RouteType.entries.toSet()) {
        FlowRow(verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = maxInRow) {
            for ((index, routeAccents) in routeAccents.filter { it.type in modes }.withIndex()) {
                VehiclePuck(
                    objects.vehicle {
                        currentStatus = Vehicle.CurrentStatus.InTransitTo
                        this.decoration = decoration
                        bearing = 45.0 * index + angleOffset
                    },
                    routeAccents,
                    selected = false,
                    onClick = null,
                    enlargeIfDecorated = true,
                )
            }
        }
    }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DemoRow(Vehicle.Decoration.Pride)
        DemoRow(Vehicle.Decoration.WinterHoliday, setOf(RouteType.LIGHT_RAIL, RouteType.HEAVY_RAIL))
        DemoRow(
            Vehicle.Decoration.GooglyEyes,
            setOf(RouteType.LIGHT_RAIL, RouteType.HEAVY_RAIL, RouteType.COMMUTER_RAIL),
        )
    }
}
