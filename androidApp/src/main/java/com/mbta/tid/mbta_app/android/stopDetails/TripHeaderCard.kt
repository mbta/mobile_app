package com.mbta.tid.mbta_app.android.stopDetails

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mapbox.maps.extension.style.style
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.UpcomingTripView
import com.mbta.tid.mbta_app.android.component.UpcomingTripViewState
import com.mbta.tid.mbta_app.android.component.routeIcon
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripHeaderSpec
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * TODO: Add localization Accessibility Add tests verify accessibility behavior? Check behavior when
 *   stopped at stop
 */
@Composable
fun TripHeaderCard(
    tripId: String,
    spec: TripHeaderSpec?,
    targetId: String,
    routeAccents: TripRouteAccents,
    now: Instant,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.padding(vertical = 16.dp).background(colorResource(R.color.fill3)),
    ) {
        Row(
            Modifier.padding(vertical = 16.dp)
                .padding(start = 30.dp, end = 16.dp)
                .heightIn(min = 56.dp)
                .semantics(mergeDescendants = true) {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (spec != null) {
                TripMarker(spec, targetId, routeAccents)
                Description(spec, tripId, targetId, routeAccents)
                Spacer(Modifier.weight(1f))
                TripIndicator(spec, routeAccents, now)
            }
        }
    }
    /*
    .background(Color.fill3)
    .foregroundStyle(Color.text)
    .clipShape(RoundedRectangle(cornerRadius: 8))
    .padding(1)
    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.halo, lineWidth: 2))
    .padding([.horizontal], 6)
    .fixedSize(horizontal: false, vertical: true)
    .dynamicTypeSize(...DynamicTypeSize.accessibility3)
    .onTapGesture { if let onTap { onTap() } }
    .accessibilityElement(children: .combine)
    .accessibilityAddTraits(onTap != nil ? .isButton : [])
    .accessibilityAddTraits([.isHeader, .updatesFrequently])
    .accessibilityHint(onTap != nil ? NSLocalizedString(
        "displays more information",
        comment: "Screen reader hint for tapping on the trip details header on the stop page"
    ) : "")
    .accessibilityHeading(.h4)
     */
}

@Composable
private fun Description(
    spec: TripHeaderSpec,
    tripId: String,
    targetId: String,
    routeAccents: TripRouteAccents
) {
    when (spec) {
        TripHeaderSpec.FinishingAnotherTrip -> FinishingAnotherTripDescription()
        TripHeaderSpec.NoVehicle -> NoVehicleDescription()
        is TripHeaderSpec.Scheduled -> ScheduleDescription(spec.entry, targetId, routeAccents)
        is TripHeaderSpec.VehicleOnTrip ->
            VehicleDescription(spec.vehicle, spec.stop, spec.entry, tripId, targetId, routeAccents)
    }
}

@Composable
private fun FinishingAnotherTripDescription() {
    Text(
        stringResource(R.string.finishing_another_trip),
        style = MaterialTheme.typography.bodySmall
    ) // TODO footnote?
}

@Composable
private fun NoVehicleDescription() {
    Text(
        stringResource(R.string.location_not_available_yet),
        style = MaterialTheme.typography.bodySmall
    ) // TODO footnote?
}

@Composable
private fun ScheduleDescription(
    startTerminalEntry: TripDetailsStopList.Entry?,
    targetId: String,
    routeAccents: TripRouteAccents
) {
    val context = LocalContext.current
    if (startTerminalEntry != null) {
        Column(
            Modifier.semantics(mergeDescendants = true) {
                contentDescription =
                    scheduleDescriptionAccessibilityText(
                        startTerminalEntry,
                        targetId,
                        routeAccents,
                        context
                    )
            },
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                stringResource(R.string.scheduled_to_depart),
                style = MaterialTheme.typography.bodySmall
            ) // TODO footnote?
            Text(
                startTerminalEntry.stop.name,
                style = MaterialTheme.typography.headlineSmall
            ) // TODO size and bold
        }
    }
}

private fun scheduleDescriptionAccessibilityText(
    stopEntry: TripDetailsStopList.Entry,
    targetId: String,
    routeAccents: TripRouteAccents,
    context: Context
): String {
    return if (targetId == stopEntry.stop.id) {
        context.getString(
            R.string.scheduled_to_depart_selected_stop_accessibility_desc,
            routeAccents.type.typeText(context, isOnly = true),
            stopEntry.stop.name
        )
    } else {
        context.getString(
            R.string.scheduled_to_depart_accessibility_desc,
            routeAccents.type.typeText(context, isOnly = true),
            stopEntry.stop.name
        )
    }
}

@Composable
private fun VehicleDescription(
    vehicle: Vehicle,
    stop: Stop,
    stopEntry: TripDetailsStopList.Entry?,
    tripId: String,
    targetId: String,
    routeAccents: TripRouteAccents
) {
    val context = LocalContext.current
    if (vehicle.tripId == tripId) {
        Column(
            Modifier.clearAndSetSemantics {
                contentDescription =
                    vehicleDescriptionAccessibilityText(
                        vehicle,
                        stop,
                        stopEntry,
                        targetId,
                        routeAccents,
                        context
                    )
            },
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            VehicleStatusDescription(vehicle.currentStatus, stopEntry) // TODO footnote
            Text(stop.name, style = MaterialTheme.typography.headlineLarge) // TODO size and bold
        }
    }
}

private fun vehicleDescriptionAccessibilityText(
    vehicle: Vehicle,
    stop: Stop,
    stopEntry: TripDetailsStopList.Entry?,
    targetId: String,
    routeAccents: TripRouteAccents,
    context: Context
): String {
    return if (targetId == stop.id) {
        context.getString(
            R.string.vehicle_desc_accessibility_desc_selected_stop,
            routeAccents.type.typeText(context, isOnly = true),
            vehicleStatusString(context, vehicle.currentStatus, stopEntry),
            stop.name
        )
    } else {
        context.getString(
            R.string.vehicle_desc_accessibility_desc,
            routeAccents.type.typeText(context, isOnly = true),
            vehicleStatusString(context, vehicle.currentStatus, stopEntry),
            stop.name
        )
    }
}

@Composable
private fun VehicleStatusDescription(
    vehicleStatus: Vehicle.CurrentStatus,
    stopEntry: TripDetailsStopList.Entry?
) {
    val context = LocalContext.current
    Text(
        vehicleStatusString(context, vehicleStatus, stopEntry),
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun vehicleStatusString(
    context: Context,
    vehicleStatus: Vehicle.CurrentStatus,
    stopEntry: TripDetailsStopList.Entry?
): String {
    return when (vehicleStatus) {
        Vehicle.CurrentStatus.IncomingAt -> context.getString(R.string.approaching)
        Vehicle.CurrentStatus.InTransitTo -> context.getString(R.string.next_stop)
        Vehicle.CurrentStatus.StoppedAt ->
            if (stopEntry != null) {
                context.getString(R.string.waiting_to_depart)
            } else {
                context.getString(R.string.now_at)
            }
    }
}

@Composable
private fun TripMarker(spec: TripHeaderSpec, targetId: String, routeAccents: TripRouteAccents) {
    when (spec) {
        TripHeaderSpec.FinishingAnotherTrip,
        TripHeaderSpec.NoVehicle -> VehicleCircle(routeAccents)
        is TripHeaderSpec.Scheduled ->
            StopDot(routeAccents, targeted = targetId == spec.stop.id, Modifier.size(36.dp))
        is TripHeaderSpec.VehicleOnTrip ->
            VehiclePuck(spec.vehicle, spec.stop, targetId, routeAccents)
    }
}

@Composable
private fun VehicleCircle(routeAccents: TripRouteAccents) {
    Box(Modifier.size(36.dp).clearAndSetSemantics {}) {
        Box(Modifier.size(32.dp).background(routeAccents.color, CircleShape)) {
            val (icon, _) = routeIcon(routeAccents.type)
            Image(
                icon,
                null,
                Modifier.size(27.5.dp).align(Alignment.Center),
                colorFilter = ColorFilter.tint(routeAccents.textColor)
            )
        }
    }
}

@Composable
private fun VehiclePuck(
    vehicle: Vehicle,
    stop: Stop,
    targetId: String,
    routeAccents: TripRouteAccents
) {
    Box(Modifier.padding(bottom = 6.dp).clearAndSetSemantics {}) {
        Box(Modifier.padding(top = 10.dp)) {
            Box(Modifier.rotate(225f)) {
                Image(
                    painterResource(R.drawable.vehicle_halo),
                    null,
                    Modifier.size(36.dp),
                    colorFilter = ColorFilter.tint(colorResource(R.color.fill3))
                )
                Image(
                    painterResource(R.drawable.vehicle_puck),
                    null,
                    Modifier.size(32.dp),
                    colorFilter = ColorFilter.tint(routeAccents.color)
                )
            }
            val (icon, _) = routeIcon(routeAccents.type)
            Image(
                icon,
                null,
                Modifier.size(27.5.dp).align(Alignment.Center),
                colorFilter = ColorFilter.tint(routeAccents.textColor)
            )
        }

        if (targetId == stop.id && vehicle.currentStatus == Vehicle.CurrentStatus.StoppedAt) {
            Box(modifier = Modifier.align(Alignment.Center).padding(bottom = 36.dp)) {
                Image(
                    painterResource(R.drawable.stop_pin_indicator),
                    null,
                    Modifier.size(20.dp, 26.dp)
                )
            }
        }
    }
}

@Composable
private fun TripIndicator(spec: TripHeaderSpec, routeAccents: TripRouteAccents, now: Instant) {
    Column {
        when (spec) {
            TripHeaderSpec.FinishingAnotherTrip,
            TripHeaderSpec.NoVehicle -> {}
            is TripHeaderSpec.VehicleOnTrip -> LiveIndicator()
            is TripHeaderSpec.Scheduled -> {}
        }

        val upcomingTripViewState = upcomingTripViewState(spec, routeAccents, now)
        if (upcomingTripViewState != null) {
            UpcomingTripView(
                upcomingTripViewState,
                routeAccents.type,
                hideRealtimeIndicators = true
            ) // TODO color text opacity 0.6
        }
    }
}

@Composable
private fun LiveIndicator() {
    val desc = stringResource(R.string.real_time_arrivals_updating_live)
    Row(
        modifier =
            Modifier.alpha(0.6f).clearAndSetSemantics {
                heading()
                contentDescription = desc
            }
    ) {
        Image(painterResource(R.drawable.live_data), null, Modifier.size(16.dp))
        Text(
            stringResource(R.string.live),
            style = MaterialTheme.typography.bodySmall
        ) // TODO footnote
    }
}

private fun upcomingTripViewState(
    spec: TripHeaderSpec,
    routeAccents: TripRouteAccents,
    now: Instant
): UpcomingTripViewState? {
    val entry =
        when (spec) {
            is TripHeaderSpec.VehicleOnTrip -> spec.entry
            is TripHeaderSpec.Scheduled -> spec.entry
            else -> null
        }
    if (entry == null) return null
    return when (val formatted = entry.format(now, routeAccents.type)) {
        TripInstantDisplay.Hidden,
        is TripInstantDisplay.Skipped -> null
        else -> UpcomingTripViewState.Some(formatted)
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
    val trip =
        objects.trip {
            id = "1234"
            headsign = "Alewife"
        }
    val vehicle =
        objects.vehicle {
            id = "y1234"
            currentStatus = Vehicle.CurrentStatus.InTransitTo
            currentStopSequence = 30
            directionId = 1
            routeId = "66"
            stopId = "place-davis"
            tripId = trip.id
        }
    val stop = objects.stop { name = "Davis" }

    Column {
        TripHeaderCard(
            tripId = trip.id,
            spec = TripHeaderSpec.VehicleOnTrip(vehicle, stop, null),
            targetId = "",
            routeAccents = TripRouteAccents(red),
            now = Clock.System.now()
        )
    }
}
