package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSignificance
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripHeaderSpec
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Composable
fun TripStops(
    targetId: String,
    stops: TripDetailsStopList,
    stopSequence: Int?,
    headerSpec: TripHeaderSpec?,
    now: Instant,
    alertSummaries: Map<String, AlertSummary?>,
    global: GlobalResponse?,
    onTapLink: (TripDetailsStopList.Entry) -> Unit,
    onOpenAlertDetails: (Alert) -> Unit,
    routeAccents: TripRouteAccents,
    showStationAccessibility: Boolean = false,
) {
    val context = LocalContext.current

    val trip = stops.trip
    val splitStops: TripDetailsStopList.TargetSplit =
        remember(targetId, stops, stopSequence, global) {
            stops.splitForTarget(targetId, stopSequence, global)
        }

    var stopsExpanded by rememberSaveable { mutableStateOf(false) }

    val routeTypeText = routeAccents.type.typeText(context, isOnly = true)
    val collapsedStops = splitStops.collapsedStops
    val stopsAway = collapsedStops?.size
    val target = splitStops.targetStop
    val hideTarget =
        headerSpec is TripHeaderSpec.Scheduled &&
            target != null &&
            target == stops.startTerminalEntry

    val showFirstStopSeparately =
        when (headerSpec) {
            TripHeaderSpec.FinishingAnotherTrip,
            TripHeaderSpec.NoVehicle -> true
            else -> false
        }

    val lastStopSequence = stops.stops.lastOrNull()?.stopSequence

    Box {
        Box(
            Modifier.matchParentSize()
                .padding(horizontal = 4.dp)
                .padding(
                    bottom =
                        if (
                            splitStops.followingStops
                                .lastOrNull()
                                ?.disruption
                                ?.alert
                                ?.significance == AlertSignificance.Major
                        )
                            4.dp
                        else 0.dp
                )
                .haloContainer(2.dp, backgroundColor = colorResource(R.color.fill2))
        )
        Column(
            Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            if (showFirstStopSeparately) {
                val firstStop = splitStops.firstStop
                if (firstStop != null) {
                    TripStopRow(
                        stop = firstStop,
                        trip,
                        now,
                        onTapLink,
                        onOpenAlertDetails,
                        routeAccents,
                        alertSummaries,
                        showStationAccessibility = showStationAccessibility,
                        firstStop = true,
                    )
                }
            }
            if (!collapsedStops.isNullOrEmpty() && stopsAway != null && target != null) {
                Row(
                    Modifier.height(IntrinsicSize.Min)
                        .clickable(
                            onClickLabel =
                                if (stopsExpanded) stringResource(R.string.collapse_remaining_stops)
                                else stringResource(R.string.expand_remaining_stops)
                        ) {
                            stopsExpanded = !stopsExpanded
                        }
                        .clearAndSetSemantics {
                            contentDescription =
                                context.getString(
                                    R.string.is_stops_away_from,
                                    routeTypeText,
                                    stopsAway,
                                    target.stop.name,
                                )
                        }
                        .padding(horizontal = 12.dp)
                        .defaultMinSize(minHeight = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedContent(
                        stopsExpanded,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(500)) togetherWith
                                fadeOut(animationSpec = tween(500))
                        },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            if (it) {
                                Icon(
                                    painterResource(R.drawable.fa_caret_right),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp).rotate(90f),
                                    tint = colorResource(R.color.deemphasized),
                                )
                                ColoredRouteLine(
                                    routeAccents.color,
                                    Modifier.padding(start = 16.dp, end = 18.dp).fillMaxHeight(),
                                )
                            } else {
                                Icon(
                                    painterResource(R.drawable.fa_caret_right),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = colorResource(R.color.deemphasized),
                                )
                                RouteLineTwist(
                                    routeAccents.color,
                                    Modifier.padding(start = 6.dp, end = 6.dp),
                                )
                            }
                        }
                    }
                    Text(
                        pluralStringResource(R.plurals.stops_away, stopsAway, stopsAway),
                        color = colorResource(R.color.text),
                        style = Typography.body,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (stopsExpanded) {
                    Column {
                        HaloUnderRouteLine(routeAccents.color)
                        StopList(
                            list = collapsedStops,
                            trip,
                            lastStopSequence,
                            now,
                            onTapLink,
                            onOpenAlertDetails,
                            routeAccents,
                            alertSummaries,
                            showStationAccessibility,
                        )
                    }
                }
            }
            // If the target is the first stop and there's no vehicle, it's already displayed in the
            // trip header
            if (target != null && !hideTarget) {
                if (
                    !collapsedStops.isNullOrEmpty() ||
                        (showFirstStopSeparately && splitStops.firstStop != null)
                ) {
                    // We want a double halo above and below the selected stop
                    if (!stopsExpanded) {
                        // Expanded stops are adding an extra separator and I'm not sure where from
                        HaloUnderRouteLine(routeAccents.color)
                    }
                    HaloUnderRouteLine(routeAccents.color)
                }
                TripStopRow(
                    stop = target,
                    trip,
                    now,
                    onTapLink,
                    onOpenAlertDetails,
                    routeAccents,
                    alertSummaries,
                    targeted = true,
                    firstStop = showFirstStopSeparately && target == stops.startTerminalEntry,
                    modifier = Modifier.background(colorResource(R.color.fill3)),
                    showStationAccessibility = showStationAccessibility,
                )

                HaloUnderRouteLine(routeAccents.color)
                HaloUnderRouteLine(routeAccents.color)
            }
            StopList(
                splitStops.followingStops,
                trip,
                lastStopSequence,
                now,
                onTapLink,
                onOpenAlertDetails,
                routeAccents,
                alertSummaries,
                showStationAccessibility,
                showDownstreamAlerts = true,
            )
        }
    }
}

@Composable
private fun HaloUnderRouteLine(color: Color) {
    Box(Modifier.padding(horizontal = 6.dp).height(IntrinsicSize.Min)) {
        HaloSeparator()
        // Lil 1x4 pt route color bar to maintain an unbroken route color line
        // over the separator
        ColoredRouteLine(color, Modifier.padding(start = 42.dp).fillMaxHeight())
    }
}

@Composable
private fun StopList(
    list: List<TripDetailsStopList.Entry>,
    trip: Trip,
    lastStopSequence: Int?,
    now: Instant,
    onTapLink: (TripDetailsStopList.Entry) -> Unit,
    onOpenAlertDetails: (Alert) -> Unit,
    routeAccents: TripRouteAccents,
    alertSummaries: Map<String, AlertSummary?>,
    showStationAccessibility: Boolean,
    showDownstreamAlerts: Boolean = false,
) {
    for (stop in list) {
        TripStopRow(
            stop,
            trip,
            now,
            onTapLink,
            onOpenAlertDetails,
            routeAccents,
            alertSummaries,
            showStationAccessibility = showStationAccessibility,
            showDownstreamAlert = showDownstreamAlerts,
            lastStop = stop.stopSequence == lastStopSequence,
        )
    }
}

@Preview
@Composable
private fun TripStopsPreview() {
    val objects = ObjectCollectionBuilder()
    val route =
        objects.route {
            color = "FFC72C"
            shortName = "109"
            textColor = "000000"
            type = RouteType.BUS
        }
    val stops =
        (1..10).map {
            objects.stop {
                name = "Stop $it"
                wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE
            }
        }
    val trip = objects.trip()
    val now = Clock.System.now()
    val alertStartIndex = 7
    val alert = objects.alert { effect = Alert.Effect.Shuttle }
    val stopList =
        TripDetailsStopList(
            trip,
            stops.mapIndexed { index, stop ->
                TripDetailsStopList.Entry(
                    stop,
                    stopSequence = index,
                    disruption =
                        if (index >= alertStartIndex)
                            UpcomingFormat.Disruption(alert, "alert-borderless-suspension")
                        else null,
                    schedule = null,
                    prediction = objects.prediction { departureTime = now + (2 * index).minutes },
                    vehicle = null,
                    routes = emptyList(),
                )
            },
        )
    MyApplicationTheme {
        Column(Modifier.background(Color.fromHex(route.color))) {
            TripStops(
                targetId = stops[4].id,
                stopList,
                4,
                TripHeaderSpec.NoVehicle,
                Clock.System.now(),
                emptyMap(),
                GlobalResponse(objects),
                onTapLink = {},
                onOpenAlertDetails = {},
                TripRouteAccents(route),
                showStationAccessibility = true,
            )
        }
    }
}
