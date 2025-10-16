package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.StopListContext
import com.mbta.tid.mbta_app.android.component.StopListRow
import com.mbta.tid.mbta_app.android.component.StopPlacement
import com.mbta.tid.mbta_app.android.component.UpcomingTripView
import com.mbta.tid.mbta_app.android.component.UpcomingTripViewState
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.containsWrappableText
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.DestinationPredictionBalance
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteBranchSegment
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SegmentAlertState
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.minutes
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@Composable
fun TripStopRow(
    stop: TripDetailsStopList.Entry,
    trip: Trip,
    now: EasternTimeInstant,
    onTapLink: (TripDetailsStopList.Entry) -> Unit,
    onOpenAlertDetails: (Alert) -> Unit,
    route: Route,
    routeAccents: TripRouteAccents,
    alertSummaries: Map<String, AlertSummary?>,
    modifier: Modifier = Modifier,
    showDownstreamAlert: Boolean = false,
    targeted: Boolean = false,
    firstStop: Boolean = false,
    lastStop: Boolean = false,
) {
    val activeElevatorAlerts = stop.activeElevatorAlerts(now)

    val disruption = stop.disruption?.takeIf { it.alert.hasNoThroughService && showDownstreamAlert }

    StopListRow(
        stop = stop.stop,
        stopLane = RouteBranchSegment.Lane.Center,
        stickConnections =
            RouteBranchSegment.StickConnection.forward(
                "".takeUnless { firstStop },
                stop.stop.id,
                "".takeUnless { lastStop },
                RouteBranchSegment.Lane.Center,
            ),
        onClick = { onTapLink(stop) },
        routeAccents = routeAccents,
        stopListContext = StopListContext.Trip,
        modifier = modifier,
        activeElevatorAlerts = activeElevatorAlerts.size,
        alertSummaries = alertSummaries,
        connectingRoutes = stop.routes,
        disruption = disruption,
        getAlertState = { fromStop, toStop ->
            when {
                fromStop == stop.stop.id &&
                    showDownstreamAlert &&
                    disruption?.alert?.effect == Alert.Effect.Shuttle -> SegmentAlertState.Shuttle
                else -> SegmentAlertState.Normal
            }
        },
        stopPlacement = StopPlacement(firstStop, lastStop),
        onOpenAlertDetails = onOpenAlertDetails,
        targeted = targeted,
        trackNumber = stop.trackNumber,
        rightSideContent = { rightSideModifier ->
            CompositionLocalProvider(LocalContentColor provides colorResource(R.color.text)) {
                val state = upcomingTripViewState(stop, trip, now, route)
                if (state != null) {
                    UpcomingTripView(
                        state,
                        rightSideModifier.then(
                            DestinationPredictionBalance.predictionWidth(
                                state.containsWrappableText()
                            )
                        ),
                        routeType = routeAccents.type,
                        hideRealtimeIndicators = true,
                        maxTextAlpha = 0.6f,
                    )
                }
            }
        },
    )
}

private fun upcomingTripViewState(
    stop: TripDetailsStopList.Entry,
    trip: Trip,
    now: EasternTimeInstant,
    route: Route,
): UpcomingTripViewState? {
    return when (val formatted = stop.format(trip, now, route)) {
        is UpcomingFormat.Some ->
            UpcomingTripViewState.Some(formatted.trips.singleOrNull()?.format ?: return null)
        is UpcomingFormat.Disruption ->
            UpcomingTripViewState.Disruption(
                FormattedAlert(formatted.alert),
                iconName = formatted.iconName,
            )
        else -> null
    }
}

@Preview
@Composable
private fun TripStopRowPreview() {
    val koin = koinApplication {
        modules(module { single { SettingsCache(MockSettingsRepository()) } })
    }
    val objects = ObjectCollectionBuilder("TripStopRowPreview")
    val trip = objects.trip()
    val now = EasternTimeInstant.now()
    val red =
        objects.route {
            color = "DA291C"
            longName = "Red Line"
            textColor = "FFFFFF"
            type = RouteType.HEAVY_RAIL
        }
    val redAccents = TripRouteAccents(red)
    KoinContext(koin.koin) {
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
                            vehicle = null,
                            routes =
                                listOf(
                                    red,
                                    objects.route {
                                        longName = "Green Line"
                                        color = "00843D"
                                        textColor = "FFFFFF"
                                    },
                                ),
                        ),
                    trip,
                    now,
                    onTapLink = {},
                    onOpenAlertDetails = {},
                    red,
                    redAccents,
                    alertSummaries = emptyMap(),
                )
                TripStopRow(
                    stop =
                        TripDetailsStopList.Entry(
                            objects.stop { name = "Park Street" },
                            stopSequence = 10,
                            disruption = null,
                            schedule = null,
                            prediction = objects.prediction { departureTime = now.plus(5.minutes) },
                            vehicle = null,
                            routes =
                                listOf(
                                    red,
                                    objects.route {
                                        longName = "Green Line"
                                        color = "00843D"
                                        textColor = "FFFFFF"
                                    },
                                ),
                        ),
                    trip,
                    now,
                    onTapLink = {},
                    onOpenAlertDetails = {},
                    red,
                    redAccents,
                    alertSummaries = emptyMap(),
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
                                ),
                        ),
                    trip,
                    now,
                    onTapLink = {},
                    onOpenAlertDetails = {},
                    objects.route { type = RouteType.COMMUTER_RAIL },
                    TripRouteAccents.default.copy(
                        type = RouteType.COMMUTER_RAIL,
                        color = Color.fromHex("DA291C"),
                    ),
                    alertSummaries = emptyMap(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun TripStopRowDisruptionsPreview() {
    val koin = koinApplication {
        modules(module { single { SettingsCache(MockSettingsRepository()) } })
    }
    val objects = ObjectCollectionBuilder("TripStopRowDisruptionsPreview")
    val trip = objects.trip()
    val now = EasternTimeInstant.now()
    val red =
        objects.route {
            color = "DA291C"
            longName = "Red Line"
            textColor = "FFFFFF"
            type = RouteType.HEAVY_RAIL
        }
    val redAccents = TripRouteAccents(red)
    KoinContext(koin.koin) {
        MyApplicationTheme {
            Box {
                Box(
                    Modifier.matchParentSize()
                        .padding(6.dp)
                        .background(colorResource(R.color.fill3))
                )
                Column(Modifier.padding(top = 6.dp)) {
                    TripStopRow(
                        stop =
                            TripDetailsStopList.Entry(
                                objects.stop { name = "Charles/MGH" },
                                stopSequence = 10,
                                disruption =
                                    UpcomingFormat.Disruption(
                                        objects.alert { effect = Alert.Effect.StopClosure },
                                        mapStopRoute = MapStopRoute.RED,
                                    ),
                                schedule = null,
                                prediction = objects.prediction { status = "Stopped 5 stops away" },
                                vehicle = null,
                                routes =
                                    listOf(
                                        red,
                                        objects.route {
                                            longName = "Green Line"
                                            color = "00843D"
                                            textColor = "FFFFFF"
                                        },
                                    ),
                            ),
                        trip,
                        now,
                        onTapLink = {},
                        onOpenAlertDetails = {},
                        red,
                        redAccents,
                        alertSummaries = emptyMap(),
                        showDownstreamAlert = true,
                    )
                    TripStopRow(
                        stop =
                            TripDetailsStopList.Entry(
                                objects.stop { name = "Park Street" },
                                stopSequence = 10,
                                disruption = null,
                                schedule = null,
                                prediction =
                                    objects.prediction { departureTime = now.plus(5.minutes) },
                                vehicle = null,
                                routes =
                                    listOf(
                                        red,
                                        objects.route {
                                            longName = "Green Line"
                                            color = "00843D"
                                            textColor = "FFFFFF"
                                        },
                                    ),
                            ),
                        trip,
                        now,
                        onTapLink = {},
                        onOpenAlertDetails = {},
                        red,
                        redAccents,
                        alertSummaries = emptyMap(),
                        showDownstreamAlert = true,
                    )
                    TripStopRow(
                        stop =
                            TripDetailsStopList.Entry(
                                objects.stop { name = "South Station" },
                                stopSequence = 10,
                                disruption =
                                    UpcomingFormat.Disruption(
                                        objects.alert { effect = Alert.Effect.Shuttle },
                                        MapStopRoute.RED,
                                    ),
                                schedule = null,
                                prediction =
                                    objects.prediction { departureTime = now.plus(5.minutes) },
                                predictionStop = objects.stop { platformCode = "1" },
                                vehicle = null,
                                routes = emptyList(),
                                elevatorAlerts =
                                    listOf(
                                        objects.alert {
                                            activePeriod(
                                                now.minus(20.minutes),
                                                now.plus(20.minutes),
                                            )
                                        }
                                    ),
                            ),
                        trip,
                        now,
                        onTapLink = {},
                        onOpenAlertDetails = {},
                        objects.route { type = RouteType.COMMUTER_RAIL },
                        TripRouteAccents.default.copy(
                            type = RouteType.COMMUTER_RAIL,
                            color = Color.fromHex("DA291C"),
                        ),
                        alertSummaries = emptyMap(),
                        showDownstreamAlert = true,
                    )
                }
            }
        }
    }
}
