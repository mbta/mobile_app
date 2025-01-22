package com.mbta.tid.mbta_app.android.state

import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.history.Visit
import com.mbta.tid.mbta_app.history.VisitHistory
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.StopResult
import com.mbta.tid.mbta_app.model.StopResultRoute
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import com.mbta.tid.mbta_app.repositories.MockVisitHistoryRepository
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class GetSearchResultTest {
    val builder = ObjectCollectionBuilder()
    val visitedStop =
        builder.stop {
            id = "visitedStopId"
            name = "visitedStopName"
        }

    val searchResults =
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

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testSearchResults() = runTest {
        var actualSearchResultsViewModel: SearchResultsViewModel? = null
        val mockVisitHistoryRepository = MockVisitHistoryRepository()
        val visitHistory = VisitHistory()

        runBlocking {
            visitHistory.add(Visit.StopVisit(visitedStop.id))
            mockVisitHistoryRepository.setVisitHistory(visitHistory)
        }

        composeTestRule.setContent {
            actualSearchResultsViewModel =
                getSearchResultsVm(
                    GlobalResponse(builder),
                    MockAnalytics(),
                    object : ISearchResultRepository {
                        override suspend fun getSearchResults(
                            query: String
                        ): ApiResult<SearchResults> {
                            return ApiResult.Ok(searchResults)
                        }
                    },
                    VisitHistoryUsecase(mockVisitHistoryRepository)
                )
        }

        composeTestRule.waitUntil { actualSearchResultsViewModel != null }
        composeTestRule.awaitIdle()

        actualSearchResultsViewModel?.getSearchResults("")
        composeTestRule.waitUntil { actualSearchResultsViewModel?.searchResults?.value != null }
        assert(
            actualSearchResultsViewModel?.searchResults?.value?.stops?.first()?.id == visitedStop.id
        )

        actualSearchResultsViewModel?.getSearchResults("query")
        composeTestRule.waitUntil {
            actualSearchResultsViewModel?.searchResults?.value == searchResults
        }
        assert(actualSearchResultsViewModel?.searchResults?.value == searchResults)
    }
}
