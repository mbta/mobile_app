package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class RailRouteShapesViewModel(private val railRouteShapeRepository: IRailRouteShapeRepository) :
    ViewModel() {
    private val _railRouteShapes: MutableStateFlow<MapFriendlyRouteResponse?> =
        MutableStateFlow(null)
    val railRouteShapes = _railRouteShapes

    init {
        viewModelScope.launch { railRouteShapes.collect { getRailRouteShapes() } }
    }

    suspend fun getRailRouteShapes() {
        when (val data = railRouteShapeRepository.getRailRouteShapes()) {
            is ApiResult.Ok -> _railRouteShapes.value = data.data
            is ApiResult.Error -> TODO("handle errors")
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
