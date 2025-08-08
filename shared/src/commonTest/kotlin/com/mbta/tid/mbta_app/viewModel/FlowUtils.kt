package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.MonotonicFrameClock
import app.cash.turbine.ReceiveTurbine
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Gets a state flow from a [MoleculeViewModel] in the context of a [TestScope] from `runTest`. */
internal fun <Event, Model> TestScope.testViewModelFlow(
    viewModel: MoleculeViewModel<Event, Model>
) = viewModel.modelsForUnitTests(this, TestFrameClock(this.testScheduler))

private class TestFrameClock(private val scheduler: TestCoroutineScheduler) : MonotonicFrameClock {
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        scheduler.advanceTimeBy(1L)
        return onFrame(scheduler.currentTime * 1_000_000L)
    }
}

suspend fun <T> ReceiveTurbine<T>.awaitItemSatisfying(
    timeout: Duration = 2.seconds,
    predicate: (T) -> Boolean,
): T {
    val itemsSkipped = mutableListOf<T>()
    while (true) {
        val item: T =
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeoutOrNull(timeout) { awaitItem() }
            } ?: break
        if (predicate(item)) {
            return item
        } else {
            itemsSkipped.add(item)
        }
    }
    fail("Did not receive satisfying item after ${timeout}, skipped over $itemsSkipped")
}
