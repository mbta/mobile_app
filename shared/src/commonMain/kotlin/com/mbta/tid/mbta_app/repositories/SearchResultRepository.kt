package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.response.SearchResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.http.path
import io.ktor.serialization.JsonConvertException
import io.ktor.utils.io.errors.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ISearchResultRepository {
    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class,
        HttpRequestTimeoutException::class
    )
    suspend fun getSearchResults(query: String): SearchResults?
}

class SearchResultRepository : KoinComponent, ISearchResultRepository {
    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getSearchResults(query: String): SearchResults? {
        if (query == "") return null
        return mobileBackendClient
            .get {
                url {
                    path("api/search/query")
                    parameters.append("query", query)
                }
                expectSuccess = true
            }
            .body<SearchResponse>()
            .data
    }
}

class MockSearchResultRepository : ISearchResultRepository {
    override suspend fun getSearchResults(query: String): SearchResults {
        return SearchResults(emptyList(), emptyList())
    }
}

class IdleSearchResultRepository : ISearchResultRepository {
    override suspend fun getSearchResults(query: String): SearchResults {
        return suspendCancellableCoroutine {}
    }
}
