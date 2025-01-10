package com.mbta.tid.mbta_app.android.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.R
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
fun DirectionLabel(direction: Direction, modifier: Modifier = Modifier) {
    val destination = direction.destination
    Column(modifier = modifier) {
        if (destination != null) {
            Text(
                stringResource(
                    R.string.directionTo,
                    stringResource(directionNameFormatted(direction))
                ),
                fontSize = 13.sp,
                modifier = Modifier.placeholderIfLoading()
            )
            Text(
                destination,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.placeholderIfLoading()
            )
        } else {
            Text(
                stringResource(directionNameFormatted(direction)),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.placeholderIfLoading()
            )
        }
    }
}
