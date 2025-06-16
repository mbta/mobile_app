package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private fun fetchGlobalData(
    errorKey: String,
    errorBannerRepository: IErrorBannerStateRepository,
    globalRepository: IGlobalRepository,
    onSuccess: (GlobalResponse) -> Unit,
) {
    CoroutineScope(Dispatchers.IO).launch {
        fetchApi(
            errorBannerRepo = errorBannerRepository,
            errorKey = errorKey,
            getData = { globalRepository.getGlobalData() },
            onSuccess = onSuccess,
            onRefreshAfterError = {
                fetchGlobalData(errorKey, errorBannerRepository, globalRepository, onSuccess)
            },
        )
    }
}

@Composable
fun getGlobalData(
    errorKey: String,
    globalRepository: IGlobalRepository = koinInject(),
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
): GlobalResponse? {
    var globalResponse: GlobalResponse? by remember { mutableStateOf(null) }
    LaunchedEffect(null) {
        fetchGlobalData(errorKey, errorBannerRepository, globalRepository) { globalResponse = it }
    }
    return globalResponse
}
