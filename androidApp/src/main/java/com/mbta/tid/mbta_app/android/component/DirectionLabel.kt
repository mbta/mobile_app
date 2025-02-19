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
fun DirectionLabel(
    direction: Direction,
    modifier: Modifier = Modifier,
    textColor: Color = LocalContentColor.current
) {
    val destination = direction.destination
    Column(modifier = modifier) {
        if (destination != null) {
            Text(
                stringResource(
                    R.string.directionTo,
                    stringResource(directionNameFormatted(direction))
                ),
                color = textColor,
                modifier = Modifier.placeholderIfLoading(),
                style = Typography.footnote
            )
            Text(
                destination,
                color = textColor,
                modifier = Modifier.placeholderIfLoading(),
                style = Typography.bodySemibold
            )
        } else {
            Text(
                stringResource(directionNameFormatted(direction)),
                color = textColor,
                modifier = Modifier.placeholderIfLoading(),
                style = Typography.bodySemibold
            )
        }
    }
}
