package com.mbta.tid.mbta_app.viewModel.composeStateHelpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private fun fetchGlobalData(
    errorKey: String,
    errorBannerRepository: IErrorBannerStateRepository,
    globalRepository: IGlobalRepository,
    coroutineDispatcher: CoroutineDispatcher,
) {
    CoroutineScope(coroutineDispatcher).launch {
        fetchApi(
            errorBannerRepo = errorBannerRepository,
            errorKey = errorKey,
            getData = { globalRepository.getGlobalData() },
            // Ignore success response because getGlobalData consumes directly from the repo
            onSuccess = {},
            onRefreshAfterError = {
                fetchGlobalData(
                    errorKey,
                    errorBannerRepository,
                    globalRepository,
                    coroutineDispatcher,
                )
            },
        )
    }
}

@Composable
internal fun getGlobalData(
    errorKey: String,
    globalRepository: IGlobalRepository = koinInject(),
    errorBannerRepository: IErrorBannerStateRepository = koinInject(),
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
): GlobalResponse? {
    val globalResponse: GlobalResponse? by globalRepository.state.collectAsState()
    LaunchedEffect(Unit) {
        fetchGlobalData(
            "$errorKey.getGlobalData",
            errorBannerRepository,
            globalRepository,
            coroutineDispatcher,
        )
    }
    return globalResponse
}
