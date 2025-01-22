package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.PatternsByStop
import com.mbta.tid.mbta_app.model.StopDetailsDepartures
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockScheduleRepository
import junit.framework.TestCase.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test

class StopDetailsViewModelTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testLoadStopDetailsLoadsSchedulesAndPredictions() = runTest {
        val objects = ObjectCollectionBuilder()
        objects.prediction {}
        objects.trip {}
        objects.vehicle { currentStatus = Vehicle.CurrentStatus.InTransitTo }
        objects.schedule {}
        objects.trip {}

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects))
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel = StopDetailsViewModel(schedulesRepo, predictionsRepo, errorBannerRepo)

        composeTestRule.setContent { LaunchedEffect(Unit) { viewModel.loadStopDetails("stop") } }

        composeTestRule.waitUntil { viewModel.stopData?.value?.predictionsByStop != null }

        assertEquals("stop", viewModel.stopData.value?.stopId)
        assertNotNull(viewModel.stopData?.value?.predictionsByStop)
        assertNotNull(viewModel.stopData?.value?.schedules != null)
    }

    @Test
    fun testRejoinStopPredictionsWhenStopDataSet() = runTest {
        val objects = ObjectCollectionBuilder()
        objects.prediction {}
        objects.trip {}
        objects.vehicle { currentStatus = Vehicle.CurrentStatus.InTransitTo }
        objects.schedule {}
        objects.trip {}

        var connectCount = 0

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects)),
                onConnectV2 = { connectCount += 1 }
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel = StopDetailsViewModel(schedulesRepo, predictionsRepo, errorBannerRepo)

        composeTestRule.setContent { LaunchedEffect(Unit) { viewModel.loadStopDetails("stop") } }

        composeTestRule.waitUntil { connectCount == 1 }

        assertEquals(viewModel.stopData.value?.stopId, "stop")
        assertNotNull(viewModel.stopData?.value?.predictionsByStop)
        assertEquals(1, connectCount)
        viewModel.rejoinStopPredictions()
        composeTestRule.awaitIdle()

        composeTestRule.waitUntil { connectCount == 2 }
        assertEquals(2, connectCount)
    }

    @Test
    fun testRejoinStopPredictionsWhenNoStop() = runTest {
        val objects = ObjectCollectionBuilder()
        objects.prediction {}
        objects.trip {}
        objects.vehicle { currentStatus = Vehicle.CurrentStatus.InTransitTo }
        objects.schedule {}
        objects.trip {}

        var connectCount = 0

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects)),
                onConnectV2 = { connectCount += 1 }
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel = StopDetailsViewModel(schedulesRepo, predictionsRepo, errorBannerRepo)

        composeTestRule.setContent { LaunchedEffect(Unit) { viewModel.loadStopDetails("stop") } }

        composeTestRule.waitUntil { connectCount == 1 }

        assertEquals(viewModel.stopData.value?.stopId, "stop")
        assertNotNull(viewModel.stopData?.value?.predictionsByStop)
        assertEquals(1, connectCount)

        viewModel.handleStopChange(null)
        composeTestRule.awaitIdle()

        assertNull(viewModel.stopData?.value)
        viewModel.rejoinStopPredictions()
        // nothing to rejoin
        assertEquals(1, connectCount)
    }

    @Test
    fun testLeaveStopPredictions() = runTest {
        val objects = ObjectCollectionBuilder()
        objects.prediction {}
        objects.trip {}
        objects.vehicle { currentStatus = Vehicle.CurrentStatus.InTransitTo }
        objects.schedule {}
        objects.trip {}

        var connectCount = 0
        var disconnectCount = 0

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects)),
                onConnectV2 = { connectCount += 1 },
                onDisconnect = { disconnectCount += 1 }
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel = StopDetailsViewModel(schedulesRepo, predictionsRepo, errorBannerRepo)

        composeTestRule.setContent { LaunchedEffect(Unit) { viewModel.loadStopDetails("stop") } }

        composeTestRule.awaitIdle()

        assertEquals(viewModel.stopData.value?.stopId, "stop")
        assertNotNull(viewModel.stopData?.value?.predictionsByStop)
        assertEquals(1, connectCount)
        assertEquals(0, disconnectCount)

        viewModel.leaveStopPredictions()

        composeTestRule.waitUntil { disconnectCount == 1 }
        assertEquals(1, disconnectCount)
    }

    @Test
    fun testSetDepartures() {
        val predictionsRepo = MockPredictionsRepository()

        val schedulesRepo = MockScheduleRepository()

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel = StopDetailsViewModel(schedulesRepo, predictionsRepo, errorBannerRepo)

        assertNull(viewModel.stopDepartures.value)

        viewModel.setDepartures(StopDetailsDepartures(emptyList()))

        assertEquals(emptyList<PatternsByStop>(), viewModel.stopDepartures.value?.routes)
    }

    @Test
    fun testHandleStopChange() = runTest {
        val objects = ObjectCollectionBuilder()

        var connectCount = 0
        var disconnectCount = 0

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects)),
                onConnectV2 = { connectCount += 1 },
                onDisconnect = { disconnectCount += 1 }
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel = StopDetailsViewModel(schedulesRepo, predictionsRepo, errorBannerRepo)

        composeTestRule.setContent {
            LaunchedEffect(Unit) {
                viewModel.loadStopDetails("stop1")
                viewModel.handleStopChange("stop2")
            }
        }

        composeTestRule.waitUntil { connectCount == 2 }

        assertEquals(1, disconnectCount)
        assertEquals(2, connectCount)
        assertEquals("stop2", viewModel.stopData.value?.stopId)
        assertNotNull(viewModel.stopData?.value?.predictionsByStop)
        assertNotNull(viewModel.stopData?.value?.schedules)
    }

    @Test
    fun testManagerHandlesStopChange() = runTest {
        val objects = ObjectCollectionBuilder()

        var connectCount = 0
        var disconnectCount = 0

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects)),
                onConnectV2 = { connectCount += 1 },
                onDisconnect = { disconnectCount += 1 }
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel = StopDetailsViewModel(schedulesRepo, predictionsRepo, errorBannerRepo)

        val stopFilters =
            mutableStateOf<StopDetailsPageFilters?>(StopDetailsPageFilters("stop1", null, null))

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.STARTED)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                var stopFilters by remember { stopFilters }
                stopDetailsManagedVM(
                    stopFilters,
                    viewModel = viewModel,
                    globalResponse = null,
                    alertData = null,
                    pinnedRoutes = setOf(),
                    updateStopFilter = { _, _ -> },
                    updateTripFilter = { _, _ -> }
                )
            }
        }

        composeTestRule.waitUntil { connectCount == 1 }

        assertEquals(1, connectCount)
        assertEquals(1, disconnectCount)
        assertEquals("stop1", viewModel.stopData.value?.stopId)

        stopFilters.value = StopDetailsPageFilters("stop2", null, null)

        composeTestRule.waitUntil { connectCount == 2 }

        assertEquals(2, connectCount)
        assertEquals(2, disconnectCount)
        assertEquals("stop2", viewModel.stopData.value?.stopId)
    }

    @Test
    fun testManagerHandlesBackgrounding() = runTest {
        val objects = ObjectCollectionBuilder()

        var connectCount = 0
        var disconnectCount = 0

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects)),
                onConnectV2 = { connectCount += 1 },
                onDisconnect = { disconnectCount += 1 }
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel = StopDetailsViewModel(schedulesRepo, predictionsRepo, errorBannerRepo)

        val stopFilters = mutableStateOf(StopDetailsPageFilters("stop1", null, null))

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                var stopFilters by remember { stopFilters }
                stopDetailsManagedVM(
                    stopFilters,
                    viewModel = viewModel,
                    globalResponse = null,
                    alertData = null,
                    pinnedRoutes = setOf(),
                    updateStopFilter = { _, _ -> },
                    updateTripFilter = { _, _ -> }
                )
            }
        }

        composeTestRule.waitUntil {
            // In resumed state, so joined 1 time for stop, 1 time for resume.
            // Need to start with lifecycle resumed in order to test pause
            connectCount == 2
        }

        assertEquals(2, connectCount)
        assertEquals(1, disconnectCount)
        assertEquals("stop1", viewModel.stopData.value?.stopId)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }

        composeTestRule.waitUntil { disconnectCount == 2 }

        assertEquals(2, connectCount)
        assertEquals(2, disconnectCount)
        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }

        composeTestRule.waitUntil { connectCount == 3 }

        assertEquals(3, connectCount)
        assertEquals(2, disconnectCount)
    }

    @Test
    fun testManagerSetsDeparturesOnChange() {
        val objects = ObjectCollectionBuilder()
        objects.stop { id = "stop1" }

        val predictionsRepo =
            MockPredictionsRepository(
                connectV2Outcome = ApiResult.Ok(PredictionsByStopJoinResponse(objects))
            )

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val errorBannerRepo = MockErrorBannerStateRepository()

        val viewModel = StopDetailsViewModel(schedulesRepo, predictionsRepo, errorBannerRepo)

        val stopFilters = mutableStateOf(StopDetailsPageFilters("stop1", null, null))

        assertNull(viewModel.stopDepartures.value)

        composeTestRule.setContent {
            var stopFilters by remember { stopFilters }
            stopDetailsManagedVM(
                stopFilters,
                viewModel = viewModel,
                globalResponse = GlobalResponse(objects),
                alertData = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                updateStopFilter = { _, _ -> },
                updateTripFilter = { _, _ -> }
            )
        }

        composeTestRule.waitUntil { viewModel.stopDepartures.value != null }

        assertNotNull(viewModel.stopDepartures.value)
    }

    @Test
    fun testManagerChecksPredictionsStale() {
        val objects = ObjectCollectionBuilder()
        objects.prediction()

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val predictionsOnJoin = PredictionsByStopJoinResponse(objects)
        val predictionsRepo = MockPredictionsRepository({}, {}, {}, null, predictionsOnJoin)

        predictionsRepo.lastUpdated = Clock.System.now()

        val stopFilters = mutableStateOf(StopDetailsPageFilters("stop1", null, null))

        var checkPredictionsStaleCount = 0
        val errorBannerRepo =
            MockErrorBannerStateRepository(
                onCheckPredictionsStale = { checkPredictionsStaleCount += 1 }
            )

        val viewModel = StopDetailsViewModel(schedulesRepo, predictionsRepo, errorBannerRepo)

        composeTestRule.setContent {
            var stopFilters by remember { stopFilters }
            stopDetailsManagedVM(
                stopFilters,
                viewModel = viewModel,
                globalResponse = GlobalResponse(objects),
                alertData = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                checkPredictionsStaleInterval = 1.seconds,
                updateStopFilter = { _, _ -> },
                updateTripFilter = { _, _ -> }
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 3000) { checkPredictionsStaleCount >= 2 }
    }

    @Test
    fun testManagersAppliesStopFilterAutomaticallyOnDepartureChange() = runTest {
        val objects = ObjectCollectionBuilder()

        val now = Clock.System.now()

        val route = objects.route {}
        val stop = objects.stop {}

        val tripId = "trip"
        val routePattern = objects.routePattern(route) { representativeTripId = tripId }
        val trip1 =
            objects.trip(routePattern) {
                id = tripId
                directionId = 0
                stopIds = listOf(stop.id)
                routePatternId = routePattern.id
            }
        objects.schedule {
            routeId = route.id
            stopId = stop.id
            stopSequence = 0
            departureTime = now.plus(10.minutes)
            trip = trip1
        }

        objects.prediction()

        val schedulesRepo = MockScheduleRepository(ScheduleResponse(objects))

        val predictionsOnJoin = PredictionsByStopJoinResponse(objects)
        val predictionsRepo = MockPredictionsRepository({}, {}, {}, null, predictionsOnJoin)
        val errorBannerRepo = MockErrorBannerStateRepository()

        val stopFilters = mutableStateOf(StopDetailsPageFilters(stop.id, null, null))

        val viewModel = StopDetailsViewModel(schedulesRepo, predictionsRepo, errorBannerRepo)

        var newStopFilter: StopDetailsFilter? = null

        composeTestRule.setContent {
            var stopFilters by remember { stopFilters }
            stopDetailsManagedVM(
                stopFilters,
                viewModel = viewModel,
                globalResponse = GlobalResponse(objects),
                alertData = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                checkPredictionsStaleInterval = 1.seconds,
                updateStopFilter = { _, filter -> newStopFilter = filter },
                updateTripFilter = { _, _ -> }
            )

            LaunchedEffect(null) {
                viewModel.setDepartures(
                    StopDetailsDepartures.fromData(
                        stop,
                        GlobalResponse(objects),
                        ScheduleResponse(objects),
                        PredictionsStreamDataResponse(objects),
                        AlertsStreamDataResponse(objects),
                        setOf(),
                        now,
                        false
                    )
                )
            }
        }

        composeTestRule.waitUntil {
            newStopFilter == StopDetailsFilter(route.id, routePattern.directionId)
        }

        assertEquals(StopDetailsFilter(route.id, routePattern.directionId), newStopFilter)
    }

    @Test
    fun testManagerAppliesTripFilterAutomaticallyOnDepartureChange() = runTest {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}

        val now = Clock.System.now()

        val route = objects.route {}

        val tripId = "trip"
        val routePattern = objects.routePattern(route) { representativeTripId = tripId }
        val trip1 =
            objects.trip(routePattern) {
                id = tripId
                directionId = 0
                stopIds = listOf(stop.id)
                routePatternId = routePattern.id
            }
        val schedule =
            objects.schedule {
                routeId = route.id
                stopId = stop.id
                stopSequence = 0
                departureTime = now.plus(10.minutes)
                trip = trip1
            }

        objects.prediction(schedule) { departureTime = now.plus(10.minutes) }

        val viewModel =
            StopDetailsViewModel(
                MockScheduleRepository(),
                MockPredictionsRepository(),
                MockErrorBannerStateRepository()
            )

        val stopFilters =
            mutableStateOf(StopDetailsPageFilters(stop.id, StopDetailsFilter(route.id, 0), null))

        var newTripFilter: TripDetailsFilter? = null

        composeTestRule.setContent {
            var stopFilters by remember { stopFilters }
            stopDetailsManagedVM(
                stopFilters,
                viewModel = viewModel,
                globalResponse = GlobalResponse(objects),
                alertData = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                checkPredictionsStaleInterval = 1.seconds,
                updateStopFilter = { _, _ -> },
                updateTripFilter = { _, tripFilter -> newTripFilter = tripFilter }
            )

            LaunchedEffect(null) {
                viewModel.setDepartures(
                    StopDetailsDepartures.fromData(
                        stop,
                        GlobalResponse(objects),
                        ScheduleResponse(objects),
                        PredictionsStreamDataResponse(objects),
                        AlertsStreamDataResponse(objects),
                        setOf(),
                        now,
                        false
                    )
                )
            }
        }

        val expectedTripFilter = TripDetailsFilter(trip1.id, null, 0, false)

        composeTestRule.waitUntil { newTripFilter == expectedTripFilter }
        kotlin.test.assertEquals(expectedTripFilter, newTripFilter)
    }

    @Test
    fun testManagerAppliesTripFilterAutomaticallyOnFilterChange() = runTest {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop {}

        val now = Clock.System.now()

        val route = objects.route {}

        val tripId = "trip"
        val routePattern = objects.routePattern(route) { representativeTripId = tripId }
        val trip1 =
            objects.trip(routePattern) {
                id = tripId
                directionId = 0
                stopIds = listOf(stop.id)
                routePatternId = routePattern.id
            }
        val schedule =
            objects.schedule {
                routeId = route.id
                stopId = stop.id
                stopSequence = 0
                departureTime = now.plus(10.minutes)
                trip = trip1
            }

        objects.prediction(schedule) { departureTime = now.plus(10.minutes) }

        val viewModel =
            StopDetailsViewModel(
                MockScheduleRepository(),
                MockPredictionsRepository(),
                MockErrorBannerStateRepository()
            )

        // There are no trips in direction 1
        val stopFilters =
            mutableStateOf(StopDetailsPageFilters(stop.id, StopDetailsFilter(route.id, 1), null))

        var newTripFilter: TripDetailsFilter? = null

        composeTestRule.setContent {
            var filters by remember { stopFilters }
            stopDetailsManagedVM(
                filters,
                viewModel = viewModel,
                globalResponse = GlobalResponse(objects),
                alertData = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                checkPredictionsStaleInterval = 1.seconds,
                updateStopFilter = { _, _ -> },
                updateTripFilter = { _, tripFilter -> newTripFilter = tripFilter }
            )

            LaunchedEffect(null) {
                viewModel.setDepartures(
                    StopDetailsDepartures.fromData(
                        stop,
                        GlobalResponse(objects),
                        ScheduleResponse(objects),
                        PredictionsStreamDataResponse(objects),
                        AlertsStreamDataResponse(objects),
                        setOf(),
                        now,
                        false
                    )
                )

                stopFilters.value =
                    StopDetailsPageFilters(stop.id, StopDetailsFilter(route.id, 0), null)
            }
        }

        val expectedTripFilter = TripDetailsFilter(trip1.id, null, 0, false)

        composeTestRule.waitUntil { newTripFilter == expectedTripFilter }
        kotlin.test.assertEquals(expectedTripFilter, newTripFilter)
    }
}
