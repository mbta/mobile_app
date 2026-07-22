package com.mbta.tid.mbta_app.android.component.stopCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DirectionLabel
import com.mbta.tid.mbta_app.android.component.DirectionRowView
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.HeadsignRowView
import com.mbta.tid.mbta_app.android.component.NavDrilldownRow
import com.mbta.tid.mbta_app.android.component.PillDecoration
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.component.routeCard.StopSubheader
import com.mbta.tid.mbta_app.android.component.warningIcon
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.LeafFormat
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.StopCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import org.koin.compose.koinInject

@Composable
fun StopCardContainer(
    modifier: Modifier = Modifier,
    data: StopCardData,
    departureContent: @Composable (StopCardData) -> Unit,
) {
    Column(modifier.haloContainer(1.dp).semantics { testTag = "StopCard" }) {
        StopSubheader(data.stop, data.elevatorAlerts, includeIcon = true)

        departureContent(data)
    }
}

@Composable
fun StopCard(
    data: StopCardData,
    globalData: GlobalResponse?,
    now: EasternTimeInstant,
    isFavorite: (RouteStopDirection) -> Boolean?,
    onOpenStopDetails: (String, StopDetailsFilter) -> Unit,
) {
    StopCardContainer(
        data = data,
        departureContent = {
            Departures(
                it,
                globalData,
                now,
                { routeStopDirection -> isFavorite(routeStopDirection) },
            ) { leaf ->
                onOpenStopDetails(
                    it.stop.id,
                    StopDetailsFilter(leaf.lineOrRoute.id, leaf.direction.id),
                )
            }
        },
    )
}

@Composable
fun Departures(
    stopData: StopCardData,
    globalData: GlobalResponse?,
    now: EasternTimeInstant,
    isFavorite: (RouteStopDirection) -> Boolean?,
    analytics: Analytics = koinInject(),
    onClick: (RouteCardData.Leaf) -> Unit,
) {
    Column {
        stopData.data.withIndex().forEach { (index, leaf) ->
            fun analyticsTappedDeparture(leafFormat: LeafFormat) {
                val format = (leafFormat as? LeafFormat.Single)?.format
                val noTrips = (format as? UpcomingFormat.NoTrips)?.noTripsFormat
                analytics.tappedDeparture(
                    leaf.lineOrRoute.id,
                    stopData.stop.id,
                    isFavorite(leaf.routeStopDirection) ?: false,
                    leaf.alertsHere().isNotEmpty(),
                    leaf.lineOrRoute.type,
                    noTrips,
                )
            }

            val formatted = leaf.format(now, globalData)
            val direction = leaf.direction

            NavDrilldownRow(
                onClick = {
                    onClick(leaf)
                    analyticsTappedDeparture(formatted)
                },
                onClickLabel = stringResource(R.string.open_for_more_arrivals),
                modifier = Modifier.padding(vertical = 10.dp).padding(start = 8.dp, end = 16.dp),
            ) { modifier ->
                val branchedNoRoutePills =
                    formatted is LeafFormat.Branched &&
                        formatted.branchRows.all { it.route == null }
                if (branchedNoRoutePills) {
                    Row(Modifier.padding(end = 8.dp)) {
                        RoutePill(
                            (leaf.lineOrRoute as? LineOrRoute.Route)?.route,
                            (leaf.lineOrRoute as? LineOrRoute.Line)?.line,
                            RoutePillType.Fixed,
                            warningAlertIconName = formatted.warningAlert?.iconName,
                        )
                    }
                }
                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                ) {
                    when {
                        formatted is LeafFormat.Single -> {
                            DirectionRowView(
                                direction.copy(
                                    destination = formatted.headsign ?: direction.destination
                                ),
                                formatted.format,
                                pillDecoration =
                                    PillDecoration.OnRow(
                                        formatted.route ?: leaf.lineOrRoute.sortRoute
                                    ),
                            )
                        }
                        formatted is LeafFormat.Branched -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                formatted.warningAlert?.let { warningAlert ->
                                    if (!branchedNoRoutePills) {
                                        warningIcon(
                                            warningAlert.iconName,
                                            modifier =
                                                Modifier.placeholderIfLoading().padding(end = 8.dp),
                                        )
                                    }
                                }
                                DirectionLabel(
                                    direction,
                                    showDestination = false,
                                    routeNamePrefix =
                                        leaf.lineOrRoute.name.takeIf { !branchedNoRoutePills },
                                )
                            }
                            for (branch in formatted.branchRows) {
                                HeadsignRowView(
                                    branch.headsign,
                                    branch.format,
                                    pillDecoration = branch.route?.let { PillDecoration.OnRow(it) },
                                )
                            }
                        }
                    }
                }
            }

            if (index < stopData.data.lastIndex) {
                HaloSeparator()
            }
        }
    }
}
