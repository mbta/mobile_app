package com.mbta.tid.mbta_app.android.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

class ErrorBannerViewModel(
    var loadingWhenPredictionsStale: Boolean = false,
    val errorRepository: IErrorBannerStateRepository,
    val settingsRepository: ISettingsRepository
) : ViewModel() {

    private val _errorState = MutableStateFlow<ErrorBannerState?>(null)
    val errorState: StateFlow<ErrorBannerState?> = _errorState.asStateFlow()

    var showDebugMessages: Boolean = false

    suspend fun activate() {
        errorRepository.subscribeToNetworkStatusChanges()
        showDebugMessages = settingsRepository.getSettings()[Settings.DevDebugMode] ?: false

        errorRepository.state.onEach { _errorState.emit(it) }.collect()
    }

    fun clearState() {
        errorRepository.clearState()
    }

    class Factory(
        var loadingWhenPredictionsStale: Boolean = false,
        val errorRepository: IErrorBannerStateRepository,
        val settingsRepository: ISettingsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ErrorBannerViewModel(
                loadingWhenPredictionsStale,
                errorRepository,
                settingsRepository
            )
                as T
        }
    }
}
