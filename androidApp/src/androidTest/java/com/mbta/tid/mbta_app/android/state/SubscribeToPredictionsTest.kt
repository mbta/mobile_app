package com.mbta.tid.mbta_app.android.state

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockPredictionsRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.ErrorBannerViewModel
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SubscribeToPredictionsTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun testPredictions() {
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
                predictionsOnJoin,
            )

        var stopIds = mutableStateOf(listOf("place-a"))
        var predictions: PredictionsStreamDataResponse? =
            PredictionsStreamDataResponse(ObjectCollectionBuilder())
        val errorBannerViewModel =
            ErrorBannerViewModel(
                MockErrorBannerStateRepository(),
                MockSentryRepository(),
                Clock.System,
            )

        composeTestRule.setContent {
            var stopIds by remember { stopIds }
            val predictionsVM =
                subscribeToPredictions(stopIds, predictionsRepo, errorBannerViewModel)
            predictions = predictionsVM.predictionsFlow.collectAsState(initial = null).value
        }

        composeTestRule.waitUntilDefaultTimeout { connectProps == listOf("place-a") }

        composeTestRule.waitUntilDefaultTimeout {
            predictions != null &&
                predictions == predictionsOnJoin.toPredictionsStreamDataResponse()
        }

        assertEquals(0, disconnectCount)

        stopIds.value = listOf("place-b")
        composeTestRule.waitUntilDefaultTimeout { disconnectCount == 1 }

        composeTestRule.waitUntilDefaultTimeout { connectProps == listOf("place-b") }
    }

    @Test
    fun testDisconnectsOnPause() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        var connectCount = 0
        var disconnectCount = 0

        val predictionsRepo =
            MockPredictionsRepository(
                {},
                { stopIds -> connectCount += 1 },
                { disconnectCount += 1 },
                null,
                null,
            )

        var stopIds = mutableStateOf(listOf("place-a"))
        var predictions: PredictionsStreamDataResponse? =
            PredictionsStreamDataResponse(ObjectCollectionBuilder())
        val errorBannerViewModel =
            ErrorBannerViewModel(
                MockErrorBannerStateRepository(),
                MockSentryRepository(),
                Clock.System,
            )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                var stopIds by remember { stopIds }
                val predictionsVM =
                    subscribeToPredictions(stopIds, predictionsRepo, errorBannerViewModel)
                predictions = predictionsVM.predictionsFlow.collectAsState(initial = null).value
            }
        }

        composeTestRule.waitUntilDefaultTimeout { connectCount == 1 }
        assertEquals(0, disconnectCount)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }

        composeTestRule.waitUntilDefaultTimeout { disconnectCount == 1 }
        assertEquals(1, connectCount)

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }

        composeTestRule.waitUntilDefaultTimeout { connectCount == 2 }
        assertEquals(1, disconnectCount)
    }

    @Test
    fun testEmptyStopList() {
        var connected = false
        val predictionsRepo =
            MockPredictionsRepository(
                onConnectV2 = { stopIds ->
                    assertEquals(emptyList<String>(), stopIds)
                    connected = true
                },
                connectV2Response =
                    PredictionsByStopJoinResponse(emptyMap(), emptyMap(), emptyMap()),
            )

        var predictions: PredictionsStreamDataResponse? = null
        val errorBannerViewModel =
            ErrorBannerViewModel(
                MockErrorBannerStateRepository(),
                MockSentryRepository(),
                Clock.System,
            )

        composeTestRule.setContent {
            val predictionsVM =
                subscribeToPredictions(emptyList(), predictionsRepo, errorBannerViewModel)
            predictions = predictionsVM.predictionsFlow.collectAsState(initial = null).value
        }

        composeTestRule.waitUntilDefaultTimeout { predictions != null }
        assertNotNull(predictions)
        assertTrue(connected)
    }

    @Test
    fun testCheckPredictionsStaleCalled() {
        val objects = ObjectCollectionBuilder()
        objects.prediction()
        val predictionsOnJoin = PredictionsByStopJoinResponse(objects)
        val predictionsRepo = MockPredictionsRepository({}, {}, {}, null, predictionsOnJoin)

        predictionsRepo.lastUpdated = EasternTimeInstant.now()

        var stopIds = mutableStateOf(listOf("place-a"))

        var checkPredictionsStaleCount = 0
        val mockErrorRepo =
            MockErrorBannerStateRepository(
                onCheckPredictionsStale = { checkPredictionsStaleCount += 1 }
            )
        val errorBannerViewModel =
            ErrorBannerViewModel(mockErrorRepo, MockSentryRepository(), Clock.System)

        composeTestRule.setContent {
            var stopIds by remember { stopIds }
            subscribeToPredictions(stopIds, predictionsRepo, errorBannerViewModel, 1.seconds)
        }

        composeTestRule.waitUntilDefaultTimeout { checkPredictionsStaleCount >= 2 }
    }
}
