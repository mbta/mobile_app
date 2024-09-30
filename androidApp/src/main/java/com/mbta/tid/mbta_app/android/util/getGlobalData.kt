package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun getGlobalData(globalRepository: IGlobalRepository = koinInject()): GlobalResponse? {
    var globalResponse: GlobalResponse? by remember { mutableStateOf(null) }

    LaunchedEffect(null) {
        withContext(Dispatchers.IO) {
            when (val data = globalRepository.getGlobalData()) {
                is ApiResult.Ok -> globalResponse = data.data
                is ApiResult.Error -> TODO("handle errors")
            }
        }
    }

    return globalResponse
}
