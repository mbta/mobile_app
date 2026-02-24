package com.mbta.tid.mbta_app.cache

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class ScheduleCacheTest {
    @Test
    fun `returns null if schedule data isn't cached`() = runBlocking {
        val cache = ScheduleCache(MockKeyedCache())

        val serviceDate = LocalDate.fromEpochDays(740032)
        assertNull(cache.getSchedule("stopId", serviceDate))
    }

    @Test
    fun `returns cached schedules when key matches`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val trip = objects.trip {}
        val schedule = objects.schedule { tripId = trip.id }

        val serviceDate = LocalDate.fromEpochDays(740032)
        val entry =
            ScheduleCache.Entry(
                ScheduleResponse(listOf(schedule), mapOf(trip.id to trip)),
                serviceDate,
                "stopId",
            )
        val cache = ScheduleCache(MockKeyedCache(mutableMapOf(entry.stopId to entry)))

        val cachedData = cache.getSchedule(entry.stopId, serviceDate)
        val firstSchedule = cachedData?.schedules?.first()
        assertEquals(entry.response.schedules.first().id, firstSchedule?.id)
        assertEquals(
            entry.response.trips.getValue(firstSchedule?.tripId!!).id,
            cachedData.trips.getValue(firstSchedule.tripId).id,
        )
    }

    @Test
    fun `deletes cached schedules when stale`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val trip = objects.trip {}
        val schedule = objects.schedule { tripId = trip.id }

        val serviceDate = LocalDate.fromEpochDays(740032)
        val entry =
            ScheduleCache.Entry(
                ScheduleResponse(listOf(schedule), mapOf(trip.id to trip)),
                serviceDate,
                "stopId",
            )
        val cacheMap = mutableMapOf(entry.stopId to entry)
        val cache = ScheduleCache(MockKeyedCache(cacheMap))

        assertNull(cache.getSchedule(entry.stopId, serviceDate.plus(1, DateTimeUnit.DAY)))
        assertFalse(cacheMap.containsKey(entry.stopId))
    }

    @Test
    fun `deletes stale cached schedules on invalidation`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val trip = objects.trip {}
        val schedule = objects.schedule { tripId = trip.id }

        val serviceDate = LocalDate.fromEpochDays(740032)
        val entry =
            ScheduleCache.Entry(
                ScheduleResponse(listOf(schedule), mapOf(trip.id to trip)),
                serviceDate,
                "stopId",
            )
        val cacheMap = mutableMapOf(entry.stopId to entry)
        val cache = ScheduleCache(MockKeyedCache(cacheMap))

        cache.deleteStaleSchedules(serviceDate.plus(1, DateTimeUnit.DAY))
        assertFalse(cacheMap.containsKey(entry.stopId))
    }

    @Test
    fun `inserts schedule data on put`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val trip = objects.trip {}
        val schedule = objects.schedule { tripId = trip.id }

        val serviceDate = LocalDate.fromEpochDays(740032)
        val entry =
            ScheduleCache.Entry(
                ScheduleResponse(listOf(schedule), mapOf(trip.id to trip)),
                serviceDate,
                "stopId",
            )
        val cacheMap = mutableMapOf<String, ScheduleCache.Entry>()
        val cache = ScheduleCache(MockKeyedCache(cacheMap))

        cache.putSchedule(entry.stopId, serviceDate, entry.response)
        assertEquals(cacheMap.get(entry.stopId), entry)
    }
}
