package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DirectionLabel
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route

@Composable
fun DirectionPicker(
    availableDirections: List<Int>,
    directions: List<Direction>,
    route: Route,
    selectedDirectionId: Int?,
    updateDirectionId: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (availableDirections.size > 1) {
        val deselectedBackgroundColor =
            colorResource(R.color.deselected_toggle_2)
                .copy(alpha = 0.6f)
                .compositeOver(Color.fromHex(route.color))

        TabRow(
            modifier =
                modifier
                    .haloContainer(2.dp, deselectedBackgroundColor, Color.Transparent, 6.dp)
                    .fillMaxWidth(),
            containerColor = deselectedBackgroundColor,
            selectedTabIndex = selectedDirectionId ?: 0,
            indicator = {},
            divider = {},
        ) {
            for (direction in availableDirections) {
                val isSelected = selectedDirectionId == direction
                val selectedDirection = directions[(direction)]
                val action = { updateDirectionId(direction) }

                Tab(
                    selected = isSelected,
                    onClick = action,
                    modifier =
                        Modifier.fillMaxHeight()
                            .haloContainer(
                                0.dp,
                                Color.Transparent,
                                if (isSelected) Color.fromHex(route.color) else Color.Transparent,
                                6.dp,
                            ),
                    selectedContentColor = Color.fromHex(route.textColor),
                    unselectedContentColor = colorResource(R.color.deselected_toggle_text),
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(8.dp),
                        if (selectedDirection.destination == null) Arrangement.Center
                        else Arrangement.Start,
                        Alignment.CenterVertically,
                    ) {
                        DirectionLabel(direction = selectedDirection)
                    }
                }
            }
        }
    } else if (availableDirections.size == 1) {
        availableDirections.firstOrNull()?.let {
            DirectionLabel(
                direction = directions[it],
                modifier.padding(horizontal = 12.dp).fillMaxWidth().semantics(
                    mergeDescendants = true
                ) {
                    heading()
                },
                textColor = Color.fromHex(route.textColor),
            )
        }
    }
}

@Preview
@Composable
private fun DirectionPickerPreview() {
    val objects = ObjectCollectionBuilder()
    val red =
        objects.route {
            color = "DA291C"
            textColor = "FFFFFF"
        }
    val bus =
        objects.route {
            color = "FFC72C"
            textColor = "000000"
        }
    val green =
        objects.route {
            color = "00843D"
            textColor = "FFFFFF"
        }
    MyApplicationTheme {
        Column {
            DirectionPicker(
                availableDirections = listOf(0, 1),
                directions =
                    listOf(
                        Direction(name = "South", destination = "Ashmont/Braintree", id = 0),
                        Direction(name = "North", destination = "Alewife", id = 1),
                    ),
                route = red,
                selectedDirectionId = 0,
                updateDirectionId = {},
            )
            DirectionPicker(
                availableDirections = listOf(0, 1),
                directions =
                    listOf(
                        Direction(
                            name = "Outbound",
                            destination = "Fields Corner or St Peter’s Square",
                            id = 0,
                        ),
                        Direction(name = "Inbound", destination = "Ruggles Station", id = 1),
                    ),
                route = bus,
                selectedDirectionId = 1,
                updateDirectionId = {},
            )
            DirectionPicker(
                availableDirections = listOf(0, 1),
                directions =
                    listOf(
                        Direction(name = "West", destination = null, id = 0),
                        Direction(name = "East", destination = null, id = 1),
                    ),
                route = green,
                selectedDirectionId = 0,
                updateDirectionId = {},
            )
        }
    }
}
