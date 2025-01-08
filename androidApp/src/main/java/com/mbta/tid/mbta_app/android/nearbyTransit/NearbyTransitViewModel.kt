package com.mbta.tid.mbta_app.android.nearbyTransit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.android.util.fetchApi
import com.mbta.tid.mbta_app.android.util.isRoughlyEqualTo
import com.mbta.tid.mbta_app.model.NearbyStaticData
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NearbyTransitViewModel(val nearbyRepository: INearbyRepository) : KoinComponent, ViewModel() {
    var loadedLocation by mutableStateOf<Position?>(null)
    var loading by mutableStateOf(false)
    var nearby by mutableStateOf<NearbyStaticData?>(null)

    private var fetchNearbyTask: Job? = null

    private val errorBannerRepository: IErrorBannerStateRepository by inject()

    fun getNearby(
        globalResponse: GlobalResponse,
        location: Position,
        setLastLocation: (Position) -> Unit,
        setSelectingLocation: (Boolean) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (loadedLocation?.isRoughlyEqualTo(location) == true) {
                return@launch
            }
            if (loading) {
                fetchNearbyTask?.cancel()
            }
            fetchNearbyTask = launch {
                loading = true
                fetchApi(
                    errorBannerRepo = errorBannerRepository,
                    errorKey = "NearbyViewModel.getNearby",
                    getData = { nearbyRepository.getNearby(globalResponse, location) },
                    onSuccess = { nearby = it },
                    onRefreshAfterError = {
                        getNearby(globalResponse, location, setLastLocation, setSelectingLocation)
                    }
                )
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
