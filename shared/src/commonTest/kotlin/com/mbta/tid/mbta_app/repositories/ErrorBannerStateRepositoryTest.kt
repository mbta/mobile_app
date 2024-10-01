package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.ErrorBannerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock

class ErrorBannerStateRepositoryTest {
    @Test
    fun `initial state is null`() = runBlocking {
        val repo = ErrorBannerStateRepository()
        assertEquals(repo.state.value, null)
    }

    @Test
    fun `updates if predictions are stale`() = runBlocking {
        val repo = ErrorBannerStateRepository()

        val lastUpdated = Clock.System.now() - 2.minutes
        val action = {}

        repo.checkPredictionsStale(lastUpdated, 1, action)

        assertEquals(ErrorBannerState.StalePredictions(lastUpdated, action), repo.state.value)
    }

    @Test
    fun `clears if predictions stop being stale`() = runBlocking {
        val repo = ErrorBannerStateRepository()

        repo.checkPredictionsStale(Clock.System.now() - 2.minutes, 1) {}

        assertNotNull(repo.state.value)

        repo.checkPredictionsStale(Clock.System.now(), 1) {}

        assertNull(repo.state.value)
    }

    @Test
    fun `clears if no predictions`() = runBlocking {
        val repo = ErrorBannerStateRepository()

        val lastUpdated = Clock.System.now() - 2.days

        repo.checkPredictionsStale(lastUpdated, 1) {}

        assertNotNull(repo.state.value)

        repo.checkPredictionsStale(lastUpdated, 0) {}

        assertNull(repo.state.value)
    }

    @Test
    fun `streams in flow`() = runBlocking {
        val repo = ErrorBannerStateRepository()

        // pass events from the flow back into this channel so we can assert as we update
        val channel = Channel<ErrorBannerState?>(capacity = Channel.RENDEZVOUS)

        launch { repo.state.take(3).collect(channel::send) }

        assertEquals(null, channel.receive())

        val lastUpdated = Clock.System.now() - 2.minutes
        val action = {}
        repo.checkPredictionsStale(lastUpdated, 1, action)

        assertEquals(ErrorBannerState.StalePredictions(lastUpdated, action), channel.receive())

        repo.checkPredictionsStale(Clock.System.now(), 1) {}

        assertNull(channel.receive())
    }
}
