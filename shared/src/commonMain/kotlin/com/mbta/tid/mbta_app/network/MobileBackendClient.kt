package com.mbta.tid.mbta_app.network

import com.mbta.tid.mbta_app.getPlatform
import com.mbta.tid.mbta_app.json
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json

class MobileBackendClient(engine: HttpClientEngine) {
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

    suspend fun get(request: HttpRequestBuilder.() -> Unit): HttpResponse {
        return httpClient.get(request)
    }
}
