package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
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

    fun getGlobalData(errorKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            fetchApi(
                errorBannerRepo = errorBannerRepository,
                errorKey = errorKey,
                getData = { globalRepository.getGlobalData() },
                onSuccess = { _globalResponse.emit(it) },
                onRefreshAfterError = { getGlobalData(errorKey) }
            )
        }
    }

    class Factory(
        private val globalRepository: IGlobalRepository,
        private val errorBannerRepository: IErrorBannerStateRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalDataViewModel(globalRepository, errorBannerRepository) as T
        }
    }
}

@Composable
fun getGlobalData(
    errorKey: String,
    globalRepository: IGlobalRepository = koinInject(),
    errorBannerRepository: IErrorBannerStateRepository = koinInject()
): GlobalResponse? {
    val viewModel: GlobalDataViewModel =
        viewModel(factory = GlobalDataViewModel.Factory(globalRepository, errorBannerRepository))

    LaunchedEffect(key1 = null) { viewModel.getGlobalData(errorKey) }
    return viewModel.globalResponse.collectAsState(initial = null).value
}
