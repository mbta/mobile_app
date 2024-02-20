package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.response.SearchResponse
import com.mbta.tid.mbta_app.model.response.StopAndRoutePatternResponse
import com.mbta.tid.mbta_app.phoenix.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.phoenixSocket
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.path
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.errors.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Backend(engine: HttpClientEngine) {
    constructor() : this(getPlatform().httpClientEngine)

    private val mobileBackendHost = "mobile-app-backend-staging.mbtace.com"
    private val httpClient =
        HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
            install(WebSockets) { contentConverter = KotlinxWebsocketSerializationConverter(json) }
            install(ContentEncoding) { gzip(0.9F) }
            defaultRequest { url("https://$mobileBackendHost") }
        }

    // mutex to avoid creating a second socket while the first one is still connecting
    private var socketMutex = Mutex()
    private var _socket: PhoenixSocket? = null

    private suspend fun socket(): PhoenixSocket =
        socketMutex.withLock {
            when (val socket = _socket) {
                null -> {
                    val newSocket =
                        httpClient.phoenixSocket { url("wss://$mobileBackendHost/socket") }
                    _socket = newSocket
                    newSocket
                }
                else -> socket
            }
        }

    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class
    )
    suspend fun runSocket() = socket().run()

    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class
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

    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class
    )
    suspend fun getGlobalData(): StopAndRoutePatternResponse =
        httpClient
            .get {
                url { path("api/global") }
                expectSuccess = true
            }
            .body()

    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class
    )
    suspend fun predictionsStopsChannel(stopIds: List<String>) =
        PredictionsStopsChannel(socket(), stopIds)
}
