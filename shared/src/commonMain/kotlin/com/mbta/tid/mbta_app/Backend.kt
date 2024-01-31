package com.mbta.tid.mbta_app

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.http.path
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.errors.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.Json

class Backend(engine: HttpClientEngine) {
    private val mobileBackendBaseUrl = "https://mobile-app-backend-staging.mbtace.com"
    private val httpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            defaultRequest { url(mobileBackendBaseUrl) }
        }

    companion object {
        val platformDefault
            get() = Backend(getPlatform().httpClientEngine)
    }

    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class
    )
    suspend fun getNearby(latitude: Double, longitude: Double): NearbyResponse =
        httpClient
            .get {
                url {
                    path("api/nearby/")
                    parameters.append("latitude", latitude.toString())
                    parameters.append("longitude", longitude.toString())
                    parameters.append("source", "v3")
                }
                expectSuccess = true
            }
            .body()

    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class
    )
    suspend fun getSearchResults(query: String): SearchResponse =
        httpClient
            .get {
                url {
                    path("api/search/query")
                    parameters.append("query", query)
                }
            }
            .body()
}
