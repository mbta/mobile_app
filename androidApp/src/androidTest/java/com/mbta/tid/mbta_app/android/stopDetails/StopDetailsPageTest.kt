package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.MainApplication
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.pages.StopDetailsPage
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.KoinTest

class StopDetailsPageTest : KoinTest {

    @get:Rule val composeTestRule = createComposeRule()

    val koinApplication = koinApplication {
        modules(
            module { single<Analytics> { MockAnalytics() } },
            repositoriesModule(MockRepositories.buildWithDefaults()),
            MainApplication.koinViewModelModule
        )
    }

    @Test
    fun testCallsUpdateDepartures() = runTest {
        val viewModel = StopDetailsViewModel.mocked()
        val filters = mutableStateOf(StopDetailsPageFilters("stop", null, null))

        var departuresUpdated = false
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                var filters by remember { filters }
                val errorBannerVM =
                    ErrorBannerViewModel(
                        false,
                        MockErrorBannerStateRepository(),
                        MockSettingsRepository()
                    )

                StopDetailsPage(
                    viewModel = viewModel,
                    filters = filters,
                    onClose = {},
                    updateStopFilter = {},
                    updateTripFilter = {},
                    updateDepartures = { departuresUpdated = true },
                    openAlertDetails = {},
                    errorBannerViewModel = errorBannerVM
                )
            }

            LaunchedEffect(null) { viewModel.setDepartures(StopDetailsDepartures(emptyList())) }
        }

        composeTestRule.waitUntil { departuresUpdated == true }

        assertTrue(departuresUpdated)
    }
}
