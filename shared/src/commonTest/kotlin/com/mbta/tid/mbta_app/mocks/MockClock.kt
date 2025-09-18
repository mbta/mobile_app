@file:OptIn(ExperimentalCoroutinesApi::class)

package com.mbta.tid.mbta_app.mocks

import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.TimeSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource

class MockClock(private val startInstant: Instant, timeSource: TimeSource.WithComparableMarks) :
    Clock {
    val startTime = EasternTimeInstant(startInstant)
    private val startMark = timeSource.markNow()

    override fun now() = startInstant + startMark.elapsedNow()
}

@Suppress("TestFunctionName")
fun TestScope.MockClock(startTime: Instant = Clock.System.now()) =
    MockClock(startTime, testTimeSource)

class MockClockTest {
    @Test
    fun `advances with test time`() = runTest {
        val clock = MockClock()
        val nowBefore = clock.now()
        advanceTimeBy(10.seconds)
        val nowAfter = clock.now()
        assertEquals(10.seconds, nowAfter - nowBefore)
    }
}
