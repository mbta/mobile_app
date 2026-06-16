package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.network.INetworkConnectivityMonitor
import com.mbta.tid.mbta_app.repositories.ErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.ErrorKey
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import dev.mokkery.MockMode
import dev.mokkery.mock
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

@OptIn(ExperimentalCoroutinesApi::class)
internal class ErrorBannerViewModelTest : KoinTest {

    private fun setUpKoin(
        coroutineDispatcher: CoroutineDispatcher,
        clock: Clock = Clock.System,
        mockNetworkMonitor: INetworkConnectivityMonitor = mock(MockMode.autofill),
        repositoriesBlock: MockRepositories.() -> Unit = {},
    ) {
        startKoin {
            modules(
                module {
                    single<CoroutineDispatcher>(named("coroutineDispatcherDefault")) {
                        coroutineDispatcher
                    }
                    single<INetworkConnectivityMonitor> { mockNetworkMonitor }
                    single<Clock> { clock }
                },
                repositoriesModule(
                    MockRepositories().apply {
                        useObjects(ObjectCollectionBuilder())
                        repositoriesBlock()
                    }
                ),
                viewModelModule(),
            )
        }
    }

    @AfterTest
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun reflectsRepositoryStateChangesAutomatically() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val errorRepo = ErrorBannerStateRepository()

        setUpKoin(dispatcher) { errorBanner = errorRepo }

        val viewModel: ErrorBannerViewModel = get()
        val action = {}

        testViewModelFlow(viewModel).test {
            assertEquals(ErrorBannerViewModel.State(false, null), awaitItem())

            errorRepo.setDataError(ErrorKey(setOf(), "FakeError"), "FakeDetails", action)
            val state = awaitItem().errorState
            assertEquals(setOf("FakeError"), (state as ErrorBannerState.DataError).messages)
            assertEquals(setOf("FakeDetails"), state.details)
        }
    }

    @Test
    fun testClearState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val errorRepo = ErrorBannerStateRepository()

        setUpKoin(dispatcher) { errorBanner = errorRepo }

        val viewModel: ErrorBannerViewModel = get()
        val action = {}

        testViewModelFlow(viewModel).test {
            assertEquals(ErrorBannerViewModel.State(false, null), awaitItem())
            errorRepo.setDataError(ErrorKey(setOf(), "FakeError"), "FakeDetails", action)
            val nextState = awaitItem().errorState
            assertEquals(setOf("FakeError"), (nextState as ErrorBannerState.DataError).messages)
            viewModel.clearState()
            assertEquals(ErrorBannerViewModel.State(false, null), awaitItem())
        }
    }

    @Test
    fun testSetIsLoading() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val errorRepo = ErrorBannerStateRepository()

        setUpKoin(dispatcher) { errorBanner = errorRepo }

        val viewModel: ErrorBannerViewModel = get()

        testViewModelFlow(viewModel).test {
            assertEquals(ErrorBannerViewModel.State(false, null), awaitItem())
            viewModel.setIsLoadingWhenPredictionsStale(true)
            assertEquals(ErrorBannerViewModel.State(true, null), awaitItem())
        }
    }

    @Test
    fun testPageChangeClearsState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val errorRepo = ErrorBannerStateRepository()

        setUpKoin(dispatcher) { errorBanner = errorRepo }

        val viewModel: ErrorBannerViewModel = get()
        val action = {}

        testViewModelFlow(viewModel).test {
            assertEquals(ErrorBannerViewModel.State(false, null), awaitItem())
            errorRepo.setDataError(ErrorKey(setOf(), "FakeError"), "FakeDetails", action)
            val nextState = awaitItem().errorState
            assertEquals(setOf("FakeError"), (nextState as ErrorBannerState.DataError).messages)
            assertEquals(setOf("FakeDetails"), nextState.details)
            viewModel.setSheetRoute(SheetRoutes.Favorites)
            assertEquals(ErrorBannerViewModel.State(false, null), awaitItem())
        }
    }

    @Test
    fun testPageNotChangedDoesntClearState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val errorRepo = ErrorBannerStateRepository()

        setUpKoin(dispatcher) { errorBanner = errorRepo }

        val viewModel: ErrorBannerViewModel = get()
        val action = {}

        testViewModelFlow(viewModel).test {
            assertEquals(ErrorBannerViewModel.State(false, null), awaitItem())
            viewModel.setSheetRoute(SheetRoutes.Favorites)
            advanceUntilIdle()
            errorRepo.setDataError(ErrorKey(setOf(), "FakeError"), "FakeDetails", action)
            val nextState = awaitItem().errorState
            assertEquals(setOf("FakeError"), (nextState as ErrorBannerState.DataError).messages)
            assertEquals(setOf("FakeDetails"), nextState.details)
            viewModel.setSheetRoute(SheetRoutes.Favorites)
        }
    }

    @Test
    fun testCheckPredictionsStale() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val errorRepo = ErrorBannerStateRepository()

        setUpKoin(dispatcher) { errorBanner = errorRepo }

        val viewModel: ErrorBannerViewModel = get()

        val action = {}
        val lastUpdated = EasternTimeInstant.now().minus(10.hours)

        testViewModelFlow(viewModel).test {
            assertEquals(ErrorBannerViewModel.State(false, null), awaitItem())
            viewModel.setSheetRoute(SheetRoutes.NearbyTransit)
            advanceUntilIdle()
            viewModel.checkPredictionsStale(lastUpdated, 2, SheetRoutes.NearbyTransit, action)
            awaitItemSatisfying {
                it ==
                    ErrorBannerViewModel.State(
                        false,
                        ErrorBannerState.StalePredictions(lastUpdated, action),
                    )
            }
        }
    }

    @Test
    fun testSkipsPredictionsStaleBannerWhenRouteMismatches() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val errorRepo = ErrorBannerStateRepository()

        setUpKoin(dispatcher) { errorBanner = errorRepo }

        val viewModel: ErrorBannerViewModel = get()

        val action = {}
        val lastUpdated = EasternTimeInstant.now().minus(10.hours)

        testViewModelFlow(viewModel).test {
            assertEquals(ErrorBannerViewModel.State(false, null), awaitItem())
            viewModel.setSheetRoute(SheetRoutes.StopDetails("stop1", null, null))
            advanceUntilIdle()
            viewModel.checkPredictionsStale(
                lastUpdated,
                2,
                SheetRoutes.StopDetails("stop2", null, null),
                action,
            )
            advanceUntilIdle()
            expectNoEvents()
        }
    }
}
