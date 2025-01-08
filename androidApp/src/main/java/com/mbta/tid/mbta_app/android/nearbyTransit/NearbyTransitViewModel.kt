package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.android.util.isRoughlyEqualTo
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NearbyTransitViewModel(val nearbyRepository: INearbyRepository) : ViewModel() {
    var loadedLocation by mutableStateOf<Position?>(null)
    var loading by mutableStateOf(false)
    var nearby by mutableStateOf<NearbyStaticData?>(null)

    private var fetchNearbyTask: Job? = null

    suspend fun getNearby(
        globalResponse: GlobalResponse,
        location: Position,
        setLastLocation: (Position) -> Unit,
        setSelectingLocation: (Boolean) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            if (loadedLocation?.isRoughlyEqualTo(location) == true) {
                return@withContext
            }
            if (loading) {
                fetchNearbyTask?.cancel()
            }
            fetchNearbyTask = launch {
                loading = true
                when (val data = nearbyRepository.getNearby(globalResponse, location)) {
                    is ApiResult.Ok -> nearby = data.data
                    is ApiResult.Error -> TODO("handle errors")
                }
                loading = false
                setLastLocation(location)
                setSelectingLocation(false)
            }
        }
    }

    fun reset() {
        loadedLocation = null
        loading = false
        nearby = null
    }

    override fun onCleared() {
        super.onCleared()
        fetchNearbyTask?.cancel()
    }
}
