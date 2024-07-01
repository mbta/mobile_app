package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.TripShapeResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.parameter
import io.ktor.http.path
import io.ktor.serialization.JsonConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// TODO: rename
interface ITripSchedulesRepository {
    @Throws(
        IOException::class,
        kotlin.coroutines.cancellation.CancellationException::class,
        JsonConvertException::class,
        ResponseException::class,
        HttpRequestTimeoutException::class
    )
    suspend fun getTripSchedules(tripId: String): TripSchedulesResponse

    suspend fun getTripShape(tripId: String): TripShapeResponse
}

class TripSchedulesRepository : ITripSchedulesRepository, KoinComponent {
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

    override suspend fun getTripShape(tripId: String): TripShapeResponse =
        mobileBackendClient
            .get {
                url {
                    path("api/trip/map")
                    parameter("trip_id", tripId)
                }
            }
            .body()
}

class IdleTripSchedulesRepository : ITripSchedulesRepository {
    override suspend fun getTripSchedules(tripId: String): TripSchedulesResponse {
        return suspendCancellableCoroutine {}
    }

    override suspend fun getTripShape(tripId: String): TripShapeResponse {
        return suspendCancellableCoroutine {}
    }
}
