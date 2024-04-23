package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Composable
fun timer(updateInterval: Duration): Instant {
    var now by remember { mutableStateOf(Clock.System.now()) }

    DisposableEffect(key1 = null) {
        val timer =
            fixedRateTimer(initialDelay = 0, period = updateInterval.inWholeMilliseconds) {
                now = Clock.System.now()
            }
        onDispose { timer.cancel() }
    }

    return now
}
