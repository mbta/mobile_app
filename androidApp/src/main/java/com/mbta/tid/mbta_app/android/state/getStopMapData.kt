package com.mbta.tid.mbta_app.android.state

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import com.mbta.tid.mbta_app.repositories.IStopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject

class StopMapViewModel(private val stopRepository: IStopRepository) : ViewModel() {
    private val _stopMapResponse = MutableStateFlow<StopMapResponse?>(null)
    val stopMapResponse: StateFlow<StopMapResponse?> = _stopMapResponse

    suspend fun getStopMapData(stopId: String) {
        Log.i("KB", "get stop map data ${stopId}")
        when (val data = stopRepository.getStopMapData(stopId)) {
            is ApiResult.Ok -> _stopMapResponse.emit(data.data)
            is ApiResult.Error -> TODO("handle errors")
        }
    }

    class Factory(private val stopRepository: IStopRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StopMapViewModel(stopRepository) as T
        }
    }
}

@Composable
fun getStopMapData(
    stopId: String,
    stopRepository: IStopRepository = koinInject(),
): StopMapResponse? {
    val viewModel: StopMapViewModel = viewModel(factory = StopMapViewModel.Factory(stopRepository))

    LaunchedEffect(key1 = stopId) { viewModel.getStopMapData(stopId) }

    return viewModel.stopMapResponse?.collectAsState(initial = null)?.value
}
