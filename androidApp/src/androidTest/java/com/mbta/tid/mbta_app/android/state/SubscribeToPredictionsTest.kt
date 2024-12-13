package com.mbta.tid.mbta_app.android.state

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SubscribeToPredictionsTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun testPredictions() = runTest {
        val objects = ObjectCollectionBuilder()
        objects.prediction()
        objects.prediction()
        val predictionsOnJoin = PredictionsByStopJoinResponse(objects)

        var connectProps: List<String>? = null
        var disconnectCount = 0

        val predictionsRepo =
            MockPredictionsRepository(
                {},
                { stops -> connectProps = stops },
                { disconnectCount += 1 },
                null,
                predictionsOnJoin
            )

        var stopIds = mutableStateOf(listOf("place-a"))
        var predictions: PredictionsStreamDataResponse? =
            PredictionsStreamDataResponse(ObjectCollectionBuilder())

        composeTestRule.setContent {
            var stopIds by remember { stopIds }
            predictions = subscribeToPredictions(stopIds, predictionsRepo)
        }

        composeTestRule.waitUntil { connectProps == listOf("place-a") }

        composeTestRule.waitUntil {
            predictions != null &&
                predictions == predictionsOnJoin?.toPredictionsStreamDataResponse()
        }

        assertEquals(0, disconnectCount)

        stopIds.value = listOf("place-b")
        composeTestRule.waitUntil { disconnectCount == 1 }

        composeTestRule.waitUntil { connectProps == listOf("place-b") }
    }

    @Test
    fun testDisconnectsOnPause() = runTest {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        var connectCount = 0
        var disconnectCount = 0

        val predictionsRepo =
            MockPredictionsRepository(
                {},
                { stopIds -> connectCount += 1 },
                { disconnectCount += 1 },
                null,
                null
            )

        var stopIds = mutableStateOf(listOf("place-a"))
        var predictions: PredictionsStreamDataResponse? =
            PredictionsStreamDataResponse(ObjectCollectionBuilder())

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                var stopIds by remember { stopIds }
                predictions = subscribeToPredictions(stopIds, predictionsRepo)
            }
        }

        composeTestRule.waitUntil { connectCount == 1 }
        assertEquals(0, disconnectCount)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }

        composeTestRule.waitUntil { disconnectCount == 1 }
        assertEquals(1, connectCount)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }

        composeTestRule.waitUntil { connectCount == 2 }
        assertEquals(1, disconnectCount)
    }
}
