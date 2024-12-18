package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.model.Coordinate
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class NearbyViewModel(
    private val nearbyRepository: INearbyRepository,
    private val globalResponse: GlobalResponse?,
    private val location: Coordinate?,
    setLastLocation: (Position) -> Unit,
    setSelectingLocation: (Boolean) -> Unit,
) : ViewModel() {
    private val _nearbyResponse = MutableStateFlow<NearbyStaticData?>(null)
    val nearbyResponse: StateFlow<NearbyStaticData?> = _nearbyResponse
    private var job: Job? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            nearbyResponse.collect {
                if (globalResponse != null && location != null) {
                    getNearby(globalResponse, location)
                    setLastLocation(
                        Position(latitude = location.latitude, longitude = location.longitude)
                    )
                    setSelectingLocation(false)
                }
            }
        }
    }

    suspend fun getNearby(globalResponse: GlobalResponse, location: Coordinate) {
        when (val data = nearbyRepository.getNearby(globalResponse, location)) {
            is ApiResult.Ok -> _nearbyResponse.emit(data.data)
            is ApiResult.Error -> TODO("handle errors")
        }
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }
}

@Composable
fun getNearby(
    globalResponse: GlobalResponse?,
    location: Coordinate?,
    setLastLocation: (Position) -> Unit,
    setSelectingLocation: (Boolean) -> Unit,
    nearbyRepository: INearbyRepository = koinInject()
): NearbyStaticData? {
    val viewModel =
        remember(globalResponse, location) {
            NearbyViewModel(
                nearbyRepository,
                globalResponse,
                location,
                setLastLocation,
                setSelectingLocation
            )
        }
    return viewModel.nearbyResponse.collectAsState(initial = null).value
}
