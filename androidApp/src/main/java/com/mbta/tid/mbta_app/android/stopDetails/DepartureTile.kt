package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripInstantDisplay

@Composable
fun DepartureTile(
    data: TileData,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    showRoutePill: Boolean = false,
    showHeadsign: Boolean = true,
    isSelected: Boolean = false
) {
    Column(
        modifier
            .clickable(
                onClickLabel =
                    if (isSelected) null
                    else stringResource(R.string.departure_tile_on_click_label),
                onClick = onTap
            )
            .heightIn(min = 56.dp)
            .haloContainer(
                borderWidth = 2.dp,
                outlineColor = if (isSelected) colorResource(R.color.halo) else Color.Transparent,
                backgroundColor =
                    if (isSelected) colorResource(R.color.fill3)
                    else colorResource(R.color.deselected_toggle_2).copy(alpha = 0.6f),
                borderRadius = 8.dp
            )
            .padding(10.dp)
            .semantics {
                if (isSelected) {
                    heading()
                    selected = true
                }
            },
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides
                if (isSelected) colorResource(R.color.text)
                else colorResource(R.color.deselected_toggle_text)
        ) {
            // If you can figure out a better way to make the pill left aligned to the text and the
            // status right aligned to the text whether the text is wider or narrower than the pill
            // + status, please do.
            val density = LocalDensity.current
            var textWidth by remember { mutableStateOf(0.dp) }

            if (showHeadsign) {
                Text(
                    data.headsign,
                    modifier =
                        Modifier.onSizeChanged { textWidth = with(density) { (it.width).toDp() } },
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Row(
                Modifier.widthIn(min = textWidth),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
        Row(Modifier.background(Color.fromHex("00843D")).padding(16.dp)) {
            DepartureTile(
                TileData(
                    trip1.id,
                    route1,
                    "Nubian",
                    RealtimePatterns.Format.Some(
                        listOf(
                            RealtimePatterns.Format.Some.FormatWithId(
                                trip1.id,
                                RouteType.BUS,
                                TripInstantDisplay.Minutes(5)
                            )
                        ),
                        secondaryAlert = null
                    ),
                    objects.upcomingTrip(schedule1)
                ),
                onTap = {},
                showHeadsign = false,
                isSelected = true
            )
            DepartureTile(
                TileData(
                    trip2.id,
                    route1,
                    "Harvard",
                    RealtimePatterns.Format.Some(
                        listOf(
                            RealtimePatterns.Format.Some.FormatWithId(
                                trip2.id,
                                RouteType.BUS,
                                TripInstantDisplay.Minutes(9)
                            )
                        ),
                        secondaryAlert = null
                    ),
                    objects.upcomingTrip(schedule1)
                ),
                onTap = {},
                showHeadsign = true
            )
            DepartureTile(
                TileData(
                    trip3.id,
                    routeB,
                    "Government Center",
                    RealtimePatterns.Format.Some(
                        listOf(
                            RealtimePatterns.Format.Some.FormatWithId(
                                trip3.id,
                                RouteType.LIGHT_RAIL,
                                TripInstantDisplay.Minutes(12)
                            )
                        ),
                        secondaryAlert = null
                    ),
                    objects.upcomingTrip(schedule1)
                ),
                onTap = {},
                showRoutePill = true,
                showHeadsign = true
            )
        }
    }
}
