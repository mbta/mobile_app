package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.repositories.IDebugRepository
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import org.koin.compose.koinInject

@Composable
fun ChannelDebugView(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    DebugView(
        modifier = modifier,
        content = {
            val debugRepository: IDebugRepository = koinInject()
            val debugState by debugRepository.state.collectAsStateWithLifecycle()
            val now by timer(updateInterval = 1.seconds)

            content()
            debugState?.channelUpdates?.let {
                Text("channel connections:")
                it.entries
                    .sortedBy { it.key }
                    .forEach { (key, value) ->
                        val trimmedKey = if (key.length > 25) "${key.substring(0, 25)}..." else key
                        val duration =
                            (now - value).absoluteValue.toString(DurationUnit.SECONDS, decimals = 0)
                        Text("$trimmedKey last updated $duration ago")
                    }
            }
            Text("socket connected: ${debugState?.socketConnected}")
        },
    )
}
