package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import org.koin.compose.koinInject

@Composable
fun DebugView(
    modifier: Modifier = Modifier,
    settingsRepo: ISettingsRepository = koinInject(),
    content: @Composable ColumnScope.() -> Unit
) {
    var settings by remember { mutableStateOf<Map<Settings, Boolean>>(emptyMap()) }
    LaunchedEffect(null) { settings = settingsRepo.getSettings() }
    if (settings[Settings.DevDebugMode] != true) return
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
                                )
                        )
                )
            }
            .padding(4.dp)
    ) {
        ProvideTextStyle(TextStyle(fontSize = 13.sp)) { content() }
    }
}
