package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Schedule
import com.mbta.tid.mbta_app.model.Stop
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class TripSchedulesResponse {
    @Serializable
    @SerialName("schedules")
    data class Schedules(val schedules: List<Schedule>) : TripSchedulesResponse() {
        override fun stops(globalData: GlobalResponse): List<Stop> =
            schedules.mapNotNull { globalData.stops[it.stopId] }

        override fun routeId(): String? = schedules.map { it.routeId }.distinct().singleOrNull()

        override fun toString() = "[TripSchedulesResponse.Schedules]"
    }

    @Serializable
    @SerialName("stop_ids")
    data class StopIds(@SerialName("stop_ids") val stopIds: List<String>) :
        TripSchedulesResponse() {
        override fun stops(globalData: GlobalResponse): List<Stop> =
            stopIds.mapNotNull { globalData.stops[it] }

        override fun routeId() = null
    }

    @Serializable
    @SerialName("unknown")
    data object Unknown : TripSchedulesResponse() {
        override fun stops(globalData: GlobalResponse): List<Stop>? = null

        override fun routeId() = null
    }

    abstract fun stops(globalData: GlobalResponse): List<Stop>?

    abstract fun routeId(): String?
}
