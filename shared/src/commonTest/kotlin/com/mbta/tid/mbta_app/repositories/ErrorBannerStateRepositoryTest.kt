package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.network.INetworkConnectivityMonitor
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import dev.mokkery.MockMode
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ErrorBannerStateRepositoryTest {
    @AfterTest fun `stop koin`() = run { stopKoin() }

    @Test
    fun `initial state is null`() = runBlocking {
        val repo = ErrorBannerStateRepository()
        assertEquals(repo.state.value, null)
    }

    @Test
    fun `updates if predictions are stale`() = runBlocking {
        val repo = ErrorBannerStateRepository()

        val lastUpdated = EasternTimeInstant.now() - 3.minutes
        val action = {}

        repo.checkPredictionsStale(lastUpdated, 1, action)

        assertEquals(ErrorBannerState.StalePredictions(lastUpdated, action), repo.state.value)
    }

    @Test
    fun `data errors override stale predictions`() {
        val repo = ErrorBannerStateRepository()

        repo.checkPredictionsStale(EasternTimeInstant.now() - 3.minutes, 1) {}

        repo.setDataError("global") {}

        assertIs<ErrorBannerState.DataError>(repo.state.value)

        repo.clearDataError("global")

        assertIs<ErrorBannerState.StalePredictions>(repo.state.value)
    }

    @Test
    fun `several data errors can exist at once`() {
        val repo = ErrorBannerStateRepository()
        val actionsCalled = mutableSetOf<String>()

        repo.setDataError("a") { actionsCalled.add("a") }
        repo.setDataError("b") { actionsCalled.add("b") }
        repo.setDataError("c") { actionsCalled.add("c") }

        assertIs<ErrorBannerState.DataError>(repo.state.value)
        repo.state.value?.action?.invoke()

        assertEquals(setOf("a", "b", "c"), actionsCalled)
    }

    @Test
    fun `clears if predictions stop being stale`() = runBlocking {
        val repo = ErrorBannerStateRepository()

        repo.checkPredictionsStale(EasternTimeInstant.now() - 3.minutes, 1) {}

        assertNotNull(repo.state.value)

        repo.checkPredictionsStale(EasternTimeInstant.now(), 1) {}

        assertNull(repo.state.value)
    }

    @Test
    fun `clears if no predictions`() = runBlocking {
        val repo = ErrorBannerStateRepository()

        val lastUpdated = EasternTimeInstant.now() - 2.days

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

        val lastUpdated = EasternTimeInstant.now() - 3.minutes
        val action = {}
        repo.checkPredictionsStale(lastUpdated, 1, action)

        assertEquals(ErrorBannerState.StalePredictions(lastUpdated, action), channel.receive())

        repo.checkPredictionsStale(EasternTimeInstant.now(), 1) {}

        assertNull(channel.receive())
    }

    @Test
    fun `subscribe to connectivity changes`() {

        val mockNetworkMonitor = mock<INetworkConnectivityMonitor>(MockMode.autofill)

        startKoin { modules(module { single<INetworkConnectivityMonitor> { mockNetworkMonitor } }) }

        val repo = ErrorBannerStateRepository()

        repo.subscribeToNetworkStatusChanges()

        verify { mockNetworkMonitor.registerListener(any(), any()) }
    }
}
