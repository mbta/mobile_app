package com.mbta.tid.mbta_app.android.state

import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SubscribeToAlertsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun testAlerts() = runTest {
        val builder = ObjectCollectionBuilder()
        builder.alert {
            id = "1"
            header = "Alert 1"
            description = "Description 1"
        }
        val alertsStreamDataResponse = AlertsStreamDataResponse(builder)

        val alertsRepo =
            object : IAlertsRepository {
                override fun connect(onReceive: (ApiResult<AlertsStreamDataResponse>) -> Unit) {
                    launch { onReceive(ApiResult.Ok(alertsStreamDataResponse)) }
                }

                override fun disconnect() {
                    { /* null-op */}
                }
            }

        var actualData: AlertsStreamDataResponse? = null
        composeRule.setContent { actualData = subscribeToAlerts(alertsRepo) }

        composeRule.awaitIdle()
        assertEquals(alertsStreamDataResponse, actualData)
    }
}
