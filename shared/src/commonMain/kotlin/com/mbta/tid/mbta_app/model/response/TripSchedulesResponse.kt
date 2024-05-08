package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Schedule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class TripSchedulesResponse {
    @Serializable
    @SerialName("schedules")
    data class Schedules(val schedules: List<Schedule>) : TripSchedulesResponse()

    @Serializable
    @SerialName("stop_ids")
    data class StopIds(@SerialName("stop_ids") val stopIds: List<String>) : TripSchedulesResponse()

    @Serializable @SerialName("unknown") data object Unknown : TripSchedulesResponse()
}
