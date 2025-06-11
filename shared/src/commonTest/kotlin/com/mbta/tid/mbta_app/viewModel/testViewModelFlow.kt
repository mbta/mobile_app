package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope

/** Gets a state flow from a [MoleculeViewModel] in the context of a [TestScope] from `runTest`. */
fun <Event, Model> TestScope.testViewModelFlow(viewModel: MoleculeViewModel<Event, Model>) =
    viewModel.modelsForUnitTests(this, TestFrameClock(this.testScheduler))

private class TestFrameClock(private val scheduler: TestCoroutineScheduler) : MonotonicFrameClock {
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        scheduler.advanceTimeBy(1L)
        return onFrame(scheduler.currentTime * 1_000_000L)
    }
}
