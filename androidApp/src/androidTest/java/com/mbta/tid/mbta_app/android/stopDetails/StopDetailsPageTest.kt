package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.pages.StopDetailsPage
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.viewModel.MockStopDetailsViewModel
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.compose.koinInject
import org.koin.test.KoinTest

class StopDetailsPageTest : KoinTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        loadKoinMocks()
    }

    @Test
    fun testUpdatesVM() {
        var activeSet = false
        var alertsSet = false
        var filtersSet = false
        var nowSet = false

        val alerts = AlertsStreamDataResponse(emptyMap())
        val filters = StopDetailsPageFilters("stop", null, null)

        val viewModel = MockStopDetailsViewModel()
        viewModel.onSetActive = { active, fromBackground -> activeSet = active && !fromBackground }
        viewModel.onSetAlerts = { alertsSet = it == alerts }
        viewModel.onSetFilters = { filtersSet = it == filters }
        viewModel.onSetNow = { nowSet = true }

        composeTestRule.setContent {
            StopDetailsPage(
                allAlerts = alerts,
                filters = filters,
                onClose = {},
                updateStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                openModal = {},
                openSheetRoute = {},
                errorBannerViewModel = koinInject(),
                stopDetailsViewModel = viewModel,
            )
        }

        composeTestRule.waitUntil { activeSet && alertsSet && filtersSet && nowSet }

        assertTrue(activeSet && alertsSet && filtersSet && nowSet)
    }
}
