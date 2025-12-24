package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.NextScheduleResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.http.path
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public interface ISchedulesRepository {
    public suspend fun getSchedule(
        stopIds: List<String>,
        now: EasternTimeInstant,
    ): ApiResult<ScheduleResponse>

    public suspend fun getSchedule(stopIds: List<String>): ApiResult<ScheduleResponse>

    public suspend fun getNextSchedule(
        route: LineOrRoute,
        stopId: String,
        directionId: Int,
        now: EasternTimeInstant,
    ): ApiResult<NextScheduleResponse>
}

internal class SchedulesRepository : ISchedulesRepository, KoinComponent {

    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getSchedule(
        stopIds: List<String>,
        now: EasternTimeInstant,
    ): ApiResult<ScheduleResponse> =
        ApiResult.runCatching {
            mobileBackendClient
                .get {
                    timeout { requestTimeoutMillis = 4000 }
                    url {
                        path("api/schedules")
                        parameters.append("stop_ids", stopIds.joinToString(separator = ","))
                        parameters.append("date_time", now.toString())
                    }
                }
                .body()
        }

    override suspend fun getSchedule(stopIds: List<String>): ApiResult<ScheduleResponse> {
        return getSchedule(stopIds, EasternTimeInstant.now())
    }

    override suspend fun getNextSchedule(
        route: LineOrRoute,
        stopId: String,
        directionId: Int,
        now: EasternTimeInstant,
    ): ApiResult<NextScheduleResponse> =
        ApiResult.runCatching {
            mobileBackendClient
                .get {
                    timeout { requestTimeoutMillis = 4000 }
                    url {
                        path("api/schedules/next")
                        parameters.append(
                            "route",
                            when (route) {
                                is LineOrRoute.Line ->
                                    route.routes.joinToString(separator = ",") { it.id.idText }
                                is LineOrRoute.Route -> route.id.idText
                            },
                        )
                        parameters.append("stop", stopId)
                        parameters.append("direction", directionId.toString())
                        parameters.append("date_time", now.toString())
                    }
                }
                .body()
        }
}

public class MockScheduleRepository(
    private val response: ApiResult<ScheduleResponse>,
    private val nextResponse: ApiResult<NextScheduleResponse>,
    private val callback: (stopIds: List<String>) -> Unit = {},
) : ISchedulesRepository {

    @DefaultArgumentInterop.Enabled
    public constructor(
        scheduleResponse: ScheduleResponse = ScheduleResponse(listOf(), mapOf()),
        nextScheduleResponse: NextScheduleResponse = NextScheduleResponse(null),
        callback: (stopIds: List<String>) -> Unit = {},
    ) : this(ApiResult.Ok(scheduleResponse), ApiResult.Ok(nextScheduleResponse), callback)

    public constructor() :
        this(
            scheduleResponse = ScheduleResponse(schedules = listOf(), trips = mapOf()),
            callback = {},
        )

    override suspend fun getSchedule(
        stopIds: List<String>,
        now: EasternTimeInstant,
    ): ApiResult<ScheduleResponse> {
        callback(stopIds)
        return response
    }

    override suspend fun getSchedule(stopIds: List<String>): ApiResult<ScheduleResponse> {
        callback(stopIds)
        return response
    }

    override suspend fun getNextSchedule(
        route: LineOrRoute,
        stopId: String,
        directionId: Int,
        now: EasternTimeInstant,
    ): ApiResult<NextScheduleResponse> {
        return nextResponse
    }
}

public class IdleScheduleRepository : ISchedulesRepository {
    override suspend fun getSchedule(
        stopIds: List<String>,
        now: EasternTimeInstant,
    ): ApiResult<ScheduleResponse> {
        return suspendCancellableCoroutine {}
    }

    override suspend fun getSchedule(stopIds: List<String>): ApiResult<ScheduleResponse> {
        return suspendCancellableCoroutine {}
    }

    override suspend fun getNextSchedule(
        route: LineOrRoute,
        stopId: String,
        directionId: Int,
        now: EasternTimeInstant,
    ): ApiResult<NextScheduleResponse> {
        return suspendCancellableCoroutine {}
    }
}
