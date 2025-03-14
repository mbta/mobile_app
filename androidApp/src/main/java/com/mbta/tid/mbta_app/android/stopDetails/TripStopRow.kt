package com.mbta.tid.mbta_app.android.stopDetails

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.component.UpcomingTripView
import com.mbta.tid.mbta_app.android.component.UpcomingTripViewState
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Composable
fun TripStopRow(
    stop: TripDetailsStopList.Entry,
    now: Instant,
    onTapLink: (TripDetailsStopList.Entry) -> Unit,
    routeAccents: TripRouteAccents,
    showElevatorAccessibility: Boolean = false,
    modifier: Modifier = Modifier,
    targeted: Boolean = false,
    firstStop: Boolean = false,
    lastStop: Boolean = false
) {
    val context = LocalContext.current
    Column(modifier.height(IntrinsicSize.Min).defaultMinSize(minHeight = 48.dp)) {
        Box(contentAlignment = Alignment.BottomCenter) {
            if (!lastStop && !targeted) {
                HaloSeparator()
            }
            Row(
                Modifier.fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showElevatorAccessibility) {
                    Icon(
                        modifier =
                            Modifier.height(24.dp)
                                .padding(
                                    start = if (stop.stop.isWheelchairAccessible) 6.dp else 3.dp
                                ),
                        painter =
                            if (stop.stop.isWheelchairAccessible) {
                                painterResource(R.drawable.wheelchair_accessible)
                            } else {
                                painterResource(R.drawable.elevator_alert)
                            },
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
                RouteLine(
                    routeAccents.color,
                    firstStop,
                    lastStop,
                    routeAccents,
                    targeted,
                    showElevatorAccessibility,
                    isWheelchairAccessible = stop.stop.isWheelchairAccessible
                )
                Column(Modifier.padding(vertical = 12.dp).padding(start = 16.dp)) {
                    Row(
                        Modifier.semantics(mergeDescendants = true) {
                                if (targeted) {
                                    heading()
                                }
                            }
                            .clickable { onTapLink(stop) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                stop.stop.name,
                                Modifier.semantics {
                                        contentDescription =
                                            stopAccessibilityLabel(
                                                stop,
                                                targeted,
                                                firstStop,
                                                context
                                            )
                                    }
                                    .placeholderIfLoading(),
                                color = colorResource(R.color.text),
                                style = if (targeted) Typography.headlineBold else Typography.body,
                            )
                            val trackNumber = stop.trackNumber
                            if (trackNumber != null) {
                                Text(
                                    stringResource(R.string.track_number, trackNumber),
                                    Modifier.semantics {
                                            contentDescription =
                                                context.getString(
                                                    R.string.boarding_track,
                                                    trackNumber
                                                )
                                        }
                                        .placeholderIfLoading(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = Typography.footnote
                                )
                            }
                        }
                        CompositionLocalProvider(
                            LocalContentColor provides colorResource(R.color.text)
                        ) {
                            UpcomingTripView(
                                upcomingTripViewState(stop, now, routeAccents),
                                Modifier.alpha(0.6f).padding(end = 12.dp).width(IntrinsicSize.Min),
                                routeType = routeAccents.type,
                                hideRealtimeIndicators = true
                            )
                        }
                    }

                    if (stop.routes.isNotEmpty()) {
                        ScrollRoutes(
                            stop,
                            Modifier.clearAndSetSemantics {
                                    contentDescription =
                                        scrollRoutesAccessibilityLabel(stop, context)
                                }
                                .clickable { onTapLink(stop) }
                        )
                    }
                }
            }
        }
    }
}

private fun connectionLabel(route: Route, context: Context) =
    context.getString(
        R.string.route_with_type,
        route.label,
        route.type.typeText(context, isOnly = true)
    )

@Composable
fun ScrollRoutes(stop: TripDetailsStopList.Entry, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp, end = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (route in stop.routes) {
            RoutePill(route, type = RoutePillType.Flex)
        }
    }
}

private fun scrollRoutesAccessibilityLabel(
    stop: TripDetailsStopList.Entry,
    context: Context
): String =
    when {
        stop.routes.isEmpty() -> ""
        stop.routes.size == 1 ->
            context.getString(
                R.string.connection_to,
                connectionLabel(stop.routes.single(), context)
            )
        else -> {
            val firstConnections = stop.routes.dropLast(1)
            val lastConnection = stop.routes.last()
            context.getString(
                R.string.connections_to_and,
                firstConnections.joinToString(separator = ", ") { connectionLabel(it, context) },
                connectionLabel(lastConnection, context)
            )
        }
    }

private fun stopAccessibilityLabel(
    stop: TripDetailsStopList.Entry,
    targeted: Boolean,
    firstStop: Boolean,
    context: Context
): String {
    val name = stop.stop.name
    return when {
        targeted && firstStop -> context.getString(R.string.selected_stop_first_stop, name)
        targeted -> context.getString(R.string.selected_stop, name)
        firstStop -> context.getString(R.string.first_stop, name)
        else -> name
    }
}

private fun upcomingTripViewState(
    stop: TripDetailsStopList.Entry,
    now: Instant,
    routeAccents: TripRouteAccents
): UpcomingTripViewState {
    val disruption = stop.disruption
    return if (disruption != null) {
        UpcomingTripViewState.Disruption(
            FormattedAlert(disruption.alert),
            iconName = disruption.iconName
        )
    } else {
        UpcomingTripViewState.Some(stop.format(now, routeAccents.type))
    }
}

@Composable
private fun RouteLine(
    color: Color,
    firstStop: Boolean,
    lastStop: Boolean,
    routeAccents: TripRouteAccents,
    targeted: Boolean,
    showElevatorAccessibility: Boolean,
    isWheelchairAccessible: Boolean
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier.fillMaxHeight()
                .padding(
                    start =
                        if (showElevatorAccessibility && isWheelchairAccessible) 5.dp
                        else if (showElevatorAccessibility) 3.dp else 34.dp
                )
                .width(20.dp)
    ) {
        Column(Modifier.fillMaxHeight()) {
            if (firstStop) {
                ColoredRouteLine(Color.Transparent, Modifier.weight(1f))
            }
            ColoredRouteLine(color, Modifier.weight(1f))
            if (lastStop) {
                ColoredRouteLine(Color.Transparent, Modifier.weight(1f))
            }
        }
        StopDot(routeAccents, targeted)
    }
}

@Preview
@Composable
private fun TripStopRowPreview() {
    val objects = ObjectCollectionBuilder()
    MyApplicationTheme {
        Column(Modifier.background(colorResource(R.color.fill3))) {
            TripStopRow(
                stop =
                    TripDetailsStopList.Entry(
                        objects.stop { name = "Charles/MGH" },
                        stopSequence = 10,
                        disruption = null,
                        schedule = null,
                        prediction = objects.prediction { status = "Stopped 5 stops away" },
                        predictionStop = null,
                        vehicle = null,
                        routes =
                            listOf(
                                objects.route {
                                    longName = "Red Line"
                                    color = "DA291C"
                                    textColor = "FFFFFF"
                                },
                                objects.route {
                                    longName = "Green Line"
                                    color = "00843D"
                                    textColor = "FFFFFF"
                                }
                            )
                    ),
                Clock.System.now(),
                onTapLink = {},
                TripRouteAccents.default.copy(
                    type = RouteType.HEAVY_RAIL,
                    color = Color.fromHex("DA291C")
                ),
                showElevatorAccessibility = true,
            )
            TripStopRow(
                stop =
                    TripDetailsStopList.Entry(
                        objects.stop { name = "Park Street" },
                        stopSequence = 10,
                        disruption = null,
                        schedule = null,
                        prediction =
                            objects.prediction {
                                departureTime = Clock.System.now().plus(5.minutes)
                            },
                        predictionStop = null,
                        vehicle = null,
                        routes =
                            listOf(
                                objects.route {
                                    longName = "Red Line"
                                    color = "DA291C"
                                    textColor = "FFFFFF"
                                },
                                objects.route {
                                    longName = "Green Line"
                                    color = "00843D"
                                    textColor = "FFFFFF"
                                }
                            )
                    ),
                Clock.System.now(),
                onTapLink = {},
                TripRouteAccents.default.copy(
                    type = RouteType.HEAVY_RAIL,
                    color = Color.fromHex("DA291C")
                ),
                showElevatorAccessibility = true
            )
            TripStopRow(
                stop =
                    TripDetailsStopList.Entry(
                        objects.stop { name = "South Station" },
                        stopSequence = 10,
                        disruption = null,
                        schedule = null,
                        prediction =
                            objects.prediction {
                                departureTime = Clock.System.now().plus(5.minutes)
                            },
                        predictionStop = objects.stop { platformCode = "1" },
                        vehicle = null,
                        routes = emptyList()
                    ),
                Clock.System.now(),
                onTapLink = {},
                TripRouteAccents.default.copy(
                    type = RouteType.COMMUTER_RAIL,
                    color = Color.fromHex("DA291C")
                ),
                showElevatorAccessibility = true,
            )
        }
    }
}
