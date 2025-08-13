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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.stopDetails.AlertCard
import com.mbta.tid.mbta_app.android.stopDetails.AlertCardSpec
import com.mbta.tid.mbta_app.android.stopDetails.StopDot
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.DestinationPredictionBalance
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.android.util.routeModeLabel
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteBranchSegment
import com.mbta.tid.mbta_app.model.SegmentAlertState
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.repositories.Settings
import kotlinx.serialization.Serializable

class StopPlacement(val isFirst: Boolean = false, val isLast: Boolean = false)

@Serializable
sealed class StopListContext {
    data object Trip : StopListContext()

    data object RouteDetails : StopListContext()
}

@Composable
fun StopListRow(
    stop: Stop,
    stopLane: RouteBranchSegment.Lane,
    stickConnections: List<RouteBranchSegment.StickConnection>,
    onClick: () -> Unit,
    routeAccents: TripRouteAccents,
    stopListContext: StopListContext,
    modifier: Modifier = Modifier,
    activeElevatorAlerts: Int = 0,
    alertSummaries: Map<String, AlertSummary?> = emptyMap(),
    connectingRoutes: List<Route>? = null,
    disruption: UpcomingFormat.Disruption? = null,
    getAlertState: (fromStop: String, toStop: String) -> SegmentAlertState = { _, _ ->
        SegmentAlertState.Normal
    },
    onClickLabel: String? = null,
    stopPlacement: StopPlacement = StopPlacement(),
    onOpenAlertDetails: (Alert) -> Unit = {},
    targeted: Boolean = false,
    trackNumber: String? = null,
    descriptor: @Composable () -> Unit = {},
    rightSideContent: @Composable RowScope.(Modifier) -> Unit = {},
) {
    val context = LocalContext.current
    val showStationAccessibility = SettingsCache.get(Settings.StationAccessibility)

    Column {
        Box(
            Modifier.padding(
                    horizontal = if (stopListContext is StopListContext.Trip) 6.dp else 0.dp
                )
                .then(modifier)
                .height(IntrinsicSize.Min)
                .defaultMinSize(minHeight = 48.dp)
        ) {
            if (!stopPlacement.isLast && !targeted && disruption == null) {
                HaloSeparator(Modifier.align(Alignment.BottomCenter))
            }
            Row(
                Modifier.fillMaxHeight()
                    .padding(start = 8.dp)
                    .semantics(mergeDescendants = true) {}
                    .clickable(onClickLabel = onClickLabel) { onClick() },
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RouteLine(routeAccents, stopLane, stickConnections, targeted, getAlertState)
                Column(Modifier.padding(vertical = 12.dp).padding(start = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (
                            showStationAccessibility &&
                                (activeElevatorAlerts > 0 || !stop.isWheelchairAccessible)
                        ) {
                            Row(
                                Modifier.padding(start = 6.dp).widthIn(max = 28.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                if (activeElevatorAlerts > 0) {
                                    Image(
                                        modifier = Modifier.height(18.dp).testTag("elevator_alert"),
                                        painter =
                                            painterResource(R.drawable.accessibility_icon_alert),
                                        contentDescription = null,
                                    )
                                } else {
                                    Image(
                                        modifier =
                                            Modifier.height(18.dp)
                                                .testTag("wheelchair_not_accessible")
                                                .placeholderIfLoading(),
                                        painter =
                                            painterResource(
                                                R.drawable.accessibility_icon_not_accessible
                                            ),
                                        contentDescription = null,
                                    )
                                }
                            }
                        }
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
                                                stopPlacement.isFirst,
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
                            },
                        )
                    }
                }
            }
        }
        if (disruption != null) {
            Box(Modifier.height(IntrinsicSize.Min)) {
                AlertCard(
                    disruption.alert,
                    alertSummaries[disruption.alert.id],
                    AlertCardSpec.Downstream,
                    routeAccents.color,
                    routeAccents.textColor,
                    onViewDetails = { onOpenAlertDetails(disruption.alert) },
                    interiorPadding = PaddingValues(start = 10.dp),
                )
            }
        }
    }
}

private fun connectionLabel(route: Route, context: Context) = routeModeLabel(context, route)

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

// leading "." creates sentence break
private fun scrollRoutesAccessibilityLabel(
    connectingRoutes: List<Route>,
    context: Context,
): String =
    ". " +
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
                    firstConnections.joinToString(separator = ", ") {
                        connectionLabel(it, context)
                    },
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
    stopLane: RouteBranchSegment.Lane,
    stickConnections: List<RouteBranchSegment.StickConnection>,
    targeted: Boolean,
    getAlertState: (fromStop: String, toStop: String) -> SegmentAlertState,
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxHeight().width(40.dp)) {
        Column(Modifier.fillMaxHeight()) {
            StickDiagram(
                routeAccents.color,
                stickConnections,
                Modifier.weight(1f),
                getAlertState = getAlertState,
            )
        }
        StopDot(
            routeAccents,
            targeted,
            when (stopLane) {
                RouteBranchSegment.Lane.Left -> Modifier.padding(end = 20.dp)
                RouteBranchSegment.Lane.Center -> Modifier
                RouteBranchSegment.Lane.Right -> Modifier.padding(start = 20.dp)
            },
        )
    }
}
