package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteStopFilter
import com.mbta.tid.mbta_app.model.RouteStopPageFilters
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

class RouteStopPageTest : KoinTest {

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
        val filters = RouteStopPageFilters("stop", RouteStopFilter(Route.Id("route"), 0), null)

        val viewModel = MockStopCentricViewModel()
        viewModel.onSetActive = { active, fromBackground -> activeSet = active && !fromBackground }
        viewModel.onSetAlerts = { alertsSet = it == alerts }
        viewModel.onSetFilters = { filtersSet = it == filters }
        viewModel.onSetNow = { nowSet = true }

        composeTestRule.setContent {
            RouteStopPage(
                filters = filters,
                allAlerts = alerts,
                navCallbacks = NavigationCallbacks.empty,
                updateRouteStopFilter = {},
                updateTripFilter = {},
                tileScrollState = rememberScrollState(),
                openModal = {},
                openSheetRoute = {},
                errorBannerViewModel = koinInject(),
                stopCentricViewModel = viewModel,
            )
        }

        composeTestRule.waitUntilDefaultTimeout { activeSet && alertsSet && filtersSet && nowSet }

        assertTrue(activeSet && alertsSet && filtersSet && nowSet)
    }
}
