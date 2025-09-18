package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.network.INetworkConnectivityMonitor
import com.mbta.tid.mbta_app.repositories.ErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.throwsErrorWith
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifyNoMoreCalls
import io.sentry.kotlin.multiplatform.Scope
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
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

            errorRepo.setDataError("FakeError", "FakeDetails", action)
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
            errorRepo.setDataError("FakeError", "FakeDetails", action)
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
            errorRepo.setDataError("FakeError", "FakeDetails", action)
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
            errorRepo.setDataError("FakeError", "FakeDetails", action)
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
            viewModel.checkPredictionsStale(lastUpdated, 2, action)
            assertEquals(
                ErrorBannerViewModel.State(
                    false,
                    ErrorBannerState.StalePredictions(lastUpdated, action),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `records recurring DataErrors in Sentry`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val errorRepo = ErrorBannerStateRepository()
        val sentryScope = mock<Scope>(MockMode.autofill)
        val sentryRepo =
            mock<ISentryRepository>(MockMode.autofill) {
                every { captureMessage(any(), any()) } calls
                    { (_: String, additionalDetails: Scope.() -> Unit) ->
                        sentryScope.additionalDetails()
                    }
            }

        val clock =
            object : Clock {
                var now = Clock.System.now()

                override fun now() = now
            }

        setUpKoin(dispatcher, clock) {
            errorBanner = errorRepo
            sentry = sentryRepo
        }

        val viewModel: ErrorBannerViewModel = get()

        testViewModelFlow(viewModel).test {
            assertEquals(ErrorBannerViewModel.State(false, null), awaitItem())
            errorRepo.setDataError("ErrorKey", "error details") {}
            var nextState = awaitItem().errorState
            assertEquals(setOf("ErrorKey"), (nextState as ErrorBannerState.DataError).messages)
            assertEquals(setOf("error details"), nextState.details)
            clock.now += 1.seconds
            val clearedAt = clock.now
            viewModel.clearState()
            assertEquals(ErrorBannerViewModel.State(false, null), awaitItem())
            clock.now += 1.seconds
            errorRepo.setDataError("ErrorKey", "error details") {}
            nextState = awaitItem().errorState
            assertEquals(setOf("ErrorKey"), (nextState as ErrorBannerState.DataError).messages)
            assertEquals(setOf("error details"), nextState.details)
            verify { sentryRepo.captureMessage("Recurring DataError [ErrorKey]", any()) }
            verify {
                sentryScope.addBreadcrumb(
                    Breadcrumb(
                        level = SentryLevel.ERROR,
                        type = "error",
                        message = "Recurring DataError",
                        category = null,
                        data =
                            mutableMapOf(
                                "previousClearedError.keys" to setOf("ErrorKey"),
                                "previousClearedError.details" to setOf("error details"),
                                "previousClearedError.clearedAt" to EasternTimeInstant(clearedAt),
                                "newErrorState.messages" to setOf("ErrorKey"),
                                "newErrorState.details" to setOf("error details"),
                            ),
                    )
                )
            }
        }
    }

    @Test
    fun `does not record DataErrors if over a minute since cleared`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val errorRepo = ErrorBannerStateRepository()
        val sentryRepo =
            mock<ISentryRepository>(MockMode.autofill) {
                every { captureMessage(any(), any()) } throwsErrorWith
                    "should not have captured Sentry message"
            }

        val clock =
            object : Clock {
                var now = Clock.System.now()

                override fun now() = now
            }

        setUpKoin(dispatcher, clock) {
            errorBanner = errorRepo
            sentry = sentryRepo
        }

        val viewModel: ErrorBannerViewModel = get()

        testViewModelFlow(viewModel).test {
            assertEquals(ErrorBannerViewModel.State(false, null), awaitItem())
            errorRepo.setDataError("ErrorKey", "error details") {}
            assertIs<ErrorBannerState.DataError>(awaitItem().errorState)
            viewModel.clearState()
            assertEquals(ErrorBannerViewModel.State(false, null), awaitItem())
            clock.now += 2.minutes
            errorRepo.setDataError("ErrorKey", "error details") {}
            assertIs<ErrorBannerState.DataError>(awaitItem().errorState)
            verifyNoMoreCalls(sentryRepo)
        }
    }

    @Test
    fun `does not record DataErrors if offline`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val errorRepo = ErrorBannerStateRepository()
        val sentryRepo =
            mock<ISentryRepository>(MockMode.autofill) {
                every { captureMessage(any(), any()) } throwsErrorWith
                    "should not have captured Sentry message"
            }

        val clock =
            object : Clock {
                var now = Clock.System.now()

                override fun now() = now
            }

        val mockNetworkMonitor =
            object : INetworkConnectivityMonitor {
                override fun registerListener(
                    onNetworkAvailable: () -> Unit,
                    onNetworkLost: () -> Unit,
                ) {
                    onNetworkLost()
                }
            }

        setUpKoin(dispatcher, clock, mockNetworkMonitor) {
            errorBanner = errorRepo
            sentry = sentryRepo
        }

        val viewModel: ErrorBannerViewModel = get()

        testViewModelFlow(viewModel).test {
            assertEquals(ErrorBannerViewModel.State(false, null), awaitItem())
            errorRepo.setDataError("ErrorKey", "error details") {}
            assertIs<ErrorBannerState.NetworkError>(awaitItem().errorState)
            viewModel.clearState()
            assertEquals(ErrorBannerViewModel.State(false, null), awaitItem())
            clock.now += 1.seconds
            errorRepo.setDataError("ErrorKey", "error details") {}
            assertIs<ErrorBannerState.NetworkError>(awaitItem().errorState)
            verifyNoMoreCalls(sentryRepo)
        }
    }
}
