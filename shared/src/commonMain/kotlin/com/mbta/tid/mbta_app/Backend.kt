package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.response.RouteResponse
import com.mbta.tid.mbta_app.model.response.SearchResponse
import com.mbta.tid.mbta_app.model.response.StopAndRoutePatternResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.http.path
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.errors.IOException
import kotlin.coroutines.cancellation.CancellationException

class Backend(engine: HttpClientEngine) {
    constructor() : this(getPlatform().httpClientEngine)

    companion object {
        const val mobileBackendHost = "mobile-app-backend-staging.mbtace.com"
    }

    private val httpClient =
        HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
            install(WebSockets) { contentConverter = KotlinxWebsocketSerializationConverter(json) }
            install(ContentEncoding) { gzip(0.9F) }
            install(HttpTimeout) { requestTimeoutMillis = 5000 }
            defaultRequest { url("https://$mobileBackendHost") }
        }

    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class,
        HttpRequestTimeoutException::class
    )
    suspend fun getGlobalData(): StopAndRoutePatternResponse =
        httpClient
            .get {
                timeout { requestTimeoutMillis = 10000 }
                url { path("api/global") }
                expectSuccess = true
            }
            .body()

    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class,
        HttpRequestTimeoutException::class
    )
    suspend fun getNearby(latitude: Double, longitude: Double): StopAndRoutePatternResponse =
        httpClient
            .get {
                url {
                    path("api/nearby/")
                    parameters.append("latitude", latitude.toString())
                    parameters.append("longitude", longitude.toString())
                }
                expectSuccess = true
            }
            .body()

    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class,
        HttpRequestTimeoutException::class
    )
    suspend fun getRailRouteShapes(): RouteResponse =
        httpClient
            .get {
                url { path("api/shapes/rail") }
                expectSuccess = true
            }
            .body()

    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class,
        HttpRequestTimeoutException::class
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
