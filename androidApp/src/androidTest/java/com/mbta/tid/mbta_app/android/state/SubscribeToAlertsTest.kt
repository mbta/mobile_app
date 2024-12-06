package com.mbta.tid.mbta_app.android.state

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.mbta.tid.mbta_app.android.util.TimerViewModel
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class MockAlertsRepository(private val scope: CoroutineScope) : IAlertsRepository {
    lateinit var alertsStreamDataResponse: AlertsStreamDataResponse
    var disconnectHook: () -> Unit = { println("original disconnect hook called") }

    override fun connect(onReceive: (ApiResult<AlertsStreamDataResponse>) -> Unit) {
        scope.launch { onReceive(ApiResult.Ok(alertsStreamDataResponse)) }
    }

    override fun disconnect() {
        disconnectHook()
    }
}

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
        val alertsRepo = MockAlertsRepository(this.backgroundScope)
        alertsRepo.alertsStreamDataResponse = alertsStreamDataResponse

        var actualData: AlertsStreamDataResponse? = null
        composeRule.setContent { actualData = subscribeToAlerts(alertsRepo) }
        composeRule.awaitIdle()
        assertEquals(alertsStreamDataResponse, actualData)
    }

    @Test
    fun testAlertsOnClear() = runTest {
        var disconnectCalled = false
        val mockAlertsRepository = MockAlertsRepository(this.backgroundScope)
        mockAlertsRepository.disconnectHook = { disconnectCalled = true }
        val viewModelStore = ViewModelStore()
        val viewModelProvider =
            ViewModelProvider(
                viewModelStore,
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AlertsViewModel(mockAlertsRepository, TimerViewModel(1.seconds)) as T
                    }
                }
            )
        viewModelProvider.get(AlertsViewModel::class)
        viewModelStore.clear()
        Assert.assertEquals(true, disconnectCalled)
    }
}
