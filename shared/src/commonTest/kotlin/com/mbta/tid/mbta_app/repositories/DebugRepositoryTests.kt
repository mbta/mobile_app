package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.mocks.MockClock
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.TestTimeSource
import kotlinx.coroutines.runBlocking
import org.koin.core.context.stopKoin

class DebugRepositoryTests {
    @AfterTest fun `stop koin`() = run { stopKoin() }

    @Test
    fun `initial state is null`() = runBlocking {
        val repo = DebugRepository()
        assertNull(repo.state.value)
    }

    @Test
    fun `updates with topic success`() = runBlocking {
        val now = EasternTimeInstant.now()
        val clock = MockClock(now.instant, TestTimeSource())
        val repo = DebugRepository(clock = clock)

        repo.setChannelSuccess("topic")

        assertEquals(mapOf("topic" to now), repo.state.value?.channelUpdates)
    }

    @Test
    fun `clears topic`() = runBlocking {
        val now = EasternTimeInstant.now()
        val clock = MockClock(now.instant, TestTimeSource())
        val repo = DebugRepository(clock = clock)

        repo.setChannelSuccess("topic1")
        repo.setChannelSuccess("topic2")

        assertEquals(mapOf("topic1" to now, "topic2" to now), repo.state.value?.channelUpdates)

        repo.clearChannelStatus("topic1")

        assertEquals(mapOf("topic2" to now), repo.state.value?.channelUpdates)
    }

    @Test
    fun `setSocketConnected toggling`() = runBlocking {
        val now = EasternTimeInstant.now()
        val clock = MockClock(now.instant, TestTimeSource())
        val repo = DebugRepository(clock = clock)

        assertFalse(repo.state.value?.socketConnected!!)
        repo.setSocketConnected(true)
        assertTrue(repo.state.value?.socketConnected!!)
        repo.setSocketConnected(false)
        assertFalse(repo.state.value?.socketConnected!!)
    }
}
