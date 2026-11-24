package com.mbta.tid.mbta_app.android.pages

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.StopOverviewPageFilters
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.utils.NavigationCallbacks
import com.mbta.tid.mbta_app.viewModel.MockStopCentricViewModel
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.compose.koinInject
import org.koin.test.KoinTest

class StopOverviewPageTest : KoinTest {

    @get:Rule val composeTestRule = createComposeRule(effectContext = UnconfinedTestDispatcher())

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
        val filters = StopOverviewPageFilters("stop")

        val viewModel = MockStopCentricViewModel()
        viewModel.onSetActive = { active, fromBackground -> activeSet = active && !fromBackground }
        viewModel.onSetAlerts = { alertsSet = it == alerts }
        viewModel.onSetFilters = { filtersSet = it == filters }
        viewModel.onSetNow = { nowSet = true }

        composeTestRule.setContent {
            StopOverviewPage(
                filters = filters,
                allAlerts = alerts,
                navCallbacks = NavigationCallbacks.empty,
                updateRouteStopFilter = {},
                openModal = {},
                errorBannerViewModel = koinInject(),
                stopCentricViewModel = viewModel,
            )
        }

        composeTestRule.waitUntilDefaultTimeout { activeSet && alertsSet && filtersSet && nowSet }

        assertTrue(activeSet && alertsSet && filtersSet && nowSet)
    }
}
