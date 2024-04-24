package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlinx.datetime.Instant

class MockScheduleRepository : ISchedulesRepository {
    override suspend fun getSchedule(stopIds: List<String>, now: Instant): ScheduleResponse {
        return ScheduleResponse(schedules = listOf(), trips = mapOf())
    }

    override suspend fun getSchedule(stopIds: List<String>): ScheduleResponse {
        return ScheduleResponse(schedules = listOf(), trips = mapOf())
    }
}
