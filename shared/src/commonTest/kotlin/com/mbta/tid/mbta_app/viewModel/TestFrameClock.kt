package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.MonotonicFrameClock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.datetime.Clock

class TestFrameClock(refreshRateHz: Int = 1000) : MonotonicFrameClock {
    private val frames =
        flow {
                val startTime = Clock.System.now()
                while (true) {
                    val now = Clock.System.now()
                    val time = now.minus(startTime).inWholeNanoseconds
                    emit(time)
                    delay(1.seconds / refreshRateHz)
                }
            }
            .shareIn(CoroutineScope(Dispatchers.Default + SupervisorJob()), SharingStarted.Lazily)

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        return frames.first().let(onFrame)
    }
}
