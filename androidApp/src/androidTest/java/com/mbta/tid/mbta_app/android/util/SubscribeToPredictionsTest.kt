package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.android.state.subscribeToPredictions
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SubscribeToPredictionsTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testPredictions() = runTest {
        fun buildSomePredictions(): PredictionsByStopJoinResponse {
            val objects = ObjectCollectionBuilder()
            objects.prediction()
            objects.prediction()
            return PredictionsByStopJoinResponse(objects)
        }

        val predictionsRepo =
            object : IPredictionsRepository {
                val stopIdsChannel = Channel<List<String>>()
                lateinit var onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit
                val disconnectChannel = Channel<Unit>()

                var isConnected = false

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
                    check(!isConnected) { "called connect when already connected" }
                    isConnected = true
                    launch { stopIdsChannel.send(stopIds) }
                    this.onJoin = onJoin
                }

                override var lastUpdated: Instant? = null

                override fun shouldForgetPredictions(predictionCount: Int) = false

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
        predictionsRepo.onJoin(ApiResult.Ok(expectedPredictions1))
        composeTestRule.awaitIdle()
        assertEquals(expectedPredictions1.toPredictionsStreamDataResponse(), predictions)

        stopIds = listOf("place-b")
        composeTestRule.awaitIdle()
        predictionsRepo.disconnectChannel.receive()
        assertEquals(listOf("place-b"), predictionsRepo.stopIdsChannel.receive())
        assertEquals(expectedPredictions1.toPredictionsStreamDataResponse(), predictions)

        val expectedPredictions2 = buildSomePredictions()
        predictionsRepo.onJoin(ApiResult.Ok(expectedPredictions2))
        composeTestRule.awaitIdle()
        assertEquals(expectedPredictions2.toPredictionsStreamDataResponse(), predictions)

        unmounted = true
        composeTestRule.awaitIdle()
        predictionsRepo.disconnectChannel.receive()
    }
}
