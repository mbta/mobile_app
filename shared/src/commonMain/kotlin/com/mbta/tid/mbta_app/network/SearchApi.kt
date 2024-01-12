package com.mbta.maple.kotlinmultiplatform.network

import com.mbta.tid.mbta_app.getPlatform
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/*class MbtaApi(httpClientEngine: HttpClientEngine = getPlatform().httpClientEngine) {
    private val httpClient =
        HttpClient(httpClientEngine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        useAlternativeNames = false
                    }
                )
            }
        }

    suspend fun searchResults(query: String): List<SearchResult> {
        val response: Stops =
            httpClient
                .get("https://api-v3.mbta.com/stops") {
                    url { parameters.append("filter[location_type]", "0,1") }
                    headers { append(HttpHeaders.Accept, "application/vnd.api+json") }
                }
                .body()
        return response.data.map(StopResource::asStop).sortedBy { it.name }
    }

    suspend fun getStop(stopId: String): Stop {
        val response: StopResponse =
            httpClient
                .get("https://api-v3.mbta.com/stops/${stopId}") {
                    headers { append(HttpHeaders.Accept, "application/vnd.api+json") }
                }
                .body()
        return response.data.asStop()
    }

    suspend fun getRoutePatternsForStop(stopId: String): List<RoutePattern> {
        val response: RoutePatternsResource =
            httpClient
                .get("https://api-v3.mbta.com/route_patterns") {
                    url {
                        parameters.append("filter[stop]", stopId)
                        parameters.append("fields[route_pattern]", "id,name")
                        parameters.append("fields[trip]", "id")
                        parameters.append("include", "representative_trip.shape")
                    }

                    headers { append(HttpHeaders.Accept, "application/vnd.api+json") }
                }
                .body()
        return response.asRoutePatterns()
    }

    suspend fun getPredictionsForStop(stopId: String): List<Prediction> {
        val response: Predictions =
            httpClient
                .get("https://api-v3.mbta.com/predictions") {
                    url {
                        parameters.append("page[limit]", "5")
                        parameters.append("sort", "departure_time")
                        parameters.append(
                            "fields[prediction]",
                            "status,schedule_relationship,direction_id,departure_time",
                        )
                        parameters.append(
                            "fields[route]",
                            "short_name,long_name,direction_names,direction_destinations",
                        )
                        parameters.append("include", "route")
                        parameters.append("filter[stop]", stopId)
                    }

                    headers { append(HttpHeaders.Accept, "application/vnd.api+json") }
                }
                .body()
        return response.asPredictions()
    }

    fun pollPredictionsForStop(
        stopId: String,
        interval: Duration = 30.seconds
    ): Flow<List<Prediction>> = flow {
        while (true) {
            emit(getPredictionsForStop(stopId))
            delay(interval)
        }
    }
}*/
