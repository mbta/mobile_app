package com.mbta.tid.mbta_app.android.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.repositories.Settings

@Composable
fun AccordionDebugView(
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val debugMode = SettingsCache.get(Settings.DevDebugMode)
    if (!debugMode) return
    val textColor = colorResource(R.color.text)
    val color = colorResource(R.color.fill3)
    val density = LocalDensity.current
    var expanded by remember { mutableStateOf(false) }
    val degrees by animateFloatAsState(if (expanded) 90f else 0f)
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
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = expanded.not() },
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ProvideTextStyle(Typography.footnote.copy(color = textColor)) { header() }
            Icon(
                painterResource(R.drawable.fa_chevron_right),
                null,
                Modifier.rotate(degrees).size(16.dp),
                textColor,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Box(Modifier.fillMaxWidth()) {
                ProvideTextStyle(Typography.footnote.copy(color = textColor)) { content() }
            }
        }
    }
}
