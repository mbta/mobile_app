package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.android.pages.StopDetailsPage
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.viewModel.ErrorBannerViewModel
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.test.KoinTest

class StopDetailsPageTest : KoinTest {

    @get:Rule val composeTestRule = createComposeRule()

    val koinApplication = testKoinApplication()

    @Test
    fun testCallsUpdateRouteCardData() {
        val viewModel = StopDetailsViewModel.mocked()
        val filters = mutableStateOf(StopDetailsPageFilters("stop", null, null))

        var routeCardDataUpdated = false
        val errorBannerVM = ErrorBannerViewModel(MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                var filters by remember { filters }

                StopDetailsPage(
                    viewModel = viewModel,
                    allAlerts = null,
                    filters = filters,
                    onClose = {},
                    updateStopFilter = {},
                    updateTripFilter = {},
                    updateRouteCardData = { routeCardDataUpdated = true },
                    tileScrollState = rememberScrollState(),
                    openModal = {},
                    openSheetRoute = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }

            LaunchedEffect(null) { viewModel.setRouteCardData(emptyList()) }
        }

        composeTestRule.waitUntil { routeCardDataUpdated }

        assertTrue(routeCardDataUpdated)
    }
}
