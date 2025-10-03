package com.mbta.tid.mbta_app.android.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
fun directionNameFormatted(direction: Direction) =
    localizedDirectionNames[direction.name] ?: R.string.heading

@Composable
fun DirectionTo(
    direction: Direction,
    textColor: Color,
    onlyServingOppositeDirection: Boolean = false,
) {
    Text(
        stringResource(
            if (onlyServingOppositeDirection) R.string.only_direction_to else R.string.directionTo,
            stringResource(directionNameFormatted(direction)),
        ),
        color = textColor,
        modifier = Modifier.placeholderIfLoading(),
        style = if (onlyServingOppositeDirection) Typography.footnoteItalic else Typography.footnote,
    )
}

@Composable
fun DestinationLabel(direction: Direction, textColor: Color, pillDecoration: PillDecoration?) {
    DestinationLabel(stringResource(directionNameFormatted(direction)), textColor, pillDecoration)
}

@Composable
fun DestinationLabel(destination: String, textColor: Color, pillDecoration: PillDecoration?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pillDecoration is PillDecoration.OnDirectionDestination) {
            RoutePill(pillDecoration.route, line = null, RoutePillType.Flex)
        }
        Text(
            destination,
            color = textColor,
            modifier = Modifier.placeholderIfLoading(),
            style = Typography.bodySemibold,
        )
    }
}

@Composable
fun DirectionLabel(
    direction: Direction,
    modifier: Modifier = Modifier,
    textColor: Color = LocalContentColor.current,
    showDestination: Boolean = true,
    pillDecoration: PillDecoration? = null,
    onlyServingOppositeDirection: Boolean = false,
) {
    val destination = direction.destination
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!showDestination) {
            DirectionTo(direction, textColor, onlyServingOppositeDirection)
        } else if (destination != null) {
            DirectionTo(direction, textColor, onlyServingOppositeDirection)
            DestinationLabel(destination, textColor, pillDecoration)
        } else {
            DestinationLabel(direction, textColor, pillDecoration)
        }
    }
}
