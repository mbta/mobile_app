package com.mbta.tid.mbta_app

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.model.response.SearchResponse
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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class Backend(engine: HttpClientEngine, val appVariant: AppVariant) {
    constructor(appVariant: AppVariant) : this(getPlatform().httpClientEngine, appVariant)

    private val httpClient =
        HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
            install(WebSockets) { contentConverter = KotlinxWebsocketSerializationConverter(json) }
            install(ContentEncoding) { gzip(0.9F) }
            install(HttpTimeout) { requestTimeoutMillis = 5000 }
            defaultRequest { url(appVariant.backendRoot) }
        }

    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class,
        HttpRequestTimeoutException::class
    )
    suspend fun getGlobalData(): GlobalResponse =
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
    suspend fun getNearby(latitude: Double, longitude: Double): NearbyResponse =
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
    suspend fun getMapFriendlyRailShapes(): MapFriendlyRouteResponse =
        httpClient
            .get {
                url { path("api/shapes/map-friendly/rail") }
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
    @DefaultArgumentInterop.Enabled
    suspend fun getSchedule(
        stopIds: List<String>,
        now: Instant = Clock.System.now()
    ): ScheduleResponse =
        httpClient
            .get {
                url {
                    path("api/schedules")
                    parameters.append("stop_ids", stopIds.joinToString(separator = ","))
                    parameters.append("date_time", now.toString())
                }
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
