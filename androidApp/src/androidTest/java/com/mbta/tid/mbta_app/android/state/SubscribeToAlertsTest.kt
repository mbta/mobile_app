package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.MockAlertsRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import com.mbta.tid.mbta_app.usecases.AlertsUsecase
import kotlin.test.assertEquals
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class SubscribeToAlertsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun testAlerts() {
        val builder = ObjectCollectionBuilder()
        builder.alert {
            id = "1"
            header = "Alert 1"
            description = "Description 1"
        }

        var connectCount = 0
        val alertsStreamDataResponse = AlertsStreamDataResponse(builder)
        val alertsRepo = MockAlertsRepository(alertsStreamDataResponse, { connectCount += 1 })
        val alertsUsecase = AlertsUsecase(alertsRepo, MockGlobalRepository())

        var actualData: AlertsStreamDataResponse? = null
        composeRule.setContent { actualData = subscribeToAlerts(alertsUsecase) }
        composeRule.waitUntilDefaultTimeout { connectCount == 1 }
        composeRule.waitUntilDefaultTimeout { alertsStreamDataResponse == actualData }
        assertEquals(alertsStreamDataResponse, actualData)
    }

    @Test
    fun testDisconnectsOnPause() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        var connectCount = 0
        var disconnectCount = 0

        val builder = ObjectCollectionBuilder()
        builder.alert {
            id = "1"
            header = "Alert 1"
            description = "Description 1"
        }

        val alertsStreamDataResponse = AlertsStreamDataResponse(builder)
        val alertsRepo =
            MockAlertsRepository(
                alertsStreamDataResponse,
                { connectCount += 1 },
                { disconnectCount += 1 },
            )
        val alertsUsecase = AlertsUsecase(alertsRepo, MockGlobalRepository())

        var actualData: AlertsStreamDataResponse? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                actualData = subscribeToAlerts(alertsUsecase)
            }
        }

        composeRule.waitUntilDefaultTimeout { connectCount == 1 }
        Assert.assertEquals(0, disconnectCount)

        composeRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }

        composeRule.waitUntilDefaultTimeout { disconnectCount == 1 }
        Assert.assertEquals(1, connectCount)

        composeRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }

        composeRule.waitUntilDefaultTimeout { connectCount == 2 }
        Assert.assertEquals(1, disconnectCount)
    }
}
