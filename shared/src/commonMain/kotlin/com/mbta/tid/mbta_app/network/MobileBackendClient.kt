package com.mbta.tid.mbta_app.network

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.getPlatform
import com.mbta.tid.mbta_app.json
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json

internal class MobileBackendClient(engine: HttpClientEngine, val appVariant: AppVariant) {
    constructor(appVariant: AppVariant) : this(getPlatform().httpClientEngine, appVariant)

    private val httpClient =
        HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
            install(WebSockets) { contentConverter = KotlinxWebsocketSerializationConverter(json) }
            install(ContentEncoding) { gzip(0.9F) }
            install(HttpTimeout) { requestTimeoutMillis = 8000 }
            defaultRequest { url(appVariant.backendRoot) }
            HttpResponseValidator {
                validateResponse { response ->
                    when (response.status.value) {
                        200 -> {}
                        304 -> {}
                        in 300..399 ->
                            throw RedirectResponseException(response, response.bodyAsText())
                        in 400..499 -> throw ClientRequestException(response, response.bodyAsText())
                        in 500..599 ->
                            throw ServerResponseException(response, response.bodyAsText())
                        else -> throw ResponseException(response, response.bodyAsText())
                    }
                }
            }
        }

    suspend fun get(request: HttpRequestBuilder.() -> Unit): HttpResponse {
        return httpClient.get(request)
    }

    suspend fun post(request: HttpRequestBuilder.() -> Unit): HttpResponse {
        return httpClient.post(request)
    }
}
