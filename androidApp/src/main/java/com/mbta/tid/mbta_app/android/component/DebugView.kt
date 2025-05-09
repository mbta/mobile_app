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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.repositories.Settings

@Composable
fun DebugView(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val debugMode = SettingsCache.get(Settings.DevDebugMode)
    if (!debugMode) return
    val color = colorResource(R.color.text)
    val density = LocalDensity.current
    Column(
        modifier
            .drawBehind {
                // https://stackoverflow.com/a/67039676
                drawRect(
                    color,
                    style =
                        Stroke(
                            width = with(density) { 2.dp.toPx() },
                            pathEffect =
                                PathEffect.dashPathEffect(
                                    with(density) { floatArrayOf(10.dp.toPx(), 10.dp.toPx()) }
                                ),
                        ),
                )
            }
            .padding(4.dp)
    ) {
        ProvideTextStyle(Typography.footnote) { content() }
    }
}
