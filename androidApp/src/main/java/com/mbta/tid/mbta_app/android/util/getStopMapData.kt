package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import com.mbta.tid.mbta_app.repositories.IStopRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun getStopMapData(
    stopRepository: IStopRepository = koinInject(),
    stopId: String
): StopMapResponse? {
    var stopMapResponse: StopMapResponse? by remember { mutableStateOf(null) }
    LaunchedEffect(stopId) {
        withContext(Dispatchers.IO) {
            when (val data = stopRepository.getStopMapData(stopId)) {
                is ApiResult.Ok -> stopMapResponse = data.data
                is ApiResult.Error -> TODO("handle errors")
            }
        }
    }
    return stopMapResponse
}
