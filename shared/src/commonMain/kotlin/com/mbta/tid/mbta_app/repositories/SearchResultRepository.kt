package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.RouteResult
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.StopResult
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.SearchResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.http.path
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ISearchResultRepository {
    suspend fun getRouteFilterResults(query: String): ApiResult<SearchResults>?

    suspend fun getSearchResults(query: String): ApiResult<SearchResults>?
}

class SearchResultRepository : KoinComponent, ISearchResultRepository {
    private val mobileBackendClient: MobileBackendClient by inject()

    private suspend fun searchRequest(endpoint: String, query: String): ApiResult<SearchResults>? {
        if (query == "") return null
        return ApiResult.runCatching {
            mobileBackendClient
                .get {
                    url {
                        path(endpoint)
                        parameters.append("query", query)
                    }
                }
                .body<SearchResponse>()
                .data
        }
    }

    override suspend fun getRouteFilterResults(query: String): ApiResult<SearchResults>? =
        searchRequest("api/search/routes", query)

    override suspend fun getSearchResults(query: String): ApiResult<SearchResults>? =
        searchRequest("api/search/query", query)
}

class MockSearchResultRepository
@DefaultArgumentInterop.Enabled
constructor(
    private val routeResults: List<RouteResult> = emptyList(),
    private val stopResults: List<StopResult> = emptyList(),
    private val onGetRouteFilterResults: () -> Unit = {},
    private val onGetSearchResults: () -> Unit = {},
) : ISearchResultRepository {
    override suspend fun getRouteFilterResults(query: String): ApiResult<SearchResults> {
        onGetRouteFilterResults()
        return ApiResult.Ok(SearchResults(routeResults, emptyList()))
    }

    override suspend fun getSearchResults(query: String): ApiResult<SearchResults> {
        onGetSearchResults()
        return ApiResult.Ok(SearchResults(routeResults, stopResults))
    }
}

class IdleSearchResultRepository : ISearchResultRepository {
    override suspend fun getRouteFilterResults(query: String): ApiResult<SearchResults>? {
        return suspendCancellableCoroutine {}
    }

    override suspend fun getSearchResults(query: String): ApiResult<SearchResults>? {
        return suspendCancellableCoroutine {}
    }
}
