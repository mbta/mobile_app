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
import com.mbta.tid.mbta_app.usecases.AlertsUsecase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class AlertsViewModel(private val alertsUsecase: AlertsUsecase) : ViewModel() {
    private val _alerts = MutableStateFlow<AlertsStreamDataResponse?>(null)
    val alertFlow: StateFlow<AlertsStreamDataResponse?> = _alerts

    fun connect() {
        alertsUsecase.connect {
            when (it) {
                is ApiResult.Ok -> {
                    _alerts.value = it.data
                }
                is ApiResult.Error -> {
                    Log.e("AlertsViewModel", "subscribeToAlerts failed: $it")
                }
            }
        }
    }

    fun disconnect() {
        alertsUsecase.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }

    class Factory(private val alertsUsecase: AlertsUsecase) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AlertsViewModel(alertsUsecase) as T
        }
    }
}

@Composable
fun subscribeToAlerts(alertsUsecase: AlertsUsecase = koinInject()): AlertsStreamDataResponse? {
    val viewModel: AlertsViewModel = viewModel(factory = AlertsViewModel.Factory(alertsUsecase))

    LifecycleResumeEffect(key1 = null) {
        CoroutineScope(Dispatchers.IO).launch { viewModel.connect() }

        onPauseOrDispose { viewModel.disconnect() }
    }

    return viewModel.alertFlow.collectAsState(initial = null).value
}
