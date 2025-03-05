package com.mbta.tid.mbta_app.android.search

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.ExperimentalTestApi
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
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.StopResult
import com.mbta.tid.mbta_app.model.StopResultRoute
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import com.mbta.tid.mbta_app.repositories.MockVisitHistoryRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinContext
import org.koin.test.KoinTest

@ExperimentalTestApi
@ExperimentalMaterial3Api
class SearchBarOverlayTest : KoinTest {
    val mockVisitHistoryRepository = MockVisitHistoryRepository()
    val builder = ObjectCollectionBuilder()
    val visitedStop =
        builder.stop {
            id = "visitedStopId"
            name = "visitedStopName"
        }
    val koinApplication =
        testKoinApplication(builder) {
            visitHistory = mockVisitHistoryRepository
            searchResults =
                object : ISearchResultRepository {
                    override suspend fun getSearchResults(query: String): ApiResult<SearchResults> {
                        return ApiResult.Ok(
                            SearchResults(
                                routes = emptyList(),
                                stops =
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
                                                )
                                        )
                                    )
                            )
                        )
                    }
                }
        }

    @get:Rule var composeTestRule = createComposeRule()

    @Test
    fun testSearchBarOverlayBehavesCorrectly() = runTest {
        val navigated = mutableStateOf(false)
        var expanded = mutableStateOf(false)

        var currentNavEntry = mutableStateOf<SheetRoutes?>(null)

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val focusRequester = remember { FocusRequester() }
                SearchBarOverlay(
                    expanded.value,
                    { expanded.value = it },
                    onStopNavigation = { navigated.value = true },
                    currentNavEntry = currentNavEntry.value,
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

        composeTestRule.awaitIdle()

        currentNavEntry.value = null

        composeTestRule.waitUntilAtLeastOneExists(hasText("Stops"))
        val searchNode = composeTestRule.onNodeWithText("Stops")
        searchNode.assertExists()
        searchNode.requestFocus()
        composeTestRule.awaitIdle()
        composeTestRule.onNodeWithText("Recently Viewed").assertExists()
        composeTestRule.waitUntilAtLeastOneExists(hasText(visitedStop.name))
        composeTestRule.onNodeWithText(visitedStop.name).assertExists()

        searchNode.performTextInput("sto")
        composeTestRule.waitUntilAtLeastOneExists(hasText("stopName"))
        composeTestRule.onNodeWithText("stopName").performClick()
        composeTestRule.waitUntil { navigated.value }
    }
}
