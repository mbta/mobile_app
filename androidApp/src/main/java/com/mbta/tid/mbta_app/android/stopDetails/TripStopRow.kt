package com.mbta.tid.mbta_app.android.stopDetails

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.component.UpcomingTripView
import com.mbta.tid.mbta_app.android.component.UpcomingTripViewState
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Composable
fun TripStopRow(
    stop: TripDetailsStopList.Entry,
    now: Instant,
    routeAccents: TripRouteAccents,
    modifier: Modifier = Modifier,
    targeted: Boolean = false,
    firstStop: Boolean = false,
    lastStop: Boolean = false
) {
    val context = LocalContext.current
    Column(modifier) {
        Box(contentAlignment = Alignment.BottomCenter) {
            if (!lastStop && !targeted) {
                HaloSeparator()
            }
            Column {
                Row(
                    Modifier.semantics(mergeDescendants = true) {
                        if (targeted) {
                            heading()
                        }
                    }
                ) {
                    Text(
                        stop.stop.name,
                        Modifier.semantics {
                            contentDescription =
                                stopAccessibilityLabel(stop, targeted, firstStop, context)
                        },
                        color = colorResource(R.color.text),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.weight(1f))
                    CompositionLocalProvider(
                        LocalContentColor provides colorResource(R.color.text)
                    ) {
                        UpcomingTripView(
                            upcomingTripViewState(stop, now, routeAccents),
                            Modifier.alpha(0.6f),
                            routeType = routeAccents.type,
                            hideRealtimeIndicators = true
                        )
                    }
                }

                if (stop.routes.isNotEmpty()) {
                    ScrollRoutes(
                        stop,
                        Modifier.semantics {
                            contentDescription = scrollRoutesAccessibilityLabel(stop, context)
                        }
                    )
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
    Row(modifier.horizontalScroll(rememberScrollState())) {
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
    val alert = stop.alert
    return if (alert != null) {
        UpcomingTripViewState.Disruption(alert.effect)
    } else {
        UpcomingTripViewState.Some(stop.format(now, routeAccents.type))
    }
}

@Preview
@Composable
private fun TripStopRowPreview() {
    val objects = ObjectCollectionBuilder()
    TripStopRow(
        stop =
            TripDetailsStopList.Entry(
                objects.stop { name = "ABC" },
                stopSequence = 10,
                alert = null,
                schedule = null,
                prediction = null,
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
        TripRouteAccents.default.copy(type = RouteType.HEAVY_RAIL)
    )
}
