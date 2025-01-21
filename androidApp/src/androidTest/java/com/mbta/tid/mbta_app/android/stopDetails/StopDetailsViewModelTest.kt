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
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockScheduleRepository
import junit.framework.TestCase.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

        val stopId = mutableStateOf<String?>("stop1")

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.STARTED)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                var stopId by remember { stopId }
                stopDetailsManagedVM(
                    stopId,
                    viewModel = viewModel,
                    globalResponse = null,
                    alertData = null,
                    pinnedRoutes = setOf()
                )
            }
        }

        composeTestRule.waitUntil { connectCount == 1 }

        assertEquals(1, connectCount)
        assertEquals(1, disconnectCount)
        assertEquals("stop1", viewModel.stopData.value?.stopId)

        stopId.value = "stop2"

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

        val stopId = mutableStateOf<String?>("stop1")

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                var stopId by remember { stopId }
                stopDetailsManagedVM(
                    stopId,
                    viewModel = viewModel,
                    globalResponse = null,
                    alertData = null,
                    pinnedRoutes = setOf()
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

        val stopId = mutableStateOf<String?>("stop1")

        assertNull(viewModel.stopDepartures.value)

        composeTestRule.setContent {
            var stopId by remember { stopId }
            stopDetailsManagedVM(
                stopId,
                viewModel = viewModel,
                globalResponse = GlobalResponse(objects),
                alertData = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf()
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

        var stopId = mutableStateOf("stop1")

        var checkPredictionsStaleCount = 0
        val errorBannerRepo =
            MockErrorBannerStateRepository(
                onCheckPredictionsStale = { checkPredictionsStaleCount += 1 }
            )

        val viewModel = StopDetailsViewModel(schedulesRepo, predictionsRepo, errorBannerRepo)

        composeTestRule.setContent {
            var stopId by remember { stopId }
            stopDetailsManagedVM(
                stopId,
                viewModel = viewModel,
                globalResponse = GlobalResponse(objects),
                alertData = AlertsStreamDataResponse(objects),
                pinnedRoutes = setOf(),
                checkPredictionsStaleInterval = 1.seconds
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 3000) { checkPredictionsStaleCount >= 2 }
    }
}
