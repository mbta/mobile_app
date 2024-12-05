package com.mbta.tid.mbta_app.android.state

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.mbta.tid.mbta_app.android.util.TimerViewModel
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class MockPredictionsRepository(private val scope: CoroutineScope) : IPredictionsRepository {
    val stopIdsChannel = Channel<List<String>>()
    lateinit var onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit
    lateinit var onMessage: (ApiResult<PredictionsByStopMessageResponse>) -> Unit
    var disconnectHook: () -> Unit = { println("original disconnect hook called") }

    override fun connect(
        stopIds: List<String>,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit
    ) {
        /* null-op */
    }

    override fun connectV2(
        stopIds: List<String>,
        onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit,
        onMessage: (ApiResult<PredictionsByStopMessageResponse>) -> Unit
    ) {

        this.onJoin = onJoin
        scope.launch { stopIdsChannel.send(stopIds) }
    }

    override var lastUpdated: Instant? = null

    override fun shouldForgetPredictions(predictionCount: Int) = false

    override fun disconnect() {
        disconnectHook()
    }
}

class SubscribeToPredictionsTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun testPredictions() = runTest {
        fun buildSomePredictions(): PredictionsByStopJoinResponse {
            val objects = ObjectCollectionBuilder()
            objects.prediction()
            objects.prediction()
            return PredictionsByStopJoinResponse(objects)
        }
        val predictionsRepo = MockPredictionsRepository(this)

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
        predictionsRepo.onJoin(ApiResult.Ok(expectedPredictions1))
        composeTestRule.awaitIdle()
        assertEquals(expectedPredictions1.toPredictionsStreamDataResponse(), predictions)

        stopIds = listOf("place-b")
        composeTestRule.awaitIdle()
        assertEquals(listOf("place-b"), predictionsRepo.stopIdsChannel.receive())
        predictionsRepo.onJoin(ApiResult.Ok(expectedPredictions1))
        composeTestRule.awaitIdle()
        assertEquals(expectedPredictions1.toPredictionsStreamDataResponse(), predictions)

        val expectedPredictions2 = buildSomePredictions()
        predictionsRepo.onJoin(ApiResult.Ok(expectedPredictions2))
        composeTestRule.awaitIdle()
        assertEquals(expectedPredictions2.toPredictionsStreamDataResponse(), predictions)

        unmounted = true
        composeTestRule.awaitIdle()
    }

    @Test
    fun testPredictionsOnClear() = runTest {
        var disconnectCalled = false
        val stopIds by mutableStateOf(listOf("place-a"))
        val mockPredictionsRepository = MockPredictionsRepository(this.backgroundScope)
        mockPredictionsRepository.disconnectHook = { disconnectCalled = true }

        val viewModelStore = ViewModelStore()
        val viewModelProvider =
            ViewModelProvider(
                viewModelStore,
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return PredictionsViewModel(
                            stopIds,
                            mockPredictionsRepository,
                            TimerViewModel(1.seconds)
                        )
                            as T
                    }
                }
            )
        viewModelProvider.get(PredictionsViewModel::class)
        viewModelStore.clear()
        assertEquals(true, disconnectCalled)
    }
}
