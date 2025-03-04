package com.mbta.tid.mbta_app.android.stopDetails

import androidx.annotation.ColorRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DirectionLabel
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.RealtimePatterns
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.StopDetailsFilter

@ColorRes private fun deselectedBackgroundColor(route: Route): Int = R.color.deselected_toggle_2

@Composable
fun DirectionPicker(
    patternsByStop: PatternsByStop,
    filter: StopDetailsFilter?,
    updateStopFilter: (StopDetailsFilter?) -> Unit,
    modifier: Modifier = Modifier
) {
    val availableDirections = patternsByStop.patterns.map { it.directionId() }.distinct().sorted()
    val directions = patternsByStop.directions
    val route = patternsByStop.representativeRoute
    val line = patternsByStop.line

    if (availableDirections.size > 1) {
        val deselectedBackgroundColor =
            colorResource(deselectedBackgroundColor(route)).copy(alpha = 0.6f)
        TabRow(
            modifier =
                modifier
                    .padding(horizontal = 2.dp)
                    .background(
                        deselectedBackgroundColor.copy(alpha = 0.6f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(2.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp)),
            selectedTabIndex = filter?.directionId ?: 0,
            indicator = {},
            divider = {}
        ) {
            for (direction in availableDirections) {
                val isSelected = filter?.directionId == direction
                val action = {
                    updateStopFilter(
                        StopDetailsFilter(routeId = line?.id ?: route.id, directionId = direction)
                    )
                }

                Tab(
                    selected = isSelected,
                    onClick = action,
                    modifier =
                        Modifier.fillMaxHeight()
                            .background(Color.fromHex(route.color))
                            .background(deselectedBackgroundColor)
                            .background(
                                if (isSelected) Color.fromHex(route.color) else Color.Transparent,
                                shape = if (isSelected) RoundedCornerShape(6.dp) else RectangleShape
                            )
                            .clip(RoundedCornerShape(6.dp)),
                    selectedContentColor = Color.fromHex(route.textColor),
                    unselectedContentColor = colorResource(R.color.deselected_toggle_text)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        DirectionLabel(direction = directions[(direction)])
                    }
                }
            }
        }
    } else if (availableDirections.size == 1) {
        availableDirections.firstOrNull()?.let {
            DirectionLabel(
                direction = directions[it],
                modifier.padding(horizontal = 8.dp).fillMaxWidth().semantics(
                    mergeDescendants = true
                ) {
                    heading()
                },
                textColor = Color.fromHex(route.textColor)
            )
        }
    }
}

@Preview
@Composable
private fun DirectionPickerPreview() {
    val objects = ObjectCollectionBuilder()
    val stop = objects.stop()
    val route =
        objects.route {
            color = "FFC72C"
            textColor = "000000"
        }
    val patternOutbound = objects.routePattern(route) { directionId = 0 }
    val patternInbound = objects.routePattern(route) { directionId = 1 }
    MyApplicationTheme {
        DirectionPicker(
            patternsByStop =
                PatternsByStop(
                    routes = listOf(route),
                    line = null,
                    stop,
                    patterns =
                        listOf(
                            RealtimePatterns.ByHeadsign(
                                route = route,
                                headsign = "Out",
                                line = null,
                                patterns = listOf(patternOutbound),
                                upcomingTrips = emptyList()
                            ),
                            RealtimePatterns.ByHeadsign(
                                route = route,
                                headsign = "In",
                                line = null,
                                patterns = listOf(patternInbound),
                                upcomingTrips = emptyList()
                            ),
                        ),
                    directions =
                        listOf(
                            Direction(name = "Outbound", destination = "Out", id = 0),
                            Direction(name = "Inbound", destination = "In", id = 1),
                        ),
                    elevatorAlerts = emptyList()
                ),
            filter = StopDetailsFilter(routeId = route.id, directionId = 0),
            updateStopFilter = {}
        )
    }
}
