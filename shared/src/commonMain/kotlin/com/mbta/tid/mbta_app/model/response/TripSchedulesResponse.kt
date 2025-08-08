package com.mbta.tid.mbta_app.model.response

import com.mbta.tid.mbta_app.model.Schedule
import com.mbta.tid.mbta_app.model.Stop
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed class TripSchedulesResponse {
    @Serializable
    @SerialName("schedules")
    public data class Schedules(internal val schedules: List<Schedule>) : TripSchedulesResponse() {
        override fun stops(globalData: GlobalResponse): List<Stop> =
            schedules.mapNotNull { globalData.stops[it.stopId] }

        override fun routeId(): String? = schedules.map { it.routeId }.distinct().singleOrNull()

        override fun toString(): String = "[TripSchedulesResponse.Schedules]"
    }

    @Serializable
    @SerialName("stop_ids")
    public data class StopIds(@SerialName("stop_ids") internal val stopIds: List<String>) :
        TripSchedulesResponse() {
        override fun stops(globalData: GlobalResponse): List<Stop> =
            stopIds.mapNotNull { globalData.stops[it] }

        override fun routeId() = null
    }

    @Serializable
    @SerialName("unknown")
    public data object Unknown : TripSchedulesResponse() {
        override fun stops(globalData: GlobalResponse): List<Stop>? = null

        override fun routeId() = null
    }

    internal abstract fun stops(globalData: GlobalResponse): List<Stop>?

    internal abstract fun routeId(): String?
}
