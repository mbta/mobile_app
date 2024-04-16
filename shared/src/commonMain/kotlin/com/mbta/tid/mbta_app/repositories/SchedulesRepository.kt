package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.path
import io.ktor.serialization.JsonConvertException
import io.ktor.utils.io.errors.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ISchedulesRepository {
    @Throws(
        IOException::class,
        CancellationException::class,
        JsonConvertException::class,
        ResponseException::class,
        HttpRequestTimeoutException::class
    )
    @DefaultArgumentInterop.Enabled
    suspend fun getSchedule(
        stopIds: List<String>,
        now: Instant = Clock.System.now()
    ): ScheduleResponse
}

class SchedulesRepository : ISchedulesRepository, KoinComponent {

    private val mobileBackendClient: MobileBackendClient by inject()

    @DefaultArgumentInterop.Enabled
    override suspend fun getSchedule(stopIds: List<String>, now: Instant): ScheduleResponse =
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

class MockScheduleRepository : ISchedulesRepository {
    override suspend fun getSchedule(stopIds: List<String>, now: Instant): ScheduleResponse {
        return ScheduleResponse(schedules = listOf(), trips = mapOf())
    }
}
