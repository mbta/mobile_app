package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.android.util.fetchApi
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class GlobalDataViewModel(
    private val globalRepository: IGlobalRepository,
    private val errorBannerRepository: IErrorBannerStateRepository
) : ViewModel() {
    private val _globalResponse = MutableStateFlow<GlobalResponse?>(null)
    var globalResponse: StateFlow<GlobalResponse?> = _globalResponse

    init {
        CoroutineScope(Dispatchers.IO).launch { globalResponse.collect { getGlobalData() } }
    }

    fun getGlobalData() {
        CoroutineScope(Dispatchers.IO).launch {
            fetchApi(
                errorBannerRepo = errorBannerRepository,
                errorKey = "GlobalDataViewModel.getGlobalData",
                getData = { globalRepository.getGlobalData() },
                onSuccess = { _globalResponse.emit(it) },
                onRefreshAfterError = { getGlobalData() }
            )
        }
    }
}

@Composable
fun getGlobalData(
    globalRepository: IGlobalRepository = koinInject(),
    errorBannerRepository: IErrorBannerStateRepository = koinInject()
): GlobalResponse? {
    val viewModel = remember { GlobalDataViewModel(globalRepository, errorBannerRepository) }
    return viewModel.globalResponse.collectAsState(initial = null).value
}
