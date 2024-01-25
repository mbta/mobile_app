package com.mbta.tid.mbta_app

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.errors.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.Json

class Backend(engine: HttpClientEngine) {
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
        }

    companion object {
        val platformDefault
            get() = Backend(getPlatform().httpClientEngine)
    }

    @Throws(IOException::class, CancellationException::class, JsonConvertException::class)
    suspend fun getNearby(latitude: Double, longitude: Double): NearbyResponse =
        httpClient
            .get("https://mobile-app-backend-staging.mbtace.com/api/nearby") {
                url {
                    parameters.append("latitude", latitude.toString())
                    parameters.append("longitude", longitude.toString())
                }
            }
            .body()
}
