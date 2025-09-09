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
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.testUtils.waitUntilAtLeastOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
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
import com.mbta.tid.mbta_app.routes.SheetRoutes
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest

@ExperimentalTestApi
@ExperimentalMaterial3Api
class SearchBarOverlayTest : KoinTest {
    private val builder = ObjectCollectionBuilder()
    private val visitedStop = builder.stop { name = "visitedStopName" }
    private val searchedStop = builder.stop { name = "stopName" }
    private val route =
        builder.route {
            longName = "Here - There"
            shortName = "3½"
            type = RouteType.BUS
        }

    @get:Rule var composeTestRule = createComposeRule()

    @Test
    fun testSearchBarOverlayBehavesCorrectly() {
        val mockVisitHistoryRepository = MockVisitHistoryRepository()

        loadKoinMocks(builder) {
            visitHistory = mockVisitHistoryRepository
            searchResults =
                MockSearchResultRepository(
                    stopResults =
                        listOf(
                            StopResult(
                                id = searchedStop.id,
                                rank = 2,
                                name = searchedStop.name,
                                zone = "stopZone",
                                isStation = false,
                                routes =
                                    listOf(
                                        StopResultRoute(type = RouteType.BUS, icon = "routeIcon")
                                    ),
                            )
                        )
                )
        }

        val navigated = mutableStateOf(false)
        val expanded = mutableStateOf(false)

        val currentNavEntry = mutableStateOf<SheetRoutes?>(null)

        composeTestRule.setContent {
            val focusRequester = remember { FocusRequester() }
            SearchBarOverlay(
                expanded.value,
                currentNavEntry.value?.showSearchBar ?: true,
                { expanded.value = it },
                onStopNavigation = { navigated.value = true },
                onRouteNavigation = {},
                inputFieldFocusRequester = focusRequester,
            ) {
                Text("Content")
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

        composeTestRule.waitUntilAtLeastOneExistsDefaultTimeout(hasText("Stops"))
        val searchNode = composeTestRule.onNodeWithText("Stops")
        searchNode.assertExists()
        searchNode.requestFocus()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Recently Viewed").assertExists()
        composeTestRule.waitUntilAtLeastOneExistsDefaultTimeout(hasText(visitedStop.name))
        composeTestRule.onNodeWithText(visitedStop.name).assertExists()

        searchNode.performTextInput("sto")
        composeTestRule.waitUntilAtLeastOneExistsDefaultTimeout(hasText(searchedStop.name))
        composeTestRule.onNodeWithText(searchedStop.name).performClick()
        composeTestRule.waitUntilDefaultTimeout { navigated.value }
    }

    @Test
    fun testHidesRoutesByDefault() {
        loadKoinMocks(builder) {
            searchResults = MockSearchResultRepository(routeResults = listOf(RouteResult(route)))
        }

        composeTestRule.setContent {
            val focusRequester = remember { FocusRequester() }
            SearchBarOverlay(
                expanded = true,
                showSearchBar = true,
                onExpandedChange = {},
                onStopNavigation = {},
                onRouteNavigation = {},
                inputFieldFocusRequester = focusRequester,
                content = {},
            )
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
        loadKoinMocks(builder) {
            searchResults = MockSearchResultRepository(routeResults = listOf(RouteResult(route)))
            settings = MockSettingsRepository(mapOf(Settings.SearchRouteResults to true))
        }

        var navigated = false

        composeTestRule.setContent {
            val focusRequester = remember { FocusRequester() }
            SearchBarOverlay(
                expanded = true,
                showSearchBar = true,
                onExpandedChange = {},
                onStopNavigation = {},
                onRouteNavigation = { navigated = true },
                inputFieldFocusRequester = focusRequester,
                content = {},
            )
        }

        val searchNode = composeTestRule.onNode(hasSetTextAction())
        searchNode.assertExists()
        searchNode.requestFocus()
        composeTestRule.waitForIdle()

        searchNode.performTextInput("anything")
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Routes"))
        composeTestRule.onNodeWithText("3½").assertIsDisplayed()
        composeTestRule.onNodeWithText("Here - There").assertIsDisplayed().performClick()
        composeTestRule.waitUntilDefaultTimeout { navigated }
    }

    @Test
    fun testOnExpandedChangeNotCalledOnFirstLoad() {
        loadKoinMocks(builder) {
            searchResults = MockSearchResultRepository(routeResults = listOf(RouteResult(route)))
            settings = MockSettingsRepository(mapOf(Settings.SearchRouteResults to true))
        }

        var onExpandedCalledWith: Boolean? = null

        composeTestRule.setContent {
            val focusRequester = remember { FocusRequester() }
            SearchBarOverlay(
                expanded = false,
                showSearchBar = true,
                onExpandedChange = { expandedVal -> onExpandedCalledWith = expandedVal },
                onStopNavigation = {},
                onRouteNavigation = {},
                inputFieldFocusRequester = focusRequester,
                content = {},
            )
        }

        val searchNode = composeTestRule.onNode(hasSetTextAction())
        searchNode.assertExists()
        composeTestRule.waitForIdle()
        assertEquals(null, onExpandedCalledWith)
        searchNode.requestFocus()

        assertEquals(true, onExpandedCalledWith)
    }
}
