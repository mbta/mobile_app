package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.R

@Composable
fun DebugView(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val color = colorResource(R.color.text)
    Column(
        modifier
            .drawBehind {
                // https://stackoverflow.com/a/67039676
                drawRect(
                    color,
                    style =
                        Stroke(
                            width = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                )
            }
            .padding(4.dp)
    ) {
        ProvideTextStyle(TextStyle(fontSize = 13.sp)) { content() }
    }
}
