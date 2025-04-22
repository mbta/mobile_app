package com.mbta.tid.mbta_app.android.stopDetails

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
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
import com.mbta.tid.mbta_app.android.util.containsWrappableText
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.DestinationPredictionBalance
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSignificance
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Composable
fun TripStopRow(
    stop: TripDetailsStopList.Entry,
    now: Instant,
    onTapLink: (TripDetailsStopList.Entry) -> Unit,
    onOpenAlertDetails: (Alert) -> Unit,
    routeAccents: TripRouteAccents,
    alertSummaries: Map<String, AlertSummary?>,
    modifier: Modifier = Modifier,
    showStationAccessibility: Boolean = false,
    showDownstreamAlert: Boolean = false,
    targeted: Boolean = false,
    firstStop: Boolean = false,
    lastStop: Boolean = false
) {
    val context = LocalContext.current
    val stateBefore =
        when {
            firstStop -> RouteLineState.Empty
            else -> RouteLineState.Regular
        }
    val stateAfter =
        when {
            lastStop -> RouteLineState.Empty
            showDownstreamAlert && stop.disruption?.alert?.effect == Alert.Effect.Shuttle ->
                RouteLineState.Shuttle
            else -> RouteLineState.Regular
        }
    val disruption =
        stop.disruption?.takeIf {
            it.alert.significance >= AlertSignificance.Major && showDownstreamAlert
        }
    Column {
        Box(
            Modifier.padding(horizontal = 6.dp)
                .then(modifier)
                .height(IntrinsicSize.Min)
                .defaultMinSize(minHeight = 48.dp)
        ) {
            if (!lastStop && !targeted && disruption == null) {
                HaloSeparator(Modifier.align(Alignment.BottomCenter))
            }
            Row(
                Modifier.fillMaxHeight().semantics { isTraversalGroup = true },
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.padding(start = 6.dp).width(28.dp).semantics {
                        isTraversalGroup = true
                        traversalIndex = 1F
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val activeElevatorAlerts = stop.activeElevatorAlerts(now)
                    if (showStationAccessibility && activeElevatorAlerts.isNotEmpty()) {
                        Image(
                            modifier = Modifier.height(18.dp).testTag("elevator_alert"),
                            painter = painterResource(R.drawable.accessibility_icon_alert),
                            contentDescription =
                                pluralStringResource(
                                    R.plurals.elevator_closure_count,
                                    activeElevatorAlerts.size,
                                    activeElevatorAlerts.size
                                )
                        )
                    } else if (showStationAccessibility && !stop.stop.isWheelchairAccessible) {
                        Image(
                            modifier = Modifier.height(18.dp).testTag("wheelchair_not_accessible"),
                            painter = painterResource(R.drawable.accessibility_icon_not_accessible),
                            contentDescription = stringResource(R.string.not_accessible)
                        )
                    }
                }
                RouteLine(routeAccents, stateBefore, stateAfter, targeted)
                Column(
                    Modifier.padding(vertical = 12.dp).padding(start = 16.dp).semantics {
                        isTraversalGroup = true
                        traversalIndex = 0F
                    }
                ) {
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
                            DestinationPredictionBalance.destinationWidth(),
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
                            val state = upcomingTripViewState(stop, now, routeAccents)
                            UpcomingTripView(
                                state,
                                Modifier.padding(end = 12.dp)
                                    .then(
                                        DestinationPredictionBalance.predictionWidth(
                                            state.containsWrappableText()
                                        )
                                    ),
                                routeType = routeAccents.type,
                                hideRealtimeIndicators = true,
                                maxTextAlpha = 0.6f
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
        if (disruption != null) {
            Box(Modifier.height(IntrinsicSize.Min)) {
                // Trying to get this spacing to look right in Android Studio previews at any device
                // DPI requires both layers of padding; moving all 40 DP to one side or the other
                // makes things stop lining up properly. Causality is a superstition.
                Row(Modifier.padding(start = 6.dp).height(IntrinsicSize.Min).matchParentSize()) {
                    Column(
                        Modifier.fillMaxHeight().padding(start = 34.dp).width(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ColoredRouteLine(routeAccents.color, Modifier.weight(1f), stateAfter)
                        if (stop.isTruncating()) {
                            ColoredRouteLine(
                                routeAccents.color,
                                Modifier.weight(1f),
                                RouteLineState.Empty
                            )
                        }
                    }
                }
                AlertCard(
                    disruption.alert,
                    alertSummaries[disruption.alert.id],
                    AlertCardSpec.Downstream,
                    routeAccents.color,
                    routeAccents.textColor,
                    onViewDetails = { onOpenAlertDetails(disruption.alert) },
                    interiorPadding = PaddingValues(start = 26.dp)
                )
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
    routeAccents: TripRouteAccents,
    stateBefore: RouteLineState,
    stateAfter: RouteLineState,
    targeted: Boolean,
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxHeight().width(20.dp)) {
        Column(Modifier.fillMaxHeight()) {
            ColoredRouteLine(routeAccents.color, Modifier.weight(1f), stateBefore)
            ColoredRouteLine(routeAccents.color, Modifier.weight(1f), stateAfter)
        }
        StopDot(routeAccents, targeted)
    }
}

@Preview
@Composable
private fun TripStopRowPreview() {
    val objects = ObjectCollectionBuilder()
    val now = Clock.System.now()
    MyApplicationTheme {
        Column(Modifier.background(colorResource(R.color.fill3))) {
            TripStopRow(
                stop =
                    TripDetailsStopList.Entry(
                        objects.stop {
                            name = "Charles/MGH"
                            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
                        },
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
                now,
                onTapLink = {},
                onOpenAlertDetails = {},
                TripRouteAccents.default.copy(
                    type = RouteType.HEAVY_RAIL,
                    color = Color.fromHex("DA291C")
                ),
                alertSummaries = emptyMap(),
                showStationAccessibility = true,
            )
            TripStopRow(
                stop =
                    TripDetailsStopList.Entry(
                        objects.stop { name = "Park Street" },
                        stopSequence = 10,
                        disruption = null,
                        schedule = null,
                        prediction = objects.prediction { departureTime = now.plus(5.minutes) },
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
                now,
                onTapLink = {},
                onOpenAlertDetails = {},
                TripRouteAccents.default.copy(
                    type = RouteType.HEAVY_RAIL,
                    color = Color.fromHex("DA291C")
                ),
                alertSummaries = emptyMap(),
                showStationAccessibility = true
            )
            TripStopRow(
                stop =
                    TripDetailsStopList.Entry(
                        objects.stop { name = "South Station" },
                        stopSequence = 10,
                        disruption = null,
                        schedule = null,
                        prediction = objects.prediction { departureTime = now.plus(5.minutes) },
                        predictionStop = objects.stop { platformCode = "1" },
                        vehicle = null,
                        routes = emptyList(),
                        elevatorAlerts =
                            listOf(
                                objects.alert {
                                    activePeriod(now.minus(20.minutes), now.plus(20.minutes))
                                }
                            )
                    ),
                now,
                onTapLink = {},
                onOpenAlertDetails = {},
                TripRouteAccents.default.copy(
                    type = RouteType.COMMUTER_RAIL,
                    color = Color.fromHex("DA291C")
                ),
                alertSummaries = emptyMap(),
                showStationAccessibility = true
            )
        }
    }
}

@Preview
@Composable
private fun TripStopRowDisruptionsPreview() {
    val objects = ObjectCollectionBuilder()
    val now = Clock.System.now()
    MyApplicationTheme {
        Box {
            Box(Modifier.matchParentSize().padding(6.dp).background(colorResource(R.color.fill3)))
            Column(Modifier.padding(top = 6.dp)) {
                TripStopRow(
                    stop =
                        TripDetailsStopList.Entry(
                            objects.stop { name = "Charles/MGH" },
                            stopSequence = 10,
                            disruption =
                                UpcomingFormat.Disruption(
                                    objects.alert { effect = Alert.Effect.StopClosure },
                                    mapStopRoute = MapStopRoute.RED
                                ),
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
                    now,
                    onTapLink = {},
                    onOpenAlertDetails = {},
                    TripRouteAccents.default.copy(
                        type = RouteType.HEAVY_RAIL,
                        color = Color.fromHex("DA291C")
                    ),
                    alertSummaries = emptyMap(),
                    showDownstreamAlert = true
                )
                TripStopRow(
                    stop =
                        TripDetailsStopList.Entry(
                            objects.stop { name = "Park Street" },
                            stopSequence = 10,
                            disruption = null,
                            schedule = null,
                            prediction = objects.prediction { departureTime = now.plus(5.minutes) },
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
                    now,
                    onTapLink = {},
                    onOpenAlertDetails = {},
                    TripRouteAccents.default.copy(
                        type = RouteType.HEAVY_RAIL,
                        color = Color.fromHex("DA291C")
                    ),
                    alertSummaries = emptyMap(),
                    showDownstreamAlert = true
                )
                TripStopRow(
                    stop =
                        TripDetailsStopList.Entry(
                            objects.stop { name = "South Station" },
                            stopSequence = 10,
                            disruption =
                                UpcomingFormat.Disruption(
                                    objects.alert { effect = Alert.Effect.Shuttle },
                                    MapStopRoute.RED
                                ),
                            schedule = null,
                            prediction = objects.prediction { departureTime = now.plus(5.minutes) },
                            predictionStop = objects.stop { platformCode = "1" },
                            vehicle = null,
                            routes = emptyList(),
                            elevatorAlerts =
                                listOf(
                                    objects.alert {
                                        activePeriod(now.minus(20.minutes), now.plus(20.minutes))
                                    }
                                )
                        ),
                    now,
                    onTapLink = {},
                    onOpenAlertDetails = {},
                    TripRouteAccents.default.copy(
                        type = RouteType.COMMUTER_RAIL,
                        color = Color.fromHex("DA291C")
                    ),
                    alertSummaries = emptyMap(),
                    showDownstreamAlert = true
                )
            }
        }
    }
}
