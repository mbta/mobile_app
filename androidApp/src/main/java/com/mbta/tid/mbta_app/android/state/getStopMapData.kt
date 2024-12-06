package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import com.mbta.tid.mbta_app.repositories.IStopRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class StopMapViewModel(private val stopRepository: IStopRepository, stopId: String) : ViewModel() {
    private val _stopMapResponse = MutableStateFlow<StopMapResponse?>(null)
    val stopMapResponse: StateFlow<StopMapResponse?> = _stopMapResponse

    init {
        CoroutineScope(Dispatchers.IO).launch { stopMapResponse.collect { getStopMapData(stopId) } }
    }

    suspend fun getStopMapData(stopId: String) {
        when (val data = stopRepository.getStopMapData(stopId)) {
            is ApiResult.Ok -> _stopMapResponse.emit(data.data)
            is ApiResult.Error -> TODO("handle errors")
        }
    }
}

@Composable
fun getStopMapData(
    stopId: String,
    stopRepository: IStopRepository = koinInject(),
): StopMapResponse? {
    val viewModel: StopMapViewModel? = remember(stopId) { StopMapViewModel(stopRepository, stopId) }

    return viewModel?.stopMapResponse?.collectAsState(initial = null)?.value
}
