package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.MonotonicFrameClock
import kotlin.coroutines.resume
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.suspendCancellableCoroutine

public actual abstract class MoleculeScopeViewModel actual constructor() {
    internal actual val scope: CoroutineScope =
        CoroutineScope(MainScope().coroutineContext + AnimationFrameClock())

    private class AnimationFrameClock : MonotonicFrameClock {
        val startTime = TimeSource.Monotonic.markNow()

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
            suspendCancellableCoroutine { continuation ->
                @Suppress("UnusedVariable")
                val gotFrame = {
                    val frameNanos = startTime.elapsedNow().inWholeNanoseconds
                    continuation.resume(onFrame(frameNanos))
                }
                @Suppress("UnusedVariable") val frame = js("requestAnimationFrame(gotFrame)")
                continuation.invokeOnCancellation { js("cancelAnimationFrame(frame)") }
            }
    }
}
