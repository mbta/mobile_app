package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.ErrorBannerViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.channels.BufferOverflow
import org.junit.Rule
import org.junit.Test

class ErrorBannerTests {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testRespondsToState() {
        val errorRepo = MockErrorBannerStateRepository(state = ErrorBannerState.NetworkError {})
        val viewModel =
            ErrorBannerViewModel(
                errorRepo,
                MockSentryRepository(),
                Clock.System,
                onEventBufferOverflow = BufferOverflow.SUSPEND,
            )

        composeTestRule.setContent { ErrorBanner(viewModel) }

        composeTestRule.onNodeWithText("Unable to connect").assertExists()

        errorRepo.mutableFlow.tryEmit(ErrorBannerState.DataError(emptySet(), emptySet(), {}))

        composeTestRule.onNodeWithText("Error loading data").assertExists()

        errorRepo.mutableFlow.tryEmit(
            ErrorBannerState.StalePredictions(
                lastUpdated = EasternTimeInstant.now().minus(2.minutes),
                action = {},
            )
        )

        composeTestRule.onNodeWithText("Updated 2 minutes ago").assertExists()
    }

    @Test
    fun testNetworkError() {
        val networkErrorRepo =
            MockErrorBannerStateRepository(state = ErrorBannerState.NetworkError {})
        val networkErrorVM =
            ErrorBannerViewModel(
                networkErrorRepo,
                MockSentryRepository(),
                Clock.System,
                onEventBufferOverflow = BufferOverflow.SUSPEND,
            )

        composeTestRule.setContent { ErrorBanner(networkErrorVM) }

        composeTestRule.onNodeWithText("Unable to connect").assertExists()
    }

    @Test
    fun testDataError() {
        val dataErrorRepo =
            MockErrorBannerStateRepository(
                state =
                    ErrorBannerState.DataError(
                        messages = emptySet(),
                        details = emptySet(),
                        action = {},
                    )
            )
        val dataErrorVM =
            ErrorBannerViewModel(
                dataErrorRepo,
                MockSentryRepository(),
                Clock.System,
                onEventBufferOverflow = BufferOverflow.SUSPEND,
            )
        composeTestRule.setContent { ErrorBanner(dataErrorVM) }

        composeTestRule.onNodeWithText("Error loading data").assertExists()
    }

    @Test
    fun testPluralPredictionsStale() {
        val staleRepo =
            MockErrorBannerStateRepository(
                state =
                    ErrorBannerState.StalePredictions(
                        lastUpdated = EasternTimeInstant.now().minus(2.minutes),
                        action = {},
                    )
            )
        val staleVM =
            ErrorBannerViewModel(
                staleRepo,
                MockSentryRepository(),
                Clock.System,
                onEventBufferOverflow = BufferOverflow.SUSPEND,
            )
        composeTestRule.setContent { ErrorBanner(staleVM) }

        composeTestRule.onNodeWithText("Updated 2 minutes ago").assertExists()
    }

    @Test
    fun testSinglePredictionsStale() {
        val staleRepo =
            MockErrorBannerStateRepository(
                state =
                    ErrorBannerState.StalePredictions(
                        lastUpdated = EasternTimeInstant.now().minus(1.minutes),
                        action = {},
                    )
            )
        val staleVM =
            ErrorBannerViewModel(
                staleRepo,
                MockSentryRepository(),
                Clock.System,
                onEventBufferOverflow = BufferOverflow.SUSPEND,
            )
        composeTestRule.setContent { ErrorBanner(staleVM) }

        composeTestRule.onNodeWithText("Updated 1 minute ago").assertExists()
    }

    @Test
    fun testLoadingWhenPredictionsStale() {
        val staleRepo =
            MockErrorBannerStateRepository(
                state =
                    ErrorBannerState.StalePredictions(
                        lastUpdated = EasternTimeInstant.now().minus(2.minutes),
                        action = {},
                    )
            )
        val staleVM =
            ErrorBannerViewModel(
                staleRepo,
                MockSentryRepository(),
                Clock.System,
                onEventBufferOverflow = BufferOverflow.SUSPEND,
            )
        staleVM.setIsLoadingWhenPredictionsStale(true)
        composeTestRule.setContent { ErrorBanner(staleVM) }

        composeTestRule.onNodeWithText("Updated 2 minutes ago").assertDoesNotExist()
    }
}
