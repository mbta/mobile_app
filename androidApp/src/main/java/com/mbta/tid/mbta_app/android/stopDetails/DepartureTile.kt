package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.stopDetailsPage.TileData
import kotlin.time.Clock

@Composable
fun DepartureTile(
    data: TileData,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    showRoutePill: Boolean = false,
    showHeadsign: Boolean = true,
    isSelected: Boolean = false,
) {
    Column(
        modifier
            .clickable(
                onClickLabel =
                    if (isSelected) null
                    else stringResource(R.string.departure_tile_on_click_label),
                onClick = onTap,
            )
            .heightIn(min = 56.dp)
            .width(IntrinsicSize.Max)
            .sizeIn(maxWidth = 195.dp)
            .fillMaxHeight()
            .haloContainer(
                borderWidth = 2.dp,
                outlineColor = if (isSelected) colorResource(R.color.halo) else Color.Transparent,
                backgroundColor =
                    if (isSelected) colorResource(R.color.fill3)
                    else colorResource(R.color.deselected_toggle_2).copy(alpha = 0.6f),
                borderRadius = 8.dp,
            )
            .padding(10.dp)
            .semantics {
                if (isSelected) {
                    heading()
                    selected = true
                }
            },
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides
                if (isSelected) colorResource(R.color.text)
                else colorResource(R.color.deselected_toggle_text)
        ) {
            val headsign = data.headsign.takeIf { showHeadsign }
            if (headsign != null) {
                Text(headsign, textAlign = TextAlign.Start, style = Typography.footnoteSemibold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (showRoutePill) {
                    Box(Modifier.padding(end = 8.dp)) {
                        RoutePill(data.route, type = RoutePillType.Flex)
                    }
                }
                TripStatus(predictions = data.formatted)
            }
        }
    }
}

@Preview
@Composable
private fun DepartureTilePreview() {
    val objects = ObjectCollectionBuilder()
    val route1 = objects.route()
    val routeB =
        objects.route {
            color = "00843D"
            longName = "Green Line B"
            shortName = "B"
            textColor = "FFFFFF"
            type = RouteType.LIGHT_RAIL
        }

    val trip1 = objects.trip()
    val schedule1 = objects.schedule { trip = trip1 }
    val trip2 = objects.trip()
    val trip3 = objects.trip()

    MyApplicationTheme {
        Row(
            Modifier.background(Color.fromHex("00843D"))
                .padding(16.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DepartureTile(
                TileData(
                    route1,
                    "Framingham",
                    UpcomingFormat.Some(
                        listOf(
                            UpcomingFormat.Some.FormattedTrip(
                                UpcomingTrip(trip1),
                                RouteType.COMMUTER_RAIL,
                                TripInstantDisplay.TimeWithStatus(
                                    Clock.System.now(),
                                    "Delay",
                                    headline = true,
                                ),
                            )
                        ),
                        secondaryAlert = null,
                    ),
                    objects.upcomingTrip(schedule1),
                ),
                onTap = {},
                showHeadsign = false,
                isSelected = true,
            )
            DepartureTile(
                TileData(
                    route1,
                    "Harvard",
                    UpcomingFormat.Some(
                        listOf(
                            UpcomingFormat.Some.FormattedTrip(
                                UpcomingTrip(trip2),
                                RouteType.BUS,
                                TripInstantDisplay.Minutes(9),
                            )
                        ),
                        secondaryAlert = null,
                    ),
                    objects.upcomingTrip(schedule1),
                ),
                onTap = {},
                showHeadsign = true,
            )
            DepartureTile(
                TileData(
                    routeB,
                    "Really long headsign that should be broken onto multiple lines",
                    UpcomingFormat.Some(
                        listOf(
                            UpcomingFormat.Some.FormattedTrip(
                                UpcomingTrip(trip3),
                                RouteType.LIGHT_RAIL,
                                TripInstantDisplay.Minutes(12),
                            )
                        ),
                        secondaryAlert = null,
                    ),
                    objects.upcomingTrip(schedule1),
                ),
                onTap = {},
                showRoutePill = true,
                showHeadsign = true,
            )
        }
    }
}
