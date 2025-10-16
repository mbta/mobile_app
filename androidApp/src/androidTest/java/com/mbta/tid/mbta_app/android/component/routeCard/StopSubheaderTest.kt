package com.mbta.tid.mbta_app.android.component.routeCard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import org.junit.Rule
import org.junit.Test

class StopSubheaderTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testBasic() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val stop = objects.stop {}

        composeTestRule.setContent {
            StopSubheader(
                RouteCardData.RouteStopData(
                    LineOrRoute.Route(route),
                    stop,
                    emptyList(),
                    emptyList(),
                )
            )
        }
        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()
    }

    @Test
    fun testAccessible() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val stop = objects.stop { wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE }

        loadKoinMocks {
            settings = MockSettingsRepository(mapOf(Settings.StationAccessibility to true))
        }
        composeTestRule.setContent {
            StopSubheader(
                RouteCardData.RouteStopData(
                    LineOrRoute.Route(route),
                    stop,
                    emptyList(),
                    emptyList(),
                )
            )
        }
        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()
        composeTestRule.onNodeWithTag("wheelchair_not_accessible").assertDoesNotExist()
    }

    @Test
    fun testNotAccessible() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val stop = objects.stop { wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE }

        loadKoinMocks {
            settings = MockSettingsRepository(mapOf(Settings.StationAccessibility to true))
        }
        composeTestRule.setContent {
            StopSubheader(
                RouteCardData.RouteStopData(
                    LineOrRoute.Route(route),
                    stop,
                    emptyList(),
                    emptyList(),
                )
            )
        }
        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()
        composeTestRule.onNodeWithText("Not accessible").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wheelchair_not_accessible").assertIsDisplayed()
    }

    @Test
    fun testElevatorAlert() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val stop = objects.stop { wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE }
        val alert = objects.alert { effect = Alert.Effect.ElevatorClosure }

        val lineOrRoute = LineOrRoute.Route(route)
        loadKoinMocks {
            settings = MockSettingsRepository(mapOf(Settings.StationAccessibility to true))
        }
        composeTestRule.setContent {
            StopSubheader(
                RouteCardData.RouteStopData(
                    lineOrRoute,
                    stop,
                    emptyList(),
                    listOf(
                        RouteCardData.Leaf(
                            lineOrRoute,
                            stop,
                            0,
                            emptyList(),
                            emptySet(),
                            emptyList(),
                            listOf(alert),
                            true,
                            true,
                            emptyList(),
                            RouteCardData.Context.StopDetailsUnfiltered,
                        )
                    ),
                )
            )
        }
        composeTestRule.onNodeWithText(stop.name).assertIsDisplayed()
        composeTestRule.onNodeWithText("1 elevator closed").assertIsDisplayed()
        composeTestRule.onNodeWithTag("elevator_alert").assertIsDisplayed()
    }
}
