package com.mbta.tid.mbta_app.android.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.Direction

private val localizedDirectionNames: Map<String, Int> =
    mapOf(
        "North" to R.string.northbound,
        "South" to R.string.southbound,
        "East" to R.string.eastbound,
        "West" to R.string.westbound,
        "Inbound" to R.string.inbound,
        "Outbound" to R.string.outbound,
    )

@StringRes
private fun directionNameFormatted(direction: Direction) =
    localizedDirectionNames[direction.name] ?: R.string.heading

@Composable
fun DirectionTo(direction: Direction, textColor: Color) {
    Text(
        stringResource(R.string.directionTo, stringResource(directionNameFormatted(direction))),
        color = textColor,
        modifier = Modifier.placeholderIfLoading(),
        style = Typography.footnote
    )
}

@Composable
fun DestinationLabel(direction: Direction, textColor: Color) {
    DestinationLabel(stringResource(directionNameFormatted(direction)), textColor)
}

@Composable
fun DestinationLabel(destination: String, textColor: Color) {
    Text(
        destination,
        color = textColor,
        modifier = Modifier.placeholderIfLoading(),
        style = Typography.bodySemibold
    )
}

@Composable
fun DirectionLabel(
    direction: Direction,
    modifier: Modifier = Modifier,
    textColor: Color = LocalContentColor.current,
    showDestination: Boolean = true
) {
    val destination = direction.destination
    Column(modifier = modifier) {
        if (!showDestination) {
            DirectionTo(direction, textColor)
        } else if (destination != null) {
            DirectionTo(direction, textColor)
            DestinationLabel(destination, textColor)
        } else {
            DestinationLabel(direction, textColor)
        }
    }
}
