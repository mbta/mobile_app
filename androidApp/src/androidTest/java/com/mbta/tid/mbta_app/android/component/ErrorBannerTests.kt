package com.mbta.tid.mbta_app.android.component

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test

class ErrorBannerTests {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testRespondsToState() {
        val errorRepo = MockErrorBannerStateRepository(state = ErrorBannerState.NetworkError {})
        val viewModel = ErrorBannerViewModel(false, errorRepo, MockSettingsRepository())

        composeTestRule.setContent {
            LaunchedEffect(null) { viewModel.activate() }
            ErrorBanner(viewModel)
        }

        composeTestRule.onNodeWithText("Unable to connect").assertExists()

        errorRepo.mutableFlow.tryEmit(ErrorBannerState.DataError(emptySet(), {}))

        composeTestRule.onNodeWithText("Error loading data").assertExists()

        errorRepo.mutableFlow.tryEmit(
            ErrorBannerState.StalePredictions(
                lastUpdated = Clock.System.now().minus(2.minutes),
                action = {}
            )
        )

        composeTestRule.onNodeWithText("Updated 2 minutes ago").assertExists()
    }

    @Test
    fun testNetworkError() {
        val networkErrorRepo =
            MockErrorBannerStateRepository(state = ErrorBannerState.NetworkError {})
        val networkErrorVM = ErrorBannerViewModel(false, networkErrorRepo, MockSettingsRepository())

        composeTestRule.setContent {
            LaunchedEffect(null) { networkErrorVM.activate() }
            ErrorBanner(networkErrorVM)
        }

        composeTestRule.onNodeWithText("Unable to connect").assertExists()
    }

    @Test
    fun testDataError() {
        val dataErrorRepo =
            MockErrorBannerStateRepository(
                state = ErrorBannerState.DataError(messages = emptySet(), action = {})
            )
        val dataErrorVM = ErrorBannerViewModel(false, dataErrorRepo, MockSettingsRepository())
        composeTestRule.setContent {
            LaunchedEffect(null) { dataErrorVM.activate() }
            ErrorBanner(dataErrorVM)
        }

        composeTestRule.onNodeWithText("Error loading data").assertExists()
    }

    @Test
    fun testPredictionsStale() {
        val staleRepo =
            MockErrorBannerStateRepository(
                state =
                    ErrorBannerState.StalePredictions(
                        lastUpdated = Clock.System.now().minus(2.minutes),
                        action = {}
                    )
            )
        val staleVM = ErrorBannerViewModel(false, staleRepo, MockSettingsRepository())
        composeTestRule.setContent {
            LaunchedEffect(null) { staleVM.activate() }
            ErrorBanner(staleVM)
        }

        composeTestRule.onNodeWithText("Updated 2 minutes ago").assertExists()
    }

    @Test
    fun testLoadingWhenPredictionsStale() {
        val staleRepo =
            MockErrorBannerStateRepository(
                state =
                    ErrorBannerState.StalePredictions(
                        lastUpdated = Clock.System.now().minus(2.minutes),
                        action = {}
                    )
            )
        val staleVM = ErrorBannerViewModel(true, staleRepo, MockSettingsRepository())
        composeTestRule.setContent {
            LaunchedEffect(null) { staleVM.activate() }
            ErrorBanner(staleVM)
        }

        composeTestRule.onNodeWithText("Updated 2 minutes ago").assertDoesNotExist()
    }
}
