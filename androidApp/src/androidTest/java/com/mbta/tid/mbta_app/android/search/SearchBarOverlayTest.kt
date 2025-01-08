package com.mbta.tid.mbta_app.android.search

import android.os.Bundle
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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import com.mbta.tid.mbta_app.history.Visit
import com.mbta.tid.mbta_app.history.VisitHistory
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.StopResult
import com.mbta.tid.mbta_app.model.StopResultRoute
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import com.mbta.tid.mbta_app.repositories.IVisitHistoryRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockVisitHistoryRepository
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
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
    val koinApplication = koinApplication {
        modules(
            module {
                single<IErrorBannerStateRepository> { MockErrorBannerStateRepository() }
                single<IGlobalRepository> { MockGlobalRepository() }
                single<IGlobalRepository> {
                    MockGlobalRepository(response = GlobalResponse(builder))
                }
                single<IVisitHistoryRepository> { mockVisitHistoryRepository }
                single<VisitHistoryUsecase> { VisitHistoryUsecase(get()) }
                single<ISearchResultRepository> {
                    object : ISearchResultRepository {
                        override suspend fun getSearchResults(
                            query: String
                        ): ApiResult<SearchResults>? {
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
            }
        )
    }

    @get:Rule var composeTestRule = createComposeRule()

    @Test
    fun testSearchBarOverlayBehavesCorrectly() = runTest {
        val navigated = mutableStateOf(false)
        var navBackStackEntry = mutableStateOf<NavBackStackEntry?>(null)

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val focusRequester = remember { FocusRequester() }
                SearchBarOverlay(
                    onStopNavigation = { navigated.value = true },
                    currentNavEntry = navBackStackEntry.value,
                    inputFieldFocusRequester = focusRequester,
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
        navBackStackEntry.value =
            NavBackStackEntry.create(
                context = null,
                arguments = Bundle().apply { putString("stopId", "visitedStopId") },
                destination = NavDestination("stop")
            )
        composeTestRule.awaitIdle()
        navBackStackEntry.value = null

        composeTestRule.waitUntilAtLeastOneExists(hasText("Search by stop"))
        val searchNode = composeTestRule.onNodeWithText("Search by stop")
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
