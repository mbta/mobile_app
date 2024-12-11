package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.TripShape
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ShapeWithStops
import com.mbta.tid.mbta_app.model.response.TripResponse
import com.mbta.tid.mbta_app.model.response.TripSchedulesResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.http.path
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ITripRepository {
    suspend fun getTripSchedules(tripId: String): ApiResult<TripSchedulesResponse>

    suspend fun getTrip(tripId: String): ApiResult<TripResponse>

    suspend fun getTripShape(tripId: String): ApiResult<TripShape>
}

class TripRepository : ITripRepository, KoinComponent {
    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getTripSchedules(tripId: String): ApiResult<TripSchedulesResponse> =
        ApiResult.runCatching {
            mobileBackendClient
                .get {
                    url {
                        path("api/schedules")
                        parameter("trip_id", tripId)
                    }
                }
                .body()
        }

    override suspend fun getTrip(tripId: String): ApiResult<TripResponse> =
        ApiResult.runCatching {
            val response =
                mobileBackendClient.get {
                    url {
                        path("api/trip")
                        parameter("trip_id", tripId)
                    }
                }

            return ApiResult.Ok(data = json.decodeFromString(response.body()))
        }

    override suspend fun getTripShape(tripId: String): ApiResult<TripShape> =
        ApiResult.runCatching {
            val response =
                mobileBackendClient.get {
                    url {
                        path("api/trip/map")
                        parameter("trip_id", tripId)
                    }
                }

            return ApiResult.Ok(data = json.decodeFromString(response.body()))
        }
}

open class IdleTripRepository : ITripRepository {
    override suspend fun getTripSchedules(tripId: String): ApiResult<TripSchedulesResponse> {
        return suspendCancellableCoroutine {}
    }

    override suspend fun getTrip(tripId: String): ApiResult<TripResponse> {
        return suspendCancellableCoroutine {}
    }

    override suspend fun getTripShape(tripId: String): ApiResult<TripShape> {
        return suspendCancellableCoroutine {}
    }
}

class MockTripRepository
@DefaultArgumentInterop.Enabled
constructor(
    var tripSchedulesResponse: TripSchedulesResponse = TripSchedulesResponse.Unknown,
    var tripResponse: TripResponse = TripResponse(ObjectCollectionBuilder().trip {}),
    var tripShape: TripShape = TripShape(ShapeWithStops(0, "", "", null, emptyList()))
) : ITripRepository {
    override suspend fun getTripSchedules(tripId: String): ApiResult<TripSchedulesResponse> {
        return ApiResult.Ok(tripSchedulesResponse)
    }

    override suspend fun getTrip(tripId: String): ApiResult<TripResponse> {
        return ApiResult.Ok(tripResponse)
    }

    override suspend fun getTripShape(tripId: String): ApiResult<TripShape> {
        return ApiResult.Ok(tripShape)
    }
}
