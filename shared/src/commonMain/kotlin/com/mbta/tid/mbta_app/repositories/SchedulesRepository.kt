package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.http.path
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ISchedulesRepository {
    suspend fun getSchedule(stopIds: List<String>, now: Instant): ApiResult<ScheduleResponse>

    suspend fun getSchedule(stopIds: List<String>): ApiResult<ScheduleResponse>
}

class SchedulesRepository : ISchedulesRepository, KoinComponent {

    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun getSchedule(
        stopIds: List<String>,
        now: Instant
    ): ApiResult<ScheduleResponse> =
        ApiResult.runCatching {
            mobileBackendClient
                .get {
                    url {
                        path("api/schedules")
                        parameters.append("stop_ids", stopIds.joinToString(separator = ","))
                        parameters.append("date_time", now.toString())
                    }
                }
                .body()
        }

    override suspend fun getSchedule(stopIds: List<String>): ApiResult<ScheduleResponse> {
        return getSchedule(stopIds, Clock.System.now())
    }
}

class MockScheduleRepository(
    private val scheduleResponse: ScheduleResponse = ScheduleResponse(listOf(), mapOf()),
    private val callback: (stopIds: List<String>) -> Unit = {}
) : ISchedulesRepository {
    constructor() :
        this(
            scheduleResponse = ScheduleResponse(schedules = listOf(), trips = mapOf()),
            callback = {}
        )

    override suspend fun getSchedule(
        stopIds: List<String>,
        now: Instant
    ): ApiResult<ScheduleResponse> {
        callback(stopIds)
        return ApiResult.Ok(scheduleResponse)
    }

    override suspend fun getSchedule(stopIds: List<String>): ApiResult<ScheduleResponse> {
        callback(stopIds)
        return ApiResult.Ok(scheduleResponse)
    }
}

class IdleScheduleRepository : ISchedulesRepository {
    override suspend fun getSchedule(
        stopIds: List<String>,
        now: Instant
    ): ApiResult<ScheduleResponse> {
        return suspendCancellableCoroutine {}
    }

    override suspend fun getSchedule(stopIds: List<String>): ApiResult<ScheduleResponse> {
        return suspendCancellableCoroutine {}
    }
}
