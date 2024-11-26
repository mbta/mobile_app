package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbta.tid.mbta_app.model.Coordinate
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class NearbyViewModel(
    private val nearbyRepository: INearbyRepository,
    private val globalResponse: GlobalResponse?,
    private val location: Coordinate,
    setLastLocation: (Position) -> Unit
) : ViewModel() {
    private val _nearbyResponse = MutableStateFlow<NearbyStaticData?>(null)
    val nearbyResponse: StateFlow<NearbyStaticData?> = _nearbyResponse
    private var job: Job? = null

    init {
        if (globalResponse != null) {
            getNearby(globalResponse)
            setLastLocation(Position(latitude = location.latitude, longitude = location.longitude))
        }
    }

    fun getNearby(globalResponse: GlobalResponse) {
        job?.cancel()
        job =
            viewModelScope.launch {
                delay(500)
                when (val data = nearbyRepository.getNearby(globalResponse, location)) {
                    is ApiResult.Ok -> _nearbyResponse.emit(data.data)
                    is ApiResult.Error -> TODO("handle errors")
                }
            }
    }
}

@Composable
fun getNearby(
    globalResponse: GlobalResponse?,
    location: Coordinate,
    setLastLocation: (Position) -> Unit,
    nearbyRepository: INearbyRepository = koinInject()
): NearbyStaticData? {
    val viewModel =
        remember(globalResponse, location) {
            NearbyViewModel(nearbyRepository, globalResponse, location, setLastLocation)
        }
    return viewModel.nearbyResponse.collectAsState(initial = null).value
}
