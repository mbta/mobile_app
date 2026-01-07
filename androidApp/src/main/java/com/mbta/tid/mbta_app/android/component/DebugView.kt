package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
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
    val textColor = colorResource(R.color.text)
    val color = colorResource(R.color.fill3)
    val density = LocalDensity.current
    Column(
        modifier
            .fillMaxWidth()
            .background(color)
            .padding(4.dp)
            .drawBehind {
                // https://stackoverflow.com/a/67039676
                drawRect(
                    textColor,
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
            .padding(8.dp)
    ) {
        ProvideTextStyle(Typography.footnote.copy(color = textColor)) { content() }
    }
}
