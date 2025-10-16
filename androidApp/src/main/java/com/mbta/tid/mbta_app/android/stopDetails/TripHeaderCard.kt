package com.mbta.tid.mbta_app.android.stopDetails

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.InfoCircle
import com.mbta.tid.mbta_app.android.component.LiveIcon
import com.mbta.tid.mbta_app.android.component.StickDiagram
import com.mbta.tid.mbta_app.android.component.TightWrapText
import com.mbta.tid.mbta_app.android.component.UpcomingTripView
import com.mbta.tid.mbta_app.android.component.UpcomingTripViewState
import com.mbta.tid.mbta_app.android.component.routeIcon
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.containsWrappableText
import com.mbta.tid.mbta_app.android.util.modifiers.DestinationPredictionBalance
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteBranchSegment
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripHeaderSpec
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.minutes

@Composable
fun TripHeaderCard(
    trip: Trip,
    spec: TripHeaderSpec?,
    targetId: String,
    route: Route,
    routeAccents: TripRouteAccents,
    now: EasternTimeInstant,
    modifier: Modifier = Modifier,
    onFollowTrip: (() -> Unit)? = null,
    onTap: (() -> Unit)? = null,
) {
    val clickable = onTap != null

    val modifier =
        if (clickable) {
            modifier.clickable(onClickLabel = stringResource(R.string.display_more_information)) {
                onTap?.let { it() }
            }
        } else {
            // clickable(false) is announced to TalkBack as "disabled", which we don't want
            modifier
        }

    CompositionLocalProvider(LocalContentColor provides colorResource(R.color.text)) {
        Box(
            Modifier.height(IntrinsicSize.Min).fillMaxWidth(),
            contentAlignment = Alignment.BottomStart,
        ) {
            Row(modifier.haloContainer(2.dp).semantics(mergeDescendants = true) {}) {
                Box(Modifier.height(IntrinsicSize.Min)) {
                    Row(
                        Modifier.padding(horizontal = 14.dp).heightIn(min = 56.dp).semantics {
                            heading()
                            liveRegion = LiveRegionMode.Polite
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (spec != null) {
                            TripMarker(spec, targetId, routeAccents)
                            Description(
                                spec,
                                trip.id,
                                targetId,
                                routeAccents,
                                clickable,
                                DestinationPredictionBalance.destinationWidth(),
                            )
                            TripIndicator(
                                spec,
                                route,
                                routeAccents,
                                trip,
                                now,
                                clickable,
                                onFollowTrip,
                            )
                        }
                    }
                    when (spec) {
                        is TripHeaderSpec.Scheduled,
                        is TripHeaderSpec.VehicleOnTrip ->
                            StickDiagram(
                                routeAccents.color,
                                listOf(
                                    RouteBranchSegment.StickConnection(
                                        fromStop = "",
                                        toStop = "",
                                        fromLane = RouteBranchSegment.Lane.Center,
                                        toLane = RouteBranchSegment.Lane.Center,
                                        fromVPos = RouteBranchSegment.VPos.Center,
                                        toVPos = RouteBranchSegment.VPos.Bottom,
                                    )
                                ),
                                Modifier.zIndex(-1f).padding(start = 12.dp).fillMaxHeight(),
                            )
                        else -> {}
                    }
                }
            }
            when (spec) {
                is TripHeaderSpec.Scheduled,
                is TripHeaderSpec.VehicleOnTrip ->
                    // Small 2x4 dp portion of route line over the outer card border
                    StickDiagram(
                        routeAccents.color,
                        listOf(
                            RouteBranchSegment.StickConnection(
                                fromStop = "",
                                toStop = "",
                                fromLane = RouteBranchSegment.Lane.Center,
                                toLane = RouteBranchSegment.Lane.Center,
                                fromVPos = RouteBranchSegment.VPos.Top,
                                toVPos = RouteBranchSegment.VPos.Bottom,
                            )
                        ),
                        Modifier.zIndex(1f).padding(start = 14.dp).height(2.dp),
                    )
                else -> {}
            }
        }
    }
}

@Composable
private fun Description(
    spec: TripHeaderSpec,
    tripId: String,
    targetId: String,
    routeAccents: TripRouteAccents,
    clickable: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.padding(vertical = 16.dp)) {
        when (spec) {
            TripHeaderSpec.FinishingAnotherTrip -> FinishingAnotherTripDescription()
            TripHeaderSpec.NoVehicle -> NoVehicleDescription()
            is TripHeaderSpec.Scheduled ->
                ScheduleDescription(spec.entry, targetId, routeAccents, clickable)
            is TripHeaderSpec.VehicleOnTrip ->
                VehicleDescription(spec, tripId, targetId, routeAccents)
        }
    }
}

@Composable
private fun FinishingAnotherTripDescription() {
    Text(stringResource(R.string.finishing_another_trip), style = Typography.footnote)
}

@Composable
private fun NoVehicleDescription() {
    Text(stringResource(R.string.location_not_available_yet), style = Typography.footnote)
}

@Composable
private fun ScheduleDescription(
    startTerminalEntry: TripDetailsStopList.Entry?,
    targetId: String,
    routeAccents: TripRouteAccents,
    clickable: Boolean,
) {
    val context = LocalContext.current
    if (startTerminalEntry != null) {
        Column(
            Modifier.semantics {
                contentDescription =
                    scheduleDescriptionAccessibilityText(
                        startTerminalEntry,
                        targetId,
                        routeAccents,
                        context,
                    )
            },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TightWrapText(
                    stringResource(R.string.scheduled_to_depart),
                    Modifier.weight(1f, fill = false),
                    style = Typography.footnote,
                )
                if (clickable) {
                    InfoCircle(Modifier.aspectRatio(1f).size(16.dp))
                }
            }
            Text(startTerminalEntry.stop.name, style = Typography.headlineBold)
        }
    }
}

private fun scheduleDescriptionAccessibilityText(
    stopEntry: TripDetailsStopList.Entry,
    targetId: String,
    routeAccents: TripRouteAccents,
    context: Context,
): String {
    return if (targetId == stopEntry.stop.id) {
        context.getString(
            R.string.scheduled_to_depart_selected_stop_accessibility_desc,
            routeAccents.type.typeText(context, isOnly = true),
            stopEntry.stop.name,
        )
    } else {
        context.getString(
            R.string.scheduled_to_depart_accessibility_desc,
            routeAccents.type.typeText(context, isOnly = true),
            stopEntry.stop.name,
        )
    }
}

@Composable
private fun VehicleDescription(
    spec: TripHeaderSpec.VehicleOnTrip,
    tripId: String,
    targetId: String,
    routeAccents: TripRouteAccents,
) {
    val context = LocalContext.current
    if (spec.vehicle.tripId == tripId) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(
                Modifier.clearAndSetSemantics {
                    contentDescription =
                        vehicleDescriptionAccessibilityText(spec, targetId, routeAccents, context)
                },
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                VehicleStatusDescription(spec.vehicle.currentStatus, spec.atTerminal)
                Text(
                    spec.stop.name,
                    style = Typography.headlineBold,
                    modifier = Modifier.placeholderIfLoading(),
                )
            }
            spec.entry?.trackNumber?.let {
                Text(
                    context.getString(R.string.track_number, it),
                    Modifier.semantics {
                            contentDescription = context.getString(R.string.boarding_track, it)
                        }
                        .placeholderIfLoading(),
                    style = Typography.footnote,
                )
            }
        }
    }
}

private fun vehicleDescriptionAccessibilityText(
    spec: TripHeaderSpec.VehicleOnTrip,
    targetId: String,
    routeAccents: TripRouteAccents,
    context: Context,
): String {
    val stop = spec.stop
    return context.getString(
        if (targetId == stop.id) R.string.vehicle_desc_accessibility_desc_selected_stop
        else R.string.vehicle_desc_accessibility_desc,
        routeAccents.type.typeText(context, isOnly = true),
        vehicleStatusString(context, spec.vehicle.currentStatus, spec.atTerminal),
        stop.name,
    )
}

@Composable
private fun VehicleStatusDescription(vehicleStatus: Vehicle.CurrentStatus, atTerminal: Boolean) {
    val context = LocalContext.current
    Text(
        vehicleStatusString(context, vehicleStatus, atTerminal),
        style = Typography.footnote,
        modifier = Modifier.placeholderIfLoading(),
    )
}

private fun vehicleStatusString(
    context: Context,
    vehicleStatus: Vehicle.CurrentStatus,
    atTerminal: Boolean,
): String {
    return when (vehicleStatus) {
        Vehicle.CurrentStatus.IncomingAt -> context.getString(R.string.approaching)
        Vehicle.CurrentStatus.InTransitTo -> context.getString(R.string.next_stop)
        Vehicle.CurrentStatus.StoppedAt ->
            if (atTerminal) context.getString(R.string.waiting_to_depart)
            else context.getString(R.string.now_at)
    }
}

@Composable
private fun TripMarker(spec: TripHeaderSpec, targetId: String, routeAccents: TripRouteAccents) {
    Box(
        Modifier.width(36.dp).fillMaxHeight().clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        when (spec) {
            TripHeaderSpec.FinishingAnotherTrip,
            TripHeaderSpec.NoVehicle -> VehicleCircle(routeAccents)
            is TripHeaderSpec.Scheduled ->
                StopDot(routeAccents, targeted = targetId == spec.stop.id)
            is TripHeaderSpec.VehicleOnTrip ->
                VehiclePuck(spec.vehicle, spec.stop, targetId, routeAccents)
        }
    }
}

@Composable
private fun VehicleCircle(routeAccents: TripRouteAccents) {
    Box(Modifier.size(32.dp).background(routeAccents.color, CircleShape)) {
        val (icon, _) = routeIcon(routeAccents.type)
        Image(
            icon,
            null,
            Modifier.size(27.5.dp).align(Alignment.Center),
            colorFilter = ColorFilter.tint(routeAccents.textColor),
        )
    }
}

@Composable
private fun VehiclePuck(
    vehicle: Vehicle,
    stop: Stop,
    targetId: String,
    routeAccents: TripRouteAccents,
) {
    Box(Modifier.clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.rotate(225f), contentAlignment = Alignment.Center) {
                Image(
                    painterResource(R.drawable.vehicle_halo),
                    null,
                    Modifier.size(36.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.background),
                )
                Image(
                    painterResource(R.drawable.vehicle_puck),
                    null,
                    Modifier.size(32.dp),
                    colorFilter = ColorFilter.tint(routeAccents.color),
                )
            }
            val (icon, _) = routeIcon(routeAccents.type)
            Image(
                icon,
                null,
                Modifier.size(27.5.dp).align(Alignment.Center),
                colorFilter = ColorFilter.tint(routeAccents.textColor),
            )
        }

        if (targetId == stop.id && vehicle.currentStatus == Vehicle.CurrentStatus.StoppedAt) {
            Box(
                modifier =
                    Modifier.align(Alignment.Center)
                        .padding(bottom = 36.dp)
                        .testTag("stop_pin_indicator")
            ) {
                Image(painterResource(R.drawable.stop_pin_indicator), null)
            }
        }
    }
}

@Composable
private fun RowScope.TripIndicator(
    spec: TripHeaderSpec,
    route: Route,
    routeAccents: TripRouteAccents,
    trip: Trip,
    now: EasternTimeInstant,
    clickable: Boolean = false,
    onFollowTrip: (() -> Unit)?,
) {
    val upcomingTripViewState = upcomingTripViewState(spec, route, trip, now)
    Column(
        DestinationPredictionBalance.predictionWidth(
            upcomingTripViewState?.containsWrappableText() ?: false
        ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End,
    ) {
        when (spec) {
            TripHeaderSpec.FinishingAnotherTrip,
            TripHeaderSpec.NoVehicle -> {
                if (clickable) {
                    InfoCircle()
                }
            }
            is TripHeaderSpec.VehicleOnTrip ->
                if (onFollowTrip != null) FollowButton(routeAccents, onFollowTrip)
                else {
                    LiveIndicator()
                }
            is TripHeaderSpec.Scheduled -> {}
        }

        if (upcomingTripViewState != null) {
            UpcomingTripView(
                upcomingTripViewState,
                routeType = routeAccents.type,
                hideRealtimeIndicators = true,
            )
        }
    }
}

@Composable
private fun FollowButton(routeAccents: TripRouteAccents, onClick: () -> Unit) {
    Column(
        Modifier.sizeIn(minHeight = 36.dp, minWidth = 97.dp)
            .haloContainer(2.dp, backgroundColor = routeAccents.color, borderRadius = 88.dp)
            .clickable(onClickLabel = null, role = Role.Button, onClick = onClick),
        Arrangement.Center,
        Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalContentColor provides routeAccents.textColor) {
            Row(
                Modifier.padding(start = 12.dp, top = 10.dp, end = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LiveIcon(size = 12.dp)
                Text(
                    stringResource(R.string.follow),
                    softWrap = false,
                    maxLines = 1,
                    style = Typography.callout,
                )
            }
        }
    }
}

@Composable
private fun LiveIndicator() {
    val desc = stringResource(R.string.real_time_arrivals_updating_live)
    Row(
        Modifier.alpha(0.6f).clearAndSetSemantics { contentDescription = desc },
        Arrangement.spacedBy(4.dp),
        Alignment.Bottom,
    ) {
        LiveIcon()
        Text(
            stringResource(R.string.live),
            style = Typography.footnote,
            modifier = Modifier.placeholderIfLoading(),
        )
    }
}

private fun upcomingTripViewState(
    spec: TripHeaderSpec,
    route: Route,
    trip: Trip,
    now: EasternTimeInstant,
): UpcomingTripViewState? {
    val entry =
        when (spec) {
            is TripHeaderSpec.VehicleOnTrip -> if (spec.atTerminal) spec.entry else null
            is TripHeaderSpec.Scheduled -> spec.entry
            else -> null
        }
    if (entry == null) return null
    return when (val formatted = entry.format(trip, now, route)) {
        is UpcomingFormat.Some ->
            UpcomingTripViewState.Some(formatted.trips.singleOrNull()?.format ?: return null)
        else -> null
    }
}

@Preview
@Composable
private fun TripHeaderCardPreview() {
    val objects = ObjectCollectionBuilder()
    val red =
        objects.route {
            id = "Red"
            longName = "Red Line"
            color = "DA291C"
            type = RouteType.HEAVY_RAIL
            textColor = "FFFFFF"
        }
    val bus =
        objects.route {
            id = "66"
            shortName = "66"
            color = "FFC72C"
            type = RouteType.BUS
            textColor = "000000"
        }
    val ferry =
        objects.route {
            id = "ferry"
            longName = "Charlestown Ferry"
            color = "008EAA"
            type = RouteType.FERRY
            textColor = "FFFFFF"
        }
    val commuter =
        objects.route {
            id = "commuter"
            longName = "Framingham/Worcester Line"
            color = "80276C"
            type = RouteType.COMMUTER_RAIL
            textColor = "FFFFFF"
        }
    val trip =
        objects.trip {
            id = "1234"
            headsign = "Alewife"
        }
    val vehicle =
        objects.vehicle {
            id = "y1234"
            currentStatus = Vehicle.CurrentStatus.StoppedAt
            currentStopSequence = 30
            directionId = 1
            routeId = "66"
            stopId = "place-davis"
            tripId = trip.id
        }
    val davis = objects.stop { name = "Davis" }
    val cityPoint = objects.stop { name = "City Point Bus Terminal" }

    val rlEntry =
        TripDetailsStopList.Entry(
            davis,
            0,
            null,
            null,
            objects.prediction { departureTime = EasternTimeInstant.now().plus(5.minutes) },
            vehicle = null,
            routes = listOf(red),
        )
    val busEntry =
        TripDetailsStopList.Entry(
            cityPoint,
            0,
            null,
            objects.schedule { departureTime = EasternTimeInstant.now().plus(5.minutes) },
            null,
            vehicle = null,
            routes = listOf(bus),
        )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MyApplicationTheme {
            TripHeaderCard(
                trip = trip,
                spec = TripHeaderSpec.VehicleOnTrip(vehicle, davis, rlEntry, true),
                targetId = davis.id,
                route = red,
                routeAccents = TripRouteAccents(red),
                now = EasternTimeInstant.now(),
            )

            TripHeaderCard(
                trip = trip,
                spec = TripHeaderSpec.VehicleOnTrip(vehicle, davis, rlEntry, true),
                targetId = davis.id,
                route = red,
                routeAccents = TripRouteAccents(red),
                now = EasternTimeInstant.now(),
                onFollowTrip = {},
            )

            TripHeaderCard(
                trip = trip,
                spec = TripHeaderSpec.Scheduled(cityPoint, busEntry),
                targetId = cityPoint.id,
                route = red,
                routeAccents = TripRouteAccents(bus),
                onTap = {},
                now = EasternTimeInstant.now(),
            )

            TripHeaderCard(
                trip = trip,
                spec = TripHeaderSpec.FinishingAnotherTrip,
                targetId = "",
                route = red,
                routeAccents = TripRouteAccents(commuter),
                onTap = {},
                now = EasternTimeInstant.now(),
            )

            TripHeaderCard(
                trip = trip,
                spec = TripHeaderSpec.NoVehicle,
                targetId = "",
                route = red,
                routeAccents = TripRouteAccents(ferry),
                onTap = {},
                now = EasternTimeInstant.now(),
            )

            CompositionLocalProvider(IsLoadingSheetContents provides true) {
                Column(modifier = Modifier.loadingShimmer()) {
                    TripHeaderCard(
                        trip = trip,
                        spec = TripHeaderSpec.VehicleOnTrip(vehicle, davis, rlEntry, false),
                        targetId = "",
                        route = red,
                        routeAccents = TripRouteAccents.default,
                        now = EasternTimeInstant.now(),
                    )
                }
            }
        }
    }
}
