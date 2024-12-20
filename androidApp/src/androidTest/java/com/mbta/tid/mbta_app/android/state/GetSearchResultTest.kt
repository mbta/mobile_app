package com.mbta.tid.mbta_app.android.state

import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.StopResult
import com.mbta.tid.mbta_app.model.StopResultRoute
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class GetSearchResultTest {
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

        composeTestRule.setContent {
            actualSearchResultsViewModel =
                getSearchResultsVm(
                    object : ISearchResultRepository {
                        override suspend fun getSearchResults(
                            query: String
                        ): ApiResult<SearchResults>? {
                            return ApiResult.Ok(searchResults)
                        }
                    }
                )
        }

        composeTestRule.waitUntil { actualSearchResultsViewModel != null }

        actualSearchResultsViewModel?.getSearchResults("query")

        composeTestRule.waitUntil { actualSearchResultsViewModel?.searchResults?.value != null }

        assert(actualSearchResultsViewModel?.searchResults?.value == searchResults)
    }
}
