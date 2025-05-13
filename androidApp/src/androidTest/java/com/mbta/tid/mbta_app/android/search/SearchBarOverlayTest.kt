package com.mbta.tid.mbta_app.android.search

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.history.Visit
import com.mbta.tid.mbta_app.history.VisitHistory
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteResult
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.StopResult
import com.mbta.tid.mbta_app.model.StopResultRoute
import com.mbta.tid.mbta_app.repositories.MockSearchResultRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.MockVisitHistoryRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinContext
import org.koin.test.KoinTest

@ExperimentalTestApi
@ExperimentalMaterial3Api
class SearchBarOverlayTest : KoinTest {
    val builder = ObjectCollectionBuilder()
    val visitedStop =
        builder.stop {
            id = "visitedStopId"
            name = "visitedStopName"
        }
    val route =
        builder.route {
            longName = "Here - There"
            shortName = "3½"
            type = RouteType.BUS
        }

    @get:Rule var composeTestRule = createComposeRule()

    @Test
    fun testSearchBarOverlayBehavesCorrectly() {
        val mockVisitHistoryRepository = MockVisitHistoryRepository()

        val koinApplication =
            testKoinApplication(builder) {
                visitHistory = mockVisitHistoryRepository
                searchResults =
                    MockSearchResultRepository(
                        stopResults =
                            listOf(
                                StopResult(
                                    id = "stopId",
                                    rank = 2,
                                    name = "stopName",
                                    zone = "stopZone",
                                    isStation = false,
                                    routes =
                                        listOf(
                                            StopResultRoute(
                                                type = RouteType.BUS,
                                                icon = "routeIcon",
                                            )
                                        ),
                                )
                            )
                    )
            }

        val navigated = mutableStateOf(false)
        val expanded = mutableStateOf(false)

        val currentNavEntry = mutableStateOf<SheetRoutes?>(null)

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val focusRequester = remember { FocusRequester() }
                SearchBarOverlay(
                    expanded.value,
                    currentNavEntry.value?.showSearchBar ?: true,
                    { expanded.value = it },
                    onStopNavigation = { navigated.value = true },
                    onRouteNavigation = {},
                    inputFieldFocusRequester = focusRequester,
                    searchResultsVm = koinViewModel(),
                ) {
                    Text("Content")
                }
            }
        }

        composeTestRule.onNodeWithText("Content").assertExists()

        // Simulate navigating to a stop and back
        runBlocking {
            mockVisitHistoryRepository.setVisitHistory(
                VisitHistory().apply { add(Visit.StopVisit(visitedStop.id)) }
            )
        }

        currentNavEntry.value = SheetRoutes.StopDetails(visitedStop.id, null, null)

        composeTestRule.waitForIdle()

        currentNavEntry.value = null

        composeTestRule.waitUntilAtLeastOneExists(hasText("Stops"))
        val searchNode = composeTestRule.onNodeWithText("Stops")
        searchNode.assertExists()
        searchNode.requestFocus()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Recently Viewed").assertExists()
        composeTestRule.waitUntilAtLeastOneExists(hasText(visitedStop.name))
        composeTestRule.onNodeWithText(visitedStop.name).assertExists()

        searchNode.performTextInput("sto")
        composeTestRule.waitUntilAtLeastOneExists(hasText("stopName"))
        composeTestRule.onNodeWithText("stopName").performClick()
        composeTestRule.waitUntil { navigated.value }
    }

    @Test
    fun testHidesRoutesByDefault() {
        val koinApplication =
            testKoinApplication(builder) {
                searchResults =
                    MockSearchResultRepository(routeResults = listOf(RouteResult(route)))
            }

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val focusRequester = remember { FocusRequester() }
                SearchBarOverlay(
                    expanded = true,
                    showSearchBar = true,
                    onExpandedChange = {},
                    onStopNavigation = {},
                    onRouteNavigation = {},
                    inputFieldFocusRequester = focusRequester,
                    searchResultsVm = koinViewModel(),
                    content = {},
                )
            }
        }

        val searchNode = composeTestRule.onNode(hasSetTextAction())
        searchNode.assertExists()
        searchNode.requestFocus()
        composeTestRule.waitForIdle()

        searchNode.performTextInput("anything")
        composeTestRule.onNodeWithText("3½").assertDoesNotExist()
        composeTestRule.onNodeWithText("Here - There").assertDoesNotExist()
    }

    @Test
    fun testShowsRoutesWhenEnabled() {
        val koinApplication =
            testKoinApplication(builder) {
                searchResults =
                    MockSearchResultRepository(routeResults = listOf(RouteResult(route)))
                settings = MockSettingsRepository(mapOf(Settings.SearchRouteResults to true))
            }

        var navigated = false

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val focusRequester = remember { FocusRequester() }
                SearchBarOverlay(
                    expanded = true,
                    showSearchBar = true,
                    onExpandedChange = {},
                    onStopNavigation = {},
                    onRouteNavigation = { navigated = true },
                    inputFieldFocusRequester = focusRequester,
                    searchResultsVm = koinViewModel(),
                    content = {},
                )
            }
        }

        val searchNode = composeTestRule.onNode(hasSetTextAction())
        searchNode.assertExists()
        searchNode.requestFocus()
        composeTestRule.waitForIdle()

        searchNode.performTextInput("anything")
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExists(hasText("Routes"))
        composeTestRule.onNodeWithText("3½").assertIsDisplayed()
        composeTestRule.onNodeWithText("Here - There").assertIsDisplayed().performClick()
        composeTestRule.waitUntil { navigated }
    }
}
