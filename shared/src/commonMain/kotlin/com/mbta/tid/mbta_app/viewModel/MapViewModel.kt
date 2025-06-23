package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.map.ColorPalette
import com.mbta.tid.mbta_app.map.RouteFeaturesBuilder
import com.mbta.tid.mbta_app.model.GlobalMapData
import com.mbta.tid.mbta_app.model.SheetRoutes
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import com.mbta.tid.mbta_app.utils.timer
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class MapViewModel(
    private val globalRepository: IGlobalRepository,
    private val railRouteShapeRepository: IRailRouteShapeRepository,
) : MoleculeViewModel<MapViewModel.Event, MapViewModel.State>() {

    sealed interface Event {

        data class SelectedStop(val stop: Stop?, val stopFilter: StopDetailsFilter?) : Event

        data class SelectedVehicle(val selectedVehicle: Vehicle?) : Event

        data class NavChanged(val currentNavEntry: SheetRoutes?) : Event

        data object Recenter : Event

        data object Panned : Event

        data class AlertsChanged(val alerts: AlertsStreamDataResponse?) : Event
    }

    sealed class State {
        data class Unfiltered(val following: Boolean) : State()

        data class StopSelected(
            val centered: Boolean,
            val stop: Stop?,
            val stopFilter: StopDetailsFilter?,
        ) : State()

        data class VehicleSelected(val centered: Boolean, val selectedVehicle: Vehicle?) : State()
    }

    @Composable
    override fun runLogic(events: Flow<Event>): State {
        val now by timer(updateInterval = 300.seconds)
        val globalData by globalRepository.state.collectAsState()
        var globalMapData by remember { mutableStateOf<GlobalMapData?>(null) }
        var railRouteShapes by remember { mutableStateOf<MapFriendlyRouteResponse?>(null) }
        var alerts by remember { mutableStateOf<AlertsStreamDataResponse?>(null) }
        var currentNavEntry by remember { mutableStateOf<SheetRoutes?>(null) }
        var state by remember { mutableStateOf<State>(State.Unfiltered(true)) }
        LaunchedEffect(null) { globalRepository.getGlobalData() }
        LaunchedEffect(null) { railRouteShapes = fetchRailRouteShapes() }
        LaunchedEffect(now) { globalMapData = globalMapData(now, globalData, alerts) }
        LaunchedEffect(null) {
            events.collect { event ->
                when (event) {
                    is Event.AlertsChanged -> alerts = event.alerts
                    is Event.NavChanged -> {
                        state = handleNavChange(event.currentNavEntry, currentNavEntry, globalData)
                        currentNavEntry = event.currentNavEntry
                    }
                    Event.Panned ->
                        state =
                            when (state) {
                                is State.StopSelected ->
                                    (state as State.StopSelected).copy(centered = false)
                                is State.Unfiltered ->
                                    (state as State.Unfiltered).copy(following = false)
                                is State.VehicleSelected ->
                                    (state as State.VehicleSelected).copy(centered = false)
                            }
                    Event.Recenter ->
                        state =
                            when (state) {
                                is State.StopSelected ->
                                    (state as State.StopSelected).copy(centered = true)
                                is State.Unfiltered ->
                                    (state as State.Unfiltered).copy(following = true)
                                is State.VehicleSelected ->
                                    (state as State.VehicleSelected).copy(centered = true)
                            }
                    is Event.SelectedStop ->
                        state = State.StopSelected(true, event.stop, event.stopFilter)
                    is Event.SelectedVehicle ->
                        state = State.VehicleSelected(true, event.selectedVehicle)
                }
            }
        }
        return state
    }

    val models
        get() = internalModels

    fun selectedStop(stop: Stop?, stopFilter: StopDetailsFilter?) =
        fireEvent(Event.SelectedStop(stop, stopFilter))

    fun selectedVehicle(selectedVehicle: Vehicle?) =
        fireEvent(Event.SelectedVehicle(selectedVehicle))

    fun navChanged(currentNavEntry: SheetRoutes?) = fireEvent(Event.NavChanged(currentNavEntry))

    fun recenter() = fireEvent(Event.Recenter)

    private suspend fun fetchRailRouteShapes(): MapFriendlyRouteResponse? {
        return when (val data = railRouteShapeRepository.getRailRouteShapes()) {
            is ApiResult.Ok -> data.data
            is ApiResult.Error -> null
        }
    }

    suspend fun handleNavChange(
        currentNavEntry: SheetRoutes?,
        previousNavEntry: SheetRoutes?,
        globalResponse: GlobalResponse?,
    ): State {
        handleNearbyNavRestoration(currentNavEntry, previousNavEntry)
        val stopDetails =
            when (currentNavEntry) {
                is SheetRoutes.StopDetails -> currentNavEntry
                else -> null
            }
        if (stopDetails == null) {
            return State.Unfiltered(true)
        }
        val stop = globalResponse?.getStop(stopDetails.stopId)
        return State.StopSelected(true, stop, stopDetails.stopFilter)
    }

    // Can't really do this because viewportProvider is too reliant on compose & Mapbox
    // This also doesn't exactly reflect a state, it's saving a state
    suspend fun handleNearbyNavRestoration(
        currentNavEntry: SheetRoutes?,
        previousNavEntry: SheetRoutes?,
    ) {
        //        if (
        //            previousNavEntry is SheetRoutes.NearbyTransit &&
        //            currentNavEntry is SheetRoutes.StopDetails
        //        ) {
        //            viewportProvider.saveNearbyTransitViewport()
        //        } else if (
        //            previousNavEntry is SheetRoutes.StopDetails &&
        //            currentNavEntry is SheetRoutes.NearbyTransit
        //        ) {
        //            refreshRouteLineSource()
        //            viewportProvider.restoreNearbyTransitViewport()
        //        }
    }

    suspend fun updateDisplayedRoutesBasedOnStop(
        globalResponse: GlobalResponse?,
        railRouteShapes: MapFriendlyRouteResponse?,
        stopMapData: StopMapResponse?,
        stopFilter: StopDetailsFilter?,
    ) {
        val globalResponse = globalResponse ?: return
        val railRouteShapes = railRouteShapes ?: return
        val stopMapData = stopMapData ?: return

        val stopFilter = stopFilter
        val filteredRoutes =
            if (stopFilter != null) {
                RouteFeaturesBuilder.filteredRouteShapesForStop(
                    stopMapData,
                    stopFilter,
                    routeCardData,
                )
            } else {
                RouteFeaturesBuilder.forRailAtStop(
                    stopMapData.routeShapes,
                    railRouteShapes.routesWithSegmentedShapes,
                    globalResponse,
                )
            }
        val newRailData =
            RouteFeaturesBuilder.generateRouteSources(filteredRoutes, globalResponse, globalMapData)
        // instead of doing this return necessary data and handle it in front end
        layerManager.run {
            updateRouteSourceData(newRailData)
            addLayers(
                filteredRoutes,
                stopLayerGeneratorState,
                globalResponse,
                if (isDarkMode) ColorPalette.dark else ColorPalette.light,
            )
        }
    }

    suspend fun globalMapData(
        now: Instant,
        globalResponse: GlobalResponse?,
        alertsData: AlertsStreamDataResponse?,
    ): GlobalMapData? =
        withContext(Dispatchers.Default) {
            globalResponse?.let { GlobalMapData(it, alertsData, now) }
        }
}
