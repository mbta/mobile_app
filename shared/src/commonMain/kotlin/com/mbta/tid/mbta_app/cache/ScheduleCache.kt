package com.mbta.tid.mbta_app.cache

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlin.getValue
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.koin.core.component.KoinComponent

public class ScheduleCache(
    private val keyedCache: IKeyedCache<Entry> =
        KeyedCache(CACHE_GROUP, CACHE_KEY_PREFIX, json.serializersModule.serializer())
) : KoinComponent {
    private companion object {
        private const val CACHE_GROUP = "scheduleCache"
        private const val CACHE_KEY_PREFIX: String = "stopSchedules"
    }

    @Serializable
    public data class Entry(
        val response: ScheduleResponse,
        val serviceDate: LocalDate,
        val stopId: String,
    )

    public suspend fun getSchedule(stopId: String, serviceDate: LocalDate): ScheduleResponse? =
        keyedCache.getEntry(stopId) { it.serviceDate != serviceDate }?.response

    public suspend fun putSchedule(
        stopId: String,
        serviceDate: LocalDate,
        schedules: ScheduleResponse,
    ): Unit = keyedCache.putEntry(stopId, Entry(schedules, serviceDate, stopId))

    public suspend fun deleteStaleSchedules(serviceDate: LocalDate): Unit =
        keyedCache.deleteStaleEntries { it.serviceDate != serviceDate }
}
