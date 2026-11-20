package com.mbta.tid.mbta_app.android.routeStop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.component.TightWrapText
import com.mbta.tid.mbta_app.android.component.TrackChangePill
import com.mbta.tid.mbta_app.android.component.TrackChangePillStyle
import com.mbta.tid.mbta_app.android.component.TrackChangePillType
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TrackChange
import com.mbta.tid.mbta_app.model.TripInstantDisplay
import com.mbta.tid.mbta_app.model.UpcomingFormat
import com.mbta.tid.mbta_app.model.UpcomingTrip
import com.mbta.tid.mbta_app.model.routeStopPage.TileData
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.minutes

@Composable
fun DepartureTile(
    data: TileData,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    showRoutePill: Boolean = false,
    showHeadsign: Boolean = true,
    isSelected: Boolean = false,
) {
    DepartureTileContainer(
        onTap,
        stringResource(R.string.departure_tile_on_click_label),
        modifier,
        isSelected,
    ) {
        val headsign = data.headsign.takeIf { showHeadsign }
        if (headsign != null) {
            Text(headsign, textAlign = TextAlign.Start, style = Typography.footnoteSemibold)
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            when (val trackChange = data.upcoming.trackChange) {
                is TrackChange.Alert -> {
                    TrackChangePill(
                        pillType = TrackChangePillType.Alert,
                        pillStyle = TrackChangePillStyle.Compact(),
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }

                is TrackChange.Scheduled -> {
                    TrackChangePill(
                        pillType =
                            TrackChangePillType.Scheduled(
                                trackChange.platformCode,
                                trackChange.platformName,
                            ),
                        pillStyle = TrackChangePillStyle.Compact(isDimmed = !isSelected),
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }

                null -> {}
            }
            if (showRoutePill) {
                Box(Modifier.padding(end = 8.dp)) {
                    RoutePill(data.route, type = RoutePillType.Flex)
                }
            }
            TripStatus(predictions = data.formatted)
        }
    }
}

/**
 * Shown after the last [DepartureTile] when there are trips scheduled later than the last predicted
 * trip, but we don’t have predictions for them yet.
 */
@Composable
fun PredictionsNotYetAvailableTile(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    val iconId = "infoIcon"
    val textColor =
        if (isSelected) colorResource(R.color.text)
        else colorResource(R.color.route_color_contrast_text)

    DepartureTileContainer(
        onTap,
        stringResource(R.string.predictions_not_available_yet_on_click_label),
        modifier,
        isSelected,
    ) {
        Row(
            modifier = Modifier.widthIn(max = 112.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TightWrapText(
                text =
                    buildAnnotatedString {
                        append(stringResource(R.string.predictions_not_available_yet))
                        appendInlineContent(iconId)
                    },
                color = textColor,
                style = Typography.footnote,
                inlineContent =
                    mapOf(
                        iconId to
                            InlineTextContent(
                                placeholder =
                                    Placeholder(
                                        16.sp,
                                        16.sp,
                                        PlaceholderVerticalAlign.Center,
                                    ),
                            ) {
                                Icon(
                                    painterResource(R.drawable.fa_circle_info),
                                    contentDescription = null,
                                    Modifier.size(16.dp).padding(start = 5.dp),
                                    tint = textColor.copy(alpha = 0.5f),
                                )
                            },
                    ),
            )
        }
    }
}

@Composable
private fun DepartureTileContainer(
    onTap: () -> Unit,
    onClickLabel: String,
    modifier: Modifier,
    isSelected: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clickable(
                onClickLabel = onClickLabel.takeUnless { isSelected },
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
                    else colorResource(R.color.route_color_contrast),
                borderRadius = 8.dp,
            )
            .padding(vertical = 10.dp, horizontal = 8.dp)
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
                else colorResource(R.color.route_color_contrast_text),
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun DepartureTilePreview() {
    val objects = ObjectCollectionBuilder("DepartureTilePreview")
    val route1 = objects.route()
    val routeB = objects.route {
        id = "Green-B"
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
                .padding(horizontal = 10.dp)
                .height(IntrinsicSize.Min),
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
                                    EasternTimeInstant.now(),
                                    "Delay",
                                    last = false,
                                    headline = true,
                                ),
                                lastTrip = false,
                                null,
                            ),
                        ),
                        warningAlert = null,
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
                                TripInstantDisplay.Minutes(9, false),
                                lastTrip = false,
                                null,
                            ),
                        ),
                        warningAlert = null,
                    ),
                    objects.upcomingTrip(schedule1),
                ),
                onTap = {},
                showHeadsign = true,
            )
            DepartureTile(
                TileData(
                    route1,
                    "Framingham",
                    UpcomingFormat.Some(
                        listOf(
                            UpcomingFormat.Some.FormattedTrip(
                                UpcomingTrip(trip1),
                                RouteType.COMMUTER_RAIL,
                                TripInstantDisplay.ScheduleTimeWithStatusColumn(
                                    EasternTimeInstant.now() - 15.minutes,
                                    "Delay",
                                    last = false,
                                    headline = true,
                                ),
                                lastTrip = false,
                                null,
                            ),
                        ),
                        warningAlert = null,
                    ),
                    objects.upcomingTrip(schedule1),
                ),
                onTap = {},
                showHeadsign = false,
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
                                TripInstantDisplay.Minutes(12, false),
                                false,
                                null,
                            ),
                        ),
                        warningAlert = null,
                    ),
                    objects.upcomingTrip(schedule1),
                ),
                onTap = {},
                showRoutePill = true,
                showHeadsign = true,
            )
            PredictionsNotYetAvailableTile(
                onTap = {},
                isSelected = false,
            )
        }
    }
}
