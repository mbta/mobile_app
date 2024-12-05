package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import com.mbta.tid.mbta_app.android.util.TimerViewModel
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.internal.notifyAll
import org.koin.compose.koinInject

class AlertsViewModel(
    private val alertsRepository: IAlertsRepository,
    private val timerViewModel: TimerViewModel
) : ViewModel() {
    private val _alerts = MutableLiveData(AlertsStreamDataResponse(emptyMap()))
    val alerts: LiveData<AlertsStreamDataResponse> = _alerts
    val alertFlow = alerts.asFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            alertsRepository.connect {
                when (it) {
                    is ApiResult.Ok -> {
                        _alerts.postValue(it.data)
                        val oldAlerts = alerts.value?.alerts ?: emptyMap()
                        if (oldAlerts.isEmpty() && it.data.alerts.isNotEmpty())
                            synchronized(alerts) { alerts.notifyAll() }
                    }
                    is ApiResult.Error -> {
                        /* TODO("handle errors") */
                    }
                }
            }
            timerViewModel.timerFlow.collect { synchronized(alerts) { alerts.notifyAll() } }
        }
    }

    override fun onCleared() {
        super.onCleared()
        alertsRepository.disconnect()
    }
}

@Composable
fun subscribeToAlerts(
    alertsRepository: IAlertsRepository = koinInject()
): AlertsStreamDataResponse? {
    val timerViewModel = remember { TimerViewModel(1.seconds) }
    val viewModel = remember { AlertsViewModel(alertsRepository, timerViewModel) }
    return viewModel.alertFlow.collectAsState(initial = null).value
}
