package com.mbta.tid.mbta_app.android.routeDetails

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.android.testUtils.waitUntilNodeCountDefaultTimeout
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteDetailsStopList
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockRouteStopsRepository
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext

class RouteStopListViewTest {
    @get:Rule val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDisplaysEverything() {
        val objects = ObjectCollectionBuilder()
        val stop1 = objects.stop { name = "Stop 1" }
        val stop2 = objects.stop { name = "Stop 2" }
        val stop3 = objects.stop { name = "Stop 3" }
        val mainRoute =
            objects.route {
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Here", "There")
                longName = "Mauve Line"
                type = RouteType.HEAVY_RAIL
            }
        val pattern0 =
            objects.routePattern(mainRoute) {
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { stopIds = listOf(stop1.id, stop2.id, stop3.id) }
            }
        objects.routePattern(mainRoute) {
            directionId = 1
            typicality = RoutePattern.Typicality.Typical
        }
        val connectingRoute =
            objects.route {
                shortName = "32"
                type = RouteType.BUS
            }
        objects.routePattern(connectingRoute) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(stop2.id) }
        }

        val clicks = mutableListOf<RouteDetailsStopList.Entry>()
        var closed = false

        val koin =
            testKoinApplication(objects) {
                routeStops =
                    MockRouteStopsRepository(
                        listOf(stop1.id, stop2.id, stop3.id),
                        routeId = mainRoute.id,
                    )
            }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RouteStopListView(
                    RouteCardData.LineOrRoute.Route(mainRoute),
                    RouteDetailsContext.Details,
                    GlobalResponse(objects),
                    onClick = clicks::add,
                    onBack = {},
                    onClose = { closed = true },
                    errorBannerViewModel = errorBannerVM,
                    rightSideContent = { context, _ ->
                        when (context) {
                            is RouteDetailsRowContext.Details ->
                                Text("rightSideContent for ${context.stop.name}")
                            else -> fail("Wrong row context provided")
                        }
                    },
                )
            }
        }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText(stop1.name))

        composeTestRule.onNodeWithText(mainRoute.longName).assertIsDisplayed()

        composeTestRule.onNodeWithText("Westbound to").assertIsDisplayed()
        composeTestRule.onNodeWithText("Here").assertIsDisplayed()
        composeTestRule.onNodeWithText("Eastbound to").assertIsDisplayed()
        composeTestRule.onNodeWithText("There").assertIsDisplayed()

        composeTestRule.onNodeWithText(stop1.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop2.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop3.name).assertIsDisplayed()

        composeTestRule.onNodeWithText("rightSideContent for ${stop1.name}").assertIsDisplayed()
        composeTestRule.onNodeWithText("rightSideContent for ${stop2.name}").assertIsDisplayed()
        composeTestRule.onNodeWithText("rightSideContent for ${stop3.name}").assertIsDisplayed()

        composeTestRule
            .onNodeWithText(connectingRoute.shortName, useUnmergedTree = true)
            .assertIsDisplayed()

        composeTestRule.onNodeWithText(stop2.name).performClick()
        assertEquals(
            listOf(RouteDetailsStopList.Entry(stop2, listOf(pattern0), listOf(connectingRoute))),
            clicks,
        )

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        assertTrue(closed)
    }

    @Test
    fun selectsWithinLine() {
        val objects = ObjectCollectionBuilder()
        val line = objects.line()
        val route1 =
            objects.route {
                lineId = line.id
                type = RouteType.BUS
                shortName = "1"
                directionDestinations = listOf("One", "")
            }
        val route2 =
            objects.route {
                lineId = line.id
                type = RouteType.BUS
                shortName = "2"
                directionDestinations = listOf("Two", "")
            }
        val route3 =
            objects.route {
                lineId = line.id
                type = RouteType.BUS
                shortName = "3"
                directionDestinations = listOf("Three", "")
            }
        objects.routePattern(route1) { directionId = 0 }

        var lastSelectedRoute: String? = null

        val koin =
            testKoinApplication(objects) {
                routeStops =
                    MockRouteStopsRepository(
                        listOf(),
                        onGet = { routeId, _ -> lastSelectedRoute = routeId },
                    )
            }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RouteStopListView(
                    RouteCardData.LineOrRoute.Line(line, setOf(route1, route2, route3)),
                    RouteDetailsContext.Details,
                    GlobalResponse(objects),
                    onClick = {},
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                    defaultSelectedRouteId = route2.id,
                    rightSideContent = { _, _ -> },
                )
            }
        }

        composeTestRule.waitUntil { lastSelectedRoute != null }
        assertEquals(route2.id, lastSelectedRoute)

        composeTestRule.onNodeWithText(route3.shortName).performClick()
        composeTestRule.waitForIdle()
        assertEquals(route3.id, lastSelectedRoute)

        composeTestRule.onNodeWithText(checkNotNull(route1.directionDestinations[0])).performClick()
        composeTestRule.waitForIdle()
        assertEquals(route1.id, lastSelectedRoute)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testCollapsesNonTypicalStops() {
        val objects = ObjectCollectionBuilder()
        val stop1 = objects.stop { name = "Stop 1" }
        val stop2 = objects.stop { name = "Stop 2" }

        val stop3NonTypical = objects.stop { name = "Stop 3" }
        val stop4NonTypical = objects.stop { name = "Stop 4" }
        val mainRoute =
            objects.route {
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Here", "There")
                longName = "Mauve Line"
                type = RouteType.HEAVY_RAIL
            }
        val typicalPattern =
            objects.routePattern(mainRoute) {
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { stopIds = listOf(stop1.id, stop2.id) }
            }

        val deviationPattern =
            objects.routePattern(mainRoute) {
                directionId = 0
                typicality = RoutePattern.Typicality.Deviation
                representativeTrip {
                    stopIds = listOf(stop1.id, stop2.id, stop3NonTypical.id, stop4NonTypical.id)
                }
            }

        val koin =
            testKoinApplication(objects) {
                routeStops =
                    MockRouteStopsRepository(
                        listOf(stop1.id, stop2.id, stop3NonTypical.id, stop4NonTypical.id),
                        routeId = mainRoute.id,
                    )
            }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RouteStopListView(
                    RouteCardData.LineOrRoute.Route(mainRoute),
                    RouteDetailsContext.Details,
                    GlobalResponse(objects),
                    onClick = {},
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                    rightSideContent = { _, _ -> },
                )
            }
        }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText(stop1.name))

        composeTestRule.onNodeWithText(stop1.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop2.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(stop3NonTypical.name).assertIsNotDisplayed()
        composeTestRule.onNodeWithText("2 less common stops").assertIsDisplayed().performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText(stop3NonTypical.name))
        composeTestRule.onNodeWithText(stop4NonTypical.name).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFavoritesWithConfirmationDialog() {
        val objects = TestData.clone()
        val route = RouteCardData.LineOrRoute.Route(objects.getRoute("Red"))

        val koin =
            testKoinApplication(objects) {
                routeStops =
                    MockRouteStopsRepository(
                        listOf("place-alfcl", "place-davis", "place-portr"),
                        routeId = route.id,
                    )
            }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RouteStopListView(
                    route,
                    RouteDetailsContext.Favorites,
                    GlobalResponse(objects),
                    onClick = {},
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                    rightSideContent = { rowContext, _ ->
                        Text(
                            "Tap me",
                            modifier =
                                Modifier.clickable {
                                    when (rowContext) {
                                        is RouteDetailsRowContext.Details -> {}
                                        is RouteDetailsRowContext.Favorites ->
                                            rowContext.onTapStar()
                                    }
                                },
                        )
                    },
                )
            }
        }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Davis"))
        // Direction toggle on route page
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Southbound to"))
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Ashmont/Braintree"))

        composeTestRule.onAllNodesWithText("Tap me")[1].performClick()

        // 2 sets of direction labels - one on toggle, and one in favorites confirmation modal
        composeTestRule.waitUntilNodeCountDefaultTimeout(hasText("Southbound to"), 2)
        composeTestRule.waitUntilNodeCountDefaultTimeout(hasText("Ashmont/Braintree"), 2)

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Add"))
    }
}
