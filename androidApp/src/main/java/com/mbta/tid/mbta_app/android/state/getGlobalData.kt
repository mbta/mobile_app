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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class GlobalDataViewModel(
    private val globalRepository: IGlobalRepository,
    private val errorBannerRepository: IErrorBannerStateRepository,
) : ViewModel() {
    fun getGlobalData(errorKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            fetchApi(
                errorBannerRepo = errorBannerRepository,
                errorKey = errorKey,
                getData = { globalRepository.getGlobalData() },
                // Response is consumed directly from the repository to avoid null states
                onSuccess = {},
                onRefreshAfterError = { getGlobalData(errorKey) },
            )
        }
    }

    class Factory(
        private val globalRepository: IGlobalRepository,
        private val errorBannerRepository: IErrorBannerStateRepository,
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
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
): GlobalResponse? {
    val viewModel: GlobalDataViewModel =
        viewModel(factory = GlobalDataViewModel.Factory(globalRepository, errorBannerRepository))

    LaunchedEffect(Unit) { viewModel.getGlobalData(errorKey) }

    return globalRepository.state.collectAsState().value
}
