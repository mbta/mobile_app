package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import okhttp3.internal.notifyAll
import org.koin.compose.koinInject

class AlertsViewModel(
    private val alertsRepository: IAlertsRepository,
) : ViewModel() {
    private val _alerts = MutableLiveData(AlertsStreamDataResponse(emptyMap()))
    val alerts: LiveData<AlertsStreamDataResponse> = _alerts
    val alertFlow = alerts.asFlow()

    fun connect() {
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
        viewModel.connect()

        onPauseOrDispose { viewModel.disconnect() }
    }

    return viewModel.alertFlow.collectAsState(initial = null).value
}
