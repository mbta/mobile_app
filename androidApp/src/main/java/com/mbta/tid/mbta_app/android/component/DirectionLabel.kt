package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.model.Direction

private val reformatDirectionNames = setOf("North", "South", "East", "West")

private fun directionNameFormatted(direction: Direction) =
    if (reformatDirectionNames.contains(direction.name)) "${direction.name}bound"
    else direction.name

@Composable
fun DirectionLabel(direction: Direction) {
    val destination = direction.destination
    Column {
        if (destination != null) {
            Text("${directionNameFormatted(direction)} to", fontSize = 13.sp)
            Text(destination, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        } else {
            Text(
                directionNameFormatted(direction),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
