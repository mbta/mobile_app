package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteResult
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest

class SearchRoutesViewModelTest {
    @Test
    fun testSearchResults() = runTest {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { longName = "searchedRoute" }

        val searchResults = SearchResults(routes = listOf(RouteResult(route)), stops = emptyList())

        val searchVM =
            SearchRoutesViewModel(
                MockAnalytics(),
                MockGlobalRepository(GlobalResponse(objects)),
                object : ISearchResultRepository {
                    override suspend fun getRouteFilterResults(
                        query: String,
                        lineIds: List<String>?,
                        routeTypes: List<RouteType>?,
                    ): ApiResult<SearchResults>? {
                        delay(10.milliseconds)
                        return ApiResult.Ok(searchResults)
                    }

                    override suspend fun getSearchResults(query: String): ApiResult<SearchResults> {
                        fail("Standard search should not be called here")
                    }
                },
                onEventBufferOverflow = BufferOverflow.SUSPEND,
            )

        testViewModelFlow(searchVM).test {
            assertEquals(SearchRoutesViewModel.State.Unfiltered, awaitItem())
            searchVM.setQuery("")
            searchVM.setQuery("query")
            assertEquals(SearchRoutesViewModel.State.Results(listOf(route.id)), awaitItem())
        }
    }

    @Test
    fun testPathPassesFilterParams() = runTest {
        val objects = ObjectCollectionBuilder()
        val route = objects.route { longName = "searchedRoute" }

        val searchResults = SearchResults(routes = listOf(RouteResult(route)), stops = emptyList())
        val expectedParams = SearchRoutesViewModel.getParamsForPath(RoutePickerPath.Silver)

        val searchVM =
            SearchRoutesViewModel(
                MockAnalytics(),
                MockGlobalRepository(GlobalResponse(objects)),
                object : ISearchResultRepository {
                    override suspend fun getRouteFilterResults(
                        query: String,
                        lineIds: List<String>?,
                        routeTypes: List<RouteType>?,
                    ): ApiResult<SearchResults>? {
                        delay(10.milliseconds)
                        assertEquals(expectedParams.lineIds, lineIds)
                        assertEquals(expectedParams.routeTypes, routeTypes)
                        return ApiResult.Ok(searchResults)
                    }

                    override suspend fun getSearchResults(query: String): ApiResult<SearchResults> {
                        fail("Standard search should not be called here")
                    }
                },
                onEventBufferOverflow = BufferOverflow.SUSPEND,
            )

        testViewModelFlow(searchVM).test {
            assertEquals(SearchRoutesViewModel.State.Unfiltered, awaitItem())
            searchVM.setPath(RoutePickerPath.Silver)
            searchVM.setQuery("query")
            assertEquals(SearchRoutesViewModel.State.Results(listOf(route.id)), awaitItem())
        }
    }

    @Test
    fun testError() = runTest {
        val objects = ObjectCollectionBuilder()

        val searchVM =
            SearchRoutesViewModel(
                MockAnalytics(),
                MockGlobalRepository(GlobalResponse(objects)),
                object : ISearchResultRepository {
                    override suspend fun getRouteFilterResults(
                        query: String,
                        lineIds: List<String>?,
                        routeTypes: List<RouteType>?,
                    ): ApiResult<SearchResults>? {
                        delay(10.milliseconds)
                        return ApiResult.Error(code = 500, "Oops")
                    }

                    override suspend fun getSearchResults(query: String): ApiResult<SearchResults> {
                        fail("Standard search should not be called here")
                    }
                },
                onEventBufferOverflow = BufferOverflow.SUSPEND,
            )

        testViewModelFlow(searchVM).test {
            assertEquals(SearchRoutesViewModel.State.Unfiltered, awaitItem())
            searchVM.setQuery("")
            searchVM.setQuery("query")
            assertEquals(SearchRoutesViewModel.State.Error, awaitItem())
        }
    }
}
