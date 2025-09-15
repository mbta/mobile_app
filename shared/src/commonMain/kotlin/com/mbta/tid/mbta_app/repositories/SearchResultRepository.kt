package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.RouteResult
import com.mbta.tid.mbta_app.model.RouteType
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

public interface ISearchResultRepository {
    public suspend fun getRouteFilterResults(
        query: String,
        lineIds: List<String>? = null,
        routeTypes: List<RouteType>? = null,
    ): ApiResult<SearchResults>?

    public suspend fun getSearchResults(query: String): ApiResult<SearchResults>?
}

internal class SearchResultRepository : KoinComponent, ISearchResultRepository {
    private val mobileBackendClient: MobileBackendClient by inject()

    private suspend fun searchRequest(
        endpoint: String,
        params: Map<String, String>,
    ): ApiResult<SearchResults>? {
        return ApiResult.runCatching {
            mobileBackendClient
                .get {
                    url {
                        path(endpoint)
                        params.forEach { (name, value) -> parameters.append(name, value) }
                    }
                }
                .body<SearchResponse>()
                .data
        }
    }

    private suspend fun searchRequest(endpoint: String, query: String): ApiResult<SearchResults>? {
        if (query == "") return null
        return searchRequest(endpoint, mapOf("query" to query))
    }

    override suspend fun getRouteFilterResults(
        query: String,
        lineIds: List<String>?,
        routeTypes: List<RouteType>?,
    ): ApiResult<SearchResults>? {
        if (query == "") return null
        val params = mutableMapOf("query" to query)
        lineIds?.let { params.put("line_id", it.joinToString(",")) }
        routeTypes?.let { params.put("type", it.joinToString(",") { type -> type.serialName }) }
        return searchRequest("api/search/routes", params)
    }

    override suspend fun getSearchResults(query: String): ApiResult<SearchResults>? =
        searchRequest("api/search/query", query)
}

public class MockSearchResultRepository
@DefaultArgumentInterop.Enabled
constructor(
    private val routeResults: List<RouteResult> = emptyList(),
    private val stopResults: List<StopResult> = emptyList(),
    private val onGetRouteFilterResults: () -> Unit = {},
    private val onGetSearchResults: () -> Unit = {},
) : ISearchResultRepository {
    override suspend fun getRouteFilterResults(
        query: String,
        lineIds: List<String>?,
        routeTypes: List<RouteType>?,
    ): ApiResult<SearchResults> {
        onGetRouteFilterResults()
        return ApiResult.Ok(SearchResults(routeResults, emptyList()))
    }

    override suspend fun getSearchResults(query: String): ApiResult<SearchResults> {
        onGetSearchResults()
        return ApiResult.Ok(SearchResults(routeResults, stopResults))
    }
}

internal class IdleSearchResultRepository : ISearchResultRepository {
    override suspend fun getRouteFilterResults(
        query: String,
        lineIds: List<String>?,
        routeTypes: List<RouteType>?,
    ): ApiResult<SearchResults>? {
        return suspendCancellableCoroutine {}
    }

    override suspend fun getSearchResults(query: String): ApiResult<SearchResults>? {
        return suspendCancellableCoroutine {}
    }
}
