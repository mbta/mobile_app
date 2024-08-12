package com.mbta.tid.mbta_app.android.fetcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.android.util.ManagedPinnedRoutes
import com.mbta.tid.mbta_app.android.util.getGlobalData
import com.mbta.tid.mbta_app.android.util.getSchedule
import com.mbta.tid.mbta_app.android.util.managePinnedRoutes
import com.mbta.tid.mbta_app.android.util.subscribeToPredictions
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Outcome
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class RepositorySubscribersTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testGlobal() = runTest {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route()
        val routePattern = objects.routePattern(route)
        val globalData = GlobalResponse(objects, mapOf(stop.id to listOf(routePattern.id)))

        val requestSync = Channel<Unit>(Channel.RENDEZVOUS)
        val globalRepo =
            object : IGlobalRepository {
                override suspend fun getGlobalData(): GlobalResponse {
                    requestSync.receive()
                    return globalData
                }
            }

        var actualData: GlobalResponse? = globalData
        composeTestRule.setContent { actualData = getGlobalData(globalRepo) }

        composeTestRule.awaitIdle()
        assertNull(actualData)

        requestSync.send(Unit)
        composeTestRule.awaitIdle()
        assertEquals(globalData, actualData)
    }

    @Test
    fun testSchedule() = runTest {
        fun buildSomeSchedules(): ScheduleResponse {
            val objects = ObjectCollectionBuilder()
            objects.schedule()
            objects.schedule()
            return ScheduleResponse(objects)
        }
        val expectedSchedules1 = buildSomeSchedules()
        val expectedSchedules2 = buildSomeSchedules()
        val expectedSchedules3 = buildSomeSchedules()

        val stops1 = listOf("stop-a")
        val stops2 = listOf("stop-b")

        val time1 = Instant.parse("2024-08-12T10:15:14-06:00")
        val time2 = Instant.parse("2024-08-12T10:17:43-06:00")

        val requestSync = Channel<Unit>(Channel.RENDEZVOUS)
        val schedulesRepo =
            object : ISchedulesRepository {
                override suspend fun getSchedule(
                    stopIds: List<String>,
                    now: Instant
                ): ScheduleResponse {
                    requestSync.receive()
                    return if (stopIds == stops1 && now == time1) expectedSchedules1
                    else if (stopIds == stops1) expectedSchedules2 else expectedSchedules3
                }

                override suspend fun getSchedule(stopIds: List<String>): ScheduleResponse {
                    throw IllegalStateException("Can't getSchedule with no time in this mock")
                }
            }

        var stopIds by mutableStateOf(stops1)
        var now by mutableStateOf(time1)
        var actualSchedules: ScheduleResponse? = expectedSchedules1
        composeTestRule.setContent {
            actualSchedules = getSchedule(stopIds = stopIds, now = now, schedulesRepo)
        }

        composeTestRule.awaitIdle()
        assertNull(actualSchedules)

        requestSync.send(Unit)
        composeTestRule.awaitIdle()
        assertEquals(expectedSchedules1, actualSchedules)

        now = time2
        composeTestRule.awaitIdle()
        assertEquals(expectedSchedules1, actualSchedules)
        requestSync.send(Unit)
        composeTestRule.awaitIdle()
        assertEquals(expectedSchedules2, actualSchedules)

        stopIds = stops2
        composeTestRule.awaitIdle()
        assertEquals(expectedSchedules2, actualSchedules)
        requestSync.send(Unit)
        composeTestRule.awaitIdle()
        assertEquals(expectedSchedules3, actualSchedules)
    }

    @Test
    fun testPinnedRoutes() = runTest {
        val getSync = Channel<Unit>(Channel.RENDEZVOUS)
        val pinnedRoutesRepo =
            object : IPinnedRoutesRepository {
                var getCalls = 0
                var setCalls = Channel<Set<String>>(Channel.RENDEZVOUS)

                override suspend fun getPinnedRoutes(): Set<String> {
                    getSync.receive()
                    getCalls++
                    return setOf("$getCalls")
                }

                override suspend fun setPinnedRoutes(routes: Set<String>) {
                    setCalls.send(routes)
                }
            }
        val togglePinnedRouteUsecase = TogglePinnedRouteUsecase(pinnedRoutesRepo)

        var mpr: ManagedPinnedRoutes? = null
        composeTestRule.setContent {
            mpr = managePinnedRoutes(pinnedRoutesRepo, togglePinnedRouteUsecase)
        }

        composeTestRule.awaitIdle()
        assertNotNull(mpr)
        assertNull(mpr!!.pinnedRoutes)
        getSync.send(Unit)
        composeTestRule.awaitIdle()
        assertEquals(setOf("1"), mpr!!.pinnedRoutes)

        mpr!!.togglePinnedRoute("place-a")
        getSync.send(Unit)
        assertEquals(setOf("2", "place-a"), pinnedRoutesRepo.setCalls.receive())
        assertEquals(setOf("1"), mpr!!.pinnedRoutes)
        getSync.send(Unit)
        composeTestRule.awaitIdle()
        assertEquals(setOf("3"), mpr!!.pinnedRoutes)

        mpr!!.togglePinnedRoute("4")
        getSync.send(Unit)
        assertEquals(emptySet<String>(), pinnedRoutesRepo.setCalls.receive())
        getSync.send(Unit)
        composeTestRule.awaitIdle()
        assertEquals(setOf("5"), mpr!!.pinnedRoutes)
    }

    @Test
    fun testPredictions() = runTest {
        fun buildSomePredictions(): PredictionsStreamDataResponse {
            val objects = ObjectCollectionBuilder()
            objects.prediction()
            objects.prediction()
            return PredictionsStreamDataResponse(objects)
        }

        val predictionsRepo =
            object : IPredictionsRepository {
                val stopIdsChannel = Channel<List<String>>()
                lateinit var onReceive:
                    (Outcome<PredictionsStreamDataResponse?, SocketError>) -> Unit
                val disconnectChannel = Channel<Unit>()

                var isConnected = false

                override fun connect(
                    stopIds: List<String>,
                    onReceive: (Outcome<PredictionsStreamDataResponse?, SocketError>) -> Unit
                ) {
                    check(!isConnected) { "called connect when already connected" }
                    isConnected = true
                    launch { stopIdsChannel.send(stopIds) }
                    this.onReceive = onReceive
                }

                override fun disconnect() {
                    check(isConnected) { "called disconnect when not connected" }
                    isConnected = false
                    launch { disconnectChannel.send(Unit) }
                }
            }

        var stopIds by mutableStateOf(listOf("place-a"))
        var unmounted by mutableStateOf(false)
        var predictions: PredictionsStreamDataResponse? =
            PredictionsStreamDataResponse(ObjectCollectionBuilder())
        composeTestRule.setContent {
            if (!unmounted) predictions = subscribeToPredictions(stopIds, predictionsRepo)
        }

        composeTestRule.awaitIdle()
        assertEquals(listOf("place-a"), predictionsRepo.stopIdsChannel.receive())
        assertNull(predictions)

        val expectedPredictions1 = buildSomePredictions()
        predictionsRepo.onReceive(Outcome(expectedPredictions1, null))
        composeTestRule.awaitIdle()
        assertEquals(expectedPredictions1, predictions)

        stopIds = listOf("place-b")
        composeTestRule.awaitIdle()
        predictionsRepo.disconnectChannel.receive()
        assertEquals(listOf("place-b"), predictionsRepo.stopIdsChannel.receive())
        assertEquals(expectedPredictions1, predictions)

        val expectedPredictions2 = buildSomePredictions()
        predictionsRepo.onReceive(Outcome(expectedPredictions2, null))
        composeTestRule.awaitIdle()
        assertEquals(expectedPredictions2, predictions)

        unmounted = true
        composeTestRule.awaitIdle()
        predictionsRepo.disconnectChannel.receive()
    }
}
