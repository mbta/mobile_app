package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.ErrorBannerViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ErrorBannerTests {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testRespondsToState() {
        val errorRepo = MockErrorBannerStateRepository(state = ErrorBannerState.NetworkError {})
        val viewModel = ErrorBannerViewModel(errorRepo, MockSentryRepository(), Clock.System)

        composeTestRule.setContent { ErrorBanner(viewModel) }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Unable to connect"))

        errorRepo.mutableFlow.tryEmit(ErrorBannerState.DataError(emptySet(), emptySet(), {}))

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Error loading data"))

        errorRepo.mutableFlow.tryEmit(
            ErrorBannerState.StalePredictions(
                lastUpdated = EasternTimeInstant.now().minus(2.minutes),
                action = {},
            )
        )

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Updated 2 minutes ago"))
    }

    @Test
    fun testNetworkError() {
        val networkErrorRepo =
            MockErrorBannerStateRepository(state = ErrorBannerState.NetworkError {})
        val networkErrorVM =
            ErrorBannerViewModel(networkErrorRepo, MockSentryRepository(), Clock.System)

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
        val dataErrorVM = ErrorBannerViewModel(dataErrorRepo, MockSentryRepository(), Clock.System)
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
        val staleVM = ErrorBannerViewModel(staleRepo, MockSentryRepository(), Clock.System)
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
        val staleVM = ErrorBannerViewModel(staleRepo, MockSentryRepository(), Clock.System)
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
        val staleVM = ErrorBannerViewModel(staleRepo, MockSentryRepository(), Clock.System)
        staleVM.setIsLoadingWhenPredictionsStale(true)
        composeTestRule.setContent { ErrorBanner(staleVM) }

        composeTestRule.onNodeWithText("Updated 2 minutes ago").assertDoesNotExist()
    }
}
