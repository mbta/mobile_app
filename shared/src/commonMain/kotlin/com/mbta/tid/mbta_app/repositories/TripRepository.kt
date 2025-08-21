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

public interface ITripRepository {
    public suspend fun getTripSchedules(tripId: String): ApiResult<TripSchedulesResponse>

    public suspend fun getTrip(tripId: String): ApiResult<TripResponse>

    public suspend fun getTripShape(tripId: String): ApiResult<TripShape>
}

internal class TripRepository : ITripRepository, KoinComponent {
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

internal open class IdleTripRepository : ITripRepository {
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

public class MockTripRepository
@DefaultArgumentInterop.Enabled
constructor(
    internal var tripSchedulesResponse: TripSchedulesResponse = TripSchedulesResponse.Unknown,
    internal var tripResponse: TripResponse = TripResponse(ObjectCollectionBuilder().trip {}),
    internal var tripShape: TripShape = TripShape(ShapeWithStops(0, "", "", null, emptyList())),
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
