package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Outcome
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SubscribeToPredictionsTest {
    @get:Rule val composeTestRule = createComposeRule()

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