package com.mbta.tid.mbta_app.android.stopDetails

import androidx.annotation.ColorRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DirectionLabel
import com.mbta.tid.mbta_app.android.util.StopDetailsFilter
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType

@ColorRes
fun deselectedBackgroundColor(route: Route): Int =
    if (route.type == RouteType.COMMUTER_RAIL || route.id == "Blue") R.color.deselected_toggle_2
    else R.color.deselected_toggle_1

@Composable
fun DirectionPicker(patternsByStop: PatternsByStop, filterState: MutableState<StopDetailsFilter?>) {
    var filter by filterState
    val availableDirections = patternsByStop.patterns.map { it.directionId() }.distinct().sorted()
    val directions = patternsByStop.directions
    val route = patternsByStop.representativeRoute
    val line = patternsByStop.line

    if (availableDirections.size > 1) {
        val deselectedBackgroundColor = colorResource(deselectedBackgroundColor(route))
        Row(
            Modifier.padding(2.dp).background(deselectedBackgroundColor, RoundedCornerShape(6.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (direction in availableDirections) {
                val isSelected = filter?.directionId == direction
                val action = {
                    filter =
                        StopDetailsFilter(routeId = line?.id ?: route.id, directionId = direction)
                }

                Button(
                    onClick = action,
                    shape = RoundedCornerShape(6.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (isSelected) Color.fromHex(route.color)
                                else deselectedBackgroundColor,
                            contentColor =
                                if (isSelected) Color.fromHex(route.textColor)
                                else colorResource(R.color.deselected_toggle_text)
                        )
                ) {
                    DirectionLabel(
                        direction = directions[(direction)],
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
