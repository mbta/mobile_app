package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugView(
    modifier: Modifier = Modifier,
    debugRepository: IDebugRepository = koinInject(),
    content: @Composable ColumnScope.() -> Unit,
    details: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val debugMode = SettingsCache.get(Settings.DevDebugMode)
    if (!debugMode) return
    var showDetails by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val textColor = colorResource(R.color.text)
    val color = colorResource(R.color.fill3)
    val density = LocalDensity.current

    val debugState by debugRepository.state.collectAsStateWithLifecycle()
    val now by timer(updateInterval = 1.seconds)

    Row(
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
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            ProvideTextStyle(Typography.footnote.copy(color = textColor)) {
                content()
                debugState?.channelUpdates?.let {
                    Text("channel connections:")
                    it.entries
                        .sortedBy { it.key }
                        .forEach { (key, value) ->
                            val trimmedKey =
                                if (key.length > 25) "${key.substring(0, 25)}..." else key
                            val duration =
                                (now - value)
                                    .absoluteValue
                                    .toString(DurationUnit.SECONDS, decimals = 0)
                            Text("$trimmedKey last updated $duration ago")
                        }
                }
            }
        }
        details?.let {
            IconButton(onClick = { showDetails = true }, modifier = Modifier.weight(0.25f)) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "Show details")
            }
        }
    }
    if (showDetails && details != null) {
        ModalBottomSheet(onDismissRequest = { showDetails = false }, sheetState = sheetState) {
            SheetContent(details) {
                scope.launch { sheetState.hide() }.invokeOnCompletion { showDetails = false }
            }
        }
    }
}

@Composable
private fun SheetContent(details: @Composable ColumnScope.() -> Unit, onClose: () -> Unit) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp).verticalScroll(scrollState)) {
        Row {
            Text("Debug Details", fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Button(onClick = onClose) { Text("Dismiss") }
        }
        ProvideTextStyle(Typography.footnote.copy(color = colorResource(R.color.text))) {
            details()
        }
    }
}
