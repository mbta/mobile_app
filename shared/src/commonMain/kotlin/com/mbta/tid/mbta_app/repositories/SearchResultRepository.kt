package com.mbta.tid.mbta_app.repositories

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
    suspend fun getSearchResults(query: String): ApiResult<SearchResults>?
}

class SearchResultRepository : KoinComponent, ISearchResultRepository {
    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getSearchResults(query: String): ApiResult<SearchResults>? {
        if (query == "") return null
        return ApiResult.runCatching {
            mobileBackendClient
                .get {
                    url {
                        path("api/search/query")
                        parameters.append("query", query)
                    }
                }
                .body<SearchResponse>()
                .data
        }
    }
}

class MockSearchResultRepository(
    private val routeResults: List<RouteResult> = emptyList(),
    private val stopResults: List<StopResult> = emptyList(),
) : ISearchResultRepository {
    override suspend fun getSearchResults(query: String): ApiResult<SearchResults> {
        return ApiResult.Ok(SearchResults(routeResults, stopResults))
    }
}

class IdleSearchResultRepository : ISearchResultRepository {
    override suspend fun getSearchResults(query: String): ApiResult<SearchResults>? {
        return suspendCancellableCoroutine {}
    }
}
