package com.mbta.tid.mbta_app.android.state

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.internal.notifyAll
import org.koin.compose.koinInject

class AlertsViewModel(
    private val alertsRepository: IAlertsRepository,
) : ViewModel() {
    private val _alerts = MutableStateFlow<AlertsStreamDataResponse?>(null)
    val alertFlow: StateFlow<AlertsStreamDataResponse?> = _alerts

    fun connect() {
        alertsRepository.connect {
            when (it) {
                is ApiResult.Ok -> {
                    val oldAlerts = _alerts.value?.alerts ?: emptyMap()

                    _alerts.value = it.data
                    if (oldAlerts.isEmpty() && it.data.alerts.isNotEmpty())
                        synchronized(alertFlow) { alertFlow.notifyAll() }
                }
                is ApiResult.Error -> {
                    Log.e("AlertsViewModel", "subscribeToAlerts failed: $it")
                }
            }
        }
    }

    fun disconnect() {
        alertsRepository.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }

    class Factory(private val alertsRepository: IAlertsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AlertsViewModel(alertsRepository) as T
        }
    }
}

@Composable
fun subscribeToAlerts(
    alertsRepository: IAlertsRepository = koinInject()
): AlertsStreamDataResponse? {
    val viewModel: AlertsViewModel = viewModel(factory = AlertsViewModel.Factory(alertsRepository))

    LifecycleResumeEffect(key1 = null) {
        CoroutineScope(Dispatchers.IO).launch { viewModel.connect() }

        onPauseOrDispose { viewModel.disconnect() }
    }

    return viewModel.alertFlow.collectAsState(initial = null).value
}
