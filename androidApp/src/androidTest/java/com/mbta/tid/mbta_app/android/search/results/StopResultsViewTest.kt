package com.mbta.tid.mbta_app.android.search.results

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopResult
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class StopResultsViewTest {
    @get:Rule var composeTestRule = createComposeRule()

    @Test
    fun testStationResultWithMultipleRoutes() {
        val objects = ObjectCollectionBuilder()
        val station =
            objects.stop {
                id = "place-sstat"
                name = "South Station"
                locationType = LocationType.STATION
            }

        val red =
            objects.route {
                id = "red"
                shortName = "RL"
                longName = "Red Line"
                type = RouteType.HEAVY_RAIL
                sortOrder = 1
            }

        val cr =
            objects.route {
                id = "cr-worcester"
                shortName = "Worcester"
                longName = "Worcester"
                type = RouteType.COMMUTER_RAIL
                sortOrder = 2
            }

        val sl =
            objects.route {
                id = "741"
                shortName = "SL1"
                longName = "Silver Line 1"
                type = RouteType.BUS
                color = "7C878E"
                sortOrder = 3
            }

        val bus4 =
            objects.route {
                id = "4"
                shortName = "4"
                type = RouteType.BUS
                sortOrder = 4
            }
        val bus5 =
            objects.route {
                id = "5"
                shortName = "5"
                type = RouteType.BUS
                sortOrder = 5
            }

        var handleSearchCalled = false

        composeTestRule.setContent {
            StopResultsView(
                RoundedCornerShape(10.dp),
                StopResult(
                    station.id,
                    1,
                    name = station.name,
                    zone = null,
                    isStation = true,
                    routes = listOf(),
                ),
                routes = listOf(bus5, bus4, sl, cr, red),
                globalResponse = null,
                handleSearch = { handleSearchCalled = true },
            )
        }

        composeTestRule.onNodeWithText("South Station").assertIsDisplayed().performClick()
        assertTrue(handleSearchCalled)
        composeTestRule
            .onNodeWithContentDescription(
                "serves Red Line train,Commuter Rail trains,Silver Line buses,buses"
            )
            .assertIsDisplayed()
    }

    @Test
    fun testStandaloneStopShowsAllRoutesResultWithMultipleRoutes() {
        val objects = ObjectCollectionBuilder()
        val stop =
            objects.stop {
                id = "platform-stop"
                name = "Platform Stop"
                locationType = LocationType.STOP
            }

        val bus4 =
            objects.route {
                id = "4"
                shortName = "4"
                type = RouteType.BUS
                sortOrder = 1
            }

        val bus5 =
            objects.route {
                id = "5"
                shortName = "5"
                type = RouteType.BUS
                sortOrder = 2
            }

        composeTestRule.setContent {
            StopResultsView(
                RoundedCornerShape(10.dp),
                StopResult(
                    stop.id,
                    1,
                    name = stop.name,
                    zone = null,
                    isStation = false,
                    routes = listOf(),
                ),
                routes = listOf(bus5, bus4),
                globalResponse = null,
                handleSearch = {},
            )
        }

        composeTestRule.onNodeWithText("Platform Stop").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("serves 4 bus,5 bus").assertIsDisplayed()
    }
}
