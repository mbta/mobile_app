package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripHeaderSpec
import kotlinx.datetime.Instant

@Composable
fun TripStops(
    targetId: String,
    stops: TripDetailsStopList,
    stopSequence: Int?,
    headerSpec: TripHeaderSpec?,
    now: Instant,
    global: GlobalResponse?,
    routeAccents: TripRouteAccents
) {
    val context = LocalContext.current

    val splitStops: TripDetailsStopList.TargetSplit? =
        remember(targetId, stops, stopSequence, global) {
            if (stopSequence != null && global != null) {
                stops.splitForTarget(targetId, stopSequence, global, combinedStopDetails = true)
            } else null
        }

    var stopsExpanded by rememberSaveable { mutableStateOf(false) }

    val routeTypeText = routeAccents.type.typeText(context, isOnly = true)
    val stopsAway = splitStops?.collapsedStops?.size
    val target = splitStops?.targetStop
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

    Column(
        Modifier.border(2.dp, colorResource(R.color.halo), shape = RoundedCornerShape(8.dp))
            .padding(1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colorResource(R.color.fill2))
    ) {
        if (splitStops != null && target != null) {
            if (showFirstStopSeparately) {
                val firstStop = splitStops.firstStop
                if (firstStop != null) {
                    TripStopRow(stop = firstStop, now, routeAccents, firstStop = true)
                }
            }
            if (splitStops.collapsedStops.isNotEmpty() && stopsAway != null) {
                Row(
                    Modifier.height(IntrinsicSize.Min)
                        .clickable(
                            onClickLabel =
                                if (stopsExpanded) stringResource(R.string.hides_remaining_stops)
                                else stringResource(R.string.lists_remaining_stops)
                        ) {
                            stopsExpanded = !stopsExpanded
                        }
                        .semantics {
                            contentDescription =
                                context.getString(
                                    R.string.is_stops_away_from,
                                    routeTypeText,
                                    stopsAway,
                                    target.stop.name
                                )
                        }
                        .padding(horizontal = 8.dp)
                        .defaultMinSize(minHeight = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        if (stopsExpanded) {
                            Icon(
                                painterResource(R.drawable.fa_caret_right),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp).rotate(90f),
                                tint = colorResource(R.color.deemphasized)
                            )
                            ColoredRouteLine(
                                routeAccents.color,
                                Modifier.padding(start = 14.dp, end = 18.dp).fillMaxHeight()
                            )
                        } else {
                            Icon(
                                painterResource(R.drawable.fa_caret_right),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = colorResource(R.color.deemphasized)
                            )
                            RouteLineTwist(
                                routeAccents.color,
                                Modifier.padding(start = 2.dp, end = 6.dp)
                            )
                        }
                    }
                    Text(
                        pluralStringResource(R.plurals.stops_away, stopsAway, stopsAway),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (stopsExpanded) {
                    Column {
                        Box(Modifier.height(IntrinsicSize.Min)) {
                            HaloSeparator()
                            // Lil 1x4 pt route color bar to maintain an unbroken route color line
                            // over the separator
                            ColoredRouteLine(
                                routeAccents.color,
                                Modifier.padding(start = 42.dp).fillMaxHeight()
                            )
                        }
                        StopList(
                            list = splitStops.collapsedStops,
                            lastStopSequence,
                            now,
                            routeAccents
                        )
                    }
                }
            }
            if (!hideTarget) {
                // If the target is the first stop and there's no vehicle, it's already displayed in
                // the trip header
                TripStopRow(
                    stop = target,
                    now,
                    routeAccents,
                    targeted = true,
                    firstStop = showFirstStopSeparately && target == stops.startTerminalEntry,
                    modifier =
                        Modifier.background(colorResource(R.color.fill3))
                            .border(2.dp, colorResource(R.color.halo))
                )
            }
            StopList(splitStops.followingStops, lastStopSequence, now, routeAccents)
        } else {
            StopList(stops.stops, lastStopSequence, now, routeAccents)
        }
    }
}

@Composable
private fun StopList(
    list: List<TripDetailsStopList.Entry>,
    lastStopSequence: Int?,
    now: Instant,
    routeAccents: TripRouteAccents
) {
    for (stop in list) {
        TripStopRow(stop, now, routeAccents, lastStop = stop.stopSequence == lastStopSequence)
    }
}
