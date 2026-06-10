package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.repositories.IDebugRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import org.koin.compose.koinInject

@Composable
fun DebugView(
    modifier: Modifier = Modifier,
    debugRepository: IDebugRepository = koinInject(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val debugMode = SettingsCache.get(Settings.DevDebugMode)
    if (!debugMode) return
    val textColor = colorResource(R.color.text)
    val color = colorResource(R.color.fill3)
    val density = LocalDensity.current

    val debugState by debugRepository.state.collectAsStateWithLifecycle()
    val now by timer(updateInterval = 1.seconds)

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
        ProvideTextStyle(Typography.footnote.copy(color = textColor)) {
            content()

            debugState?.channelUpdates?.let {
                Text("channel connections:")
                it.forEach { (key, value) ->
                    val trimmedKey = if (key.length > 25) "${key.substring(0, 25)}..." else key
                    val duration =
                        (now - value).absoluteValue.toString(DurationUnit.SECONDS, decimals = 0)
                    Text("$trimmedKey last updated $duration ago")
                }
            }
        }
    }
}
