package com.mbta.tid.mbta_app.android.state

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class RailRouteShapesViewModel(private val railRouteShapeRepository: IRailRouteShapeRepository) :
    ViewModel() {
    private val _railRouteShapes: MutableStateFlow<MapFriendlyRouteResponse?> =
        MutableStateFlow(null)
    val railRouteShapes: StateFlow<MapFriendlyRouteResponse?> = _railRouteShapes

    init {
        CoroutineScope(Dispatchers.IO).launch { railRouteShapes.collect { getRailRouteShapes() } }
    }

    suspend fun getRailRouteShapes() {
        when (val data = railRouteShapeRepository.getRailRouteShapes()) {
            is ApiResult.Ok -> _railRouteShapes.value = data.data
            is ApiResult.Error ->
                Log.e("RailRouteShapesViewModel", "getRailRouteShapes failed: $data")
        }
    }
}

@Composable
fun getRailRouteShapes(
    railRouteShapeRepository: IRailRouteShapeRepository = koinInject()
): MapFriendlyRouteResponse? {
    val viewModel = remember { RailRouteShapesViewModel(railRouteShapeRepository) }
    return viewModel.railRouteShapes.collectAsState(initial = null).value
}
