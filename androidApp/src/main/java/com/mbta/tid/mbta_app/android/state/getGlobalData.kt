package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class GlobalDataViewModel(private val globalRepository: IGlobalRepository) : ViewModel() {
    private val _globalResponse = MutableStateFlow<GlobalResponse?>(null)
    var globalResponse: StateFlow<GlobalResponse?> = _globalResponse

    init {
        CoroutineScope(Dispatchers.IO).launch { globalResponse.collect { getGlobalData() } }
    }

    suspend fun getGlobalData() {
        when (val data = globalRepository.getGlobalData()) {
            is ApiResult.Ok -> _globalResponse.emit(data.data)
            is ApiResult.Error -> TODO("handle errors")
        }
    }
}

@Composable
fun getGlobalData(globalRepository: IGlobalRepository = koinInject()): GlobalResponse? {
    val viewModel = remember { GlobalDataViewModel(globalRepository) }
    return viewModel.globalResponse.collectAsState(initial = null).value
}
