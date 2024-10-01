package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.TripShape
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.TripResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.path
import io.ktor.serialization.JsonConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ITripRepository {
    @Throws(
        IOException::class,
        kotlin.coroutines.cancellation.CancellationException::class,
        JsonConvertException::class,
        ResponseException::class,
        HttpRequestTimeoutException::class
    )
    suspend fun getTripSchedules(tripId: String): TripSchedulesResponse

    suspend fun getTrip(tripId: String): ApiResult<TripResponse>

    suspend fun getTripShape(tripId: String): ApiResult<TripShape>
}

class TripRepository : ITripRepository, KoinComponent {
    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getTripSchedules(tripId: String): TripSchedulesResponse =
        mobileBackendClient
            .get {
                url {
                    path("api/schedules")
                    parameter("trip_id", tripId)
                }
            }
            .body()

    override suspend fun getTrip(tripId: String): ApiResult<TripResponse> {
        try {
            val response =
                mobileBackendClient.get {
                    url {
                        path("api/trip")
                        parameter("trip_id", tripId)
                    }
                }

            if (response.status === HttpStatusCode.OK) {
                return ApiResult.Ok(data = json.decodeFromString(response.body()))
            } else {
                return ApiResult.Error(
                    code = response.status.value,
                    message = response.bodyAsText()
                )
            }
        } catch (e: Exception) {
            return ApiResult.Error(message = e.message ?: e.toString())
        }
    }

    override suspend fun getTripShape(tripId: String): ApiResult<TripShape> {
        try {
            val response =
                mobileBackendClient.get {
                    url {
                        path("api/trip/map")
                        parameter("trip_id", tripId)
                    }
                }

            if (response.status === HttpStatusCode.OK) {
                return ApiResult.Ok(data = json.decodeFromString(response.body()))
            } else {
                return ApiResult.Error(
                    code = response.status.value,
                    message = response.bodyAsText()
                )
            }
        } catch (e: Exception) {
            return ApiResult.Error(message = e.message ?: e.toString())
        }
    }
}

open class IdleTripRepository : ITripRepository {
    override suspend fun getTripSchedules(tripId: String): TripSchedulesResponse {
        return suspendCancellableCoroutine {}
    }

    override suspend fun getTrip(tripId: String): ApiResult<TripResponse> {
        return suspendCancellableCoroutine {}
    }

    override suspend fun getTripShape(tripId: String): ApiResult<TripShape> {
        return suspendCancellableCoroutine {}
    }
}
