package com.mbta.tid.mbta_app.android.component

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.stopDetails.AlertCard
import com.mbta.tid.mbta_app.android.stopDetails.AlertCardSpec
import com.mbta.tid.mbta_app.android.stopDetails.ColoredRouteLine
import com.mbta.tid.mbta_app.android.stopDetails.RouteLineState
import com.mbta.tid.mbta_app.android.stopDetails.StopDot
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.DestinationPredictionBalance
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.UpcomingFormat

enum class StopRowStyle {
    FirstLineStop,
    MidLineStop,
    LastLineStop,
    StandaloneStop,
}

@Composable
fun StopListRow(
    stop: Stop,
    onClick: () -> Unit,
    routeAccents: TripRouteAccents,
    modifier: Modifier = Modifier,
    activeElevatorAlerts: Int = 0,
    alertSummaries: Map<String, AlertSummary?> = emptyMap(),
    connectingRoutes: List<Route>? = null,
    disruption: UpcomingFormat.Disruption? = null,
    isTruncating: Boolean = false,
    stopRowStyle: StopRowStyle = StopRowStyle.MidLineStop,
    onOpenAlertDetails: (Alert) -> Unit = {},
    showDownstreamAlert: Boolean = false,
    showStationAccessibility: Boolean = false,
    targeted: Boolean = false,
    trackNumber: String? = null,
    descriptor: @Composable () -> Unit = {},
    rightSideContent: @Composable RowScope.(Modifier) -> Unit = {},
) {
    val context = LocalContext.current

    val stateBefore =
        when (stopRowStyle) {
            StopRowStyle.FirstLineStop -> RouteLineState.Empty
            else -> RouteLineState.Regular
        }
    val stateAfter =
        when {
            stopRowStyle == StopRowStyle.LastLineStop -> RouteLineState.Empty
            showDownstreamAlert && disruption?.alert?.effect == Alert.Effect.Shuttle ->
                RouteLineState.Shuttle
            else -> RouteLineState.Regular
        }
    Column {
        Box(
            Modifier.padding(horizontal = 6.dp)
                .then(modifier)
                .height(IntrinsicSize.Min)
                .defaultMinSize(minHeight = 48.dp)
        ) {
            if (stopRowStyle != StopRowStyle.LastLineStop && !targeted && disruption == null) {
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
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (showStationAccessibility && activeElevatorAlerts > 0) {
                        Image(
                            modifier = Modifier.height(18.dp).testTag("elevator_alert"),
                            painter = painterResource(R.drawable.accessibility_icon_alert),
                            contentDescription = null,
                        )
                    } else if (showStationAccessibility && !stop.isWheelchairAccessible) {
                        Image(
                            modifier =
                                Modifier.height(18.dp)
                                    .testTag("wheelchair_not_accessible")
                                    .placeholderIfLoading(),
                            painter = painterResource(R.drawable.accessibility_icon_not_accessible),
                            contentDescription = null,
                        )
                    }
                }
                if (stopRowStyle != StopRowStyle.StandaloneStop) {
                    RouteLine(routeAccents, stateBefore, stateAfter, targeted)
                } else {
                    Row(modifier = Modifier.width(20.dp)) {
                        if (stop.locationType == LocationType.STATION) {
                            Icon(
                                painter = painterResource(R.drawable.mbta_logo),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.semantics { testTag = "mbta_logo" },
                            )
                        } else if (stop.vehicleType == RouteType.BUS) {
                            Icon(
                                painter = painterResource(R.drawable.stop_bus),
                                contentDescription = null,
                                modifier = Modifier.semantics { testTag = "stop_bus" },
                                tint = Color.Unspecified,
                            )
                        } else {
                            StopDot(routeAccents, false)
                        }
                    }
                }
                Column(
                    Modifier.padding(vertical = 12.dp).padding(start = 16.dp).semantics {
                        isTraversalGroup = true
                        traversalIndex = 0F
                    }
                ) {
                    Row(
                        Modifier.semantics(mergeDescendants = true) { heading() }
                            .clickable { onClick() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(
                            DestinationPredictionBalance.destinationWidth(),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            descriptor()
                            Text(
                                stop.name,
                                Modifier.semantics {
                                        contentDescription =
                                            stopAccessibilityLabel(
                                                stop,
                                                targeted,
                                                stopRowStyle == StopRowStyle.FirstLineStop,
                                                context,
                                            )
                                    }
                                    .placeholderIfLoading(),
                                color = colorResource(R.color.text),
                                style = if (targeted) Typography.headlineBold else Typography.body,
                            )
                            if (trackNumber != null) {
                                Text(
                                    stringResource(R.string.track_number, trackNumber),
                                    Modifier.semantics {
                                            contentDescription =
                                                context.getString(
                                                    R.string.boarding_track,
                                                    trackNumber,
                                                )
                                        }
                                        .placeholderIfLoading(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = Typography.footnote,
                                )
                            }
                        }
                        rightSideContent(Modifier.padding(end = 12.dp))
                        // Adding the accessibility description into the stop label rather than on
                        // the accessibility icon so that it is clear which stop it is associated
                        // with. Empty Rows, Canvas, Text that don't take up size, etc. do not have
                        // their content descriptions read.
                        val stopNotAccessibleContentDescription =
                            stringResource(R.string.not_accessible_stop_card)
                        val elevatorAlertContentDescription =
                            pluralStringResource(
                                R.plurals.elevator_closure_count_accessibility_description,
                                activeElevatorAlerts,
                                activeElevatorAlerts,
                            )

                        Text(
                            "",
                            modifier =
                                Modifier.semantics {
                                        contentDescription =
                                            if (
                                                showStationAccessibility &&
                                                    !stop.isWheelchairAccessible
                                            ) {
                                                stopNotAccessibleContentDescription
                                            } else if (
                                                showStationAccessibility && activeElevatorAlerts > 0
                                            ) {
                                                elevatorAlertContentDescription
                                            } else {
                                                ""
                                            }
                                    }
                                    .width(1.dp)
                                    .height(1.dp),
                        )
                    }

                    if (!connectingRoutes.isNullOrEmpty()) {
                        ScrollRoutes(
                            connectingRoutes,
                            Modifier.clearAndSetSemantics {
                                    contentDescription =
                                        scrollRoutesAccessibilityLabel(connectingRoutes, context)
                                }
                                .clickable { onClick() },
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ColoredRouteLine(routeAccents.color, Modifier.weight(1f), stateAfter)
                        if (isTruncating) {
                            ColoredRouteLine(
                                routeAccents.color,
                                Modifier.weight(1f),
                                RouteLineState.Empty,
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
                    interiorPadding = PaddingValues(start = 26.dp),
                )
            }
        }
    }
}

private fun connectionLabel(route: Route, context: Context) =
    context.getString(
        R.string.route_with_type,
        route.label,
        route.type.typeText(context, isOnly = true),
    )

@Composable
fun ScrollRoutes(routes: List<Route>, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp, end = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (route in routes) {
            RoutePill(route, type = RoutePillType.Flex)
        }
    }
}

private fun scrollRoutesAccessibilityLabel(
    connectingRoutes: List<Route>,
    context: Context,
): String =
    when {
        connectingRoutes.isEmpty() -> ""
        connectingRoutes.size == 1 ->
            context.getString(
                R.string.connection_to,
                connectionLabel(connectingRoutes.single(), context),
            )
        else -> {
            val firstConnections = connectingRoutes.dropLast(1)
            val lastConnection = connectingRoutes.last()
            context.getString(
                R.string.connections_to_and,
                firstConnections.joinToString(separator = ", ") { connectionLabel(it, context) },
                connectionLabel(lastConnection, context),
            )
        }
    }

private fun stopAccessibilityLabel(
    stop: Stop,
    targeted: Boolean,
    firstStop: Boolean,
    context: Context,
): String {
    val name = stop.name
    return when {
        targeted && firstStop -> context.getString(R.string.selected_stop_first_stop, name)
        targeted -> context.getString(R.string.selected_stop, name)
        firstStop -> context.getString(R.string.first_stop, name)
        else -> name
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
