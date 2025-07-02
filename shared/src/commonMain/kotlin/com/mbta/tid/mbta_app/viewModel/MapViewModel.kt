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
import com.mbta.tid.mbta_app.map.RouteSourceData
import com.mbta.tid.mbta_app.map.StopFeaturesBuilder
import com.mbta.tid.mbta_app.map.StopLayerGenerator
import com.mbta.tid.mbta_app.model.GlobalMapData
import com.mbta.tid.mbta_app.model.RouteCardData
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
import com.mbta.tid.mbta_app.repositories.IStopRepository
import com.mbta.tid.mbta_app.utils.IMapLayerManager
import com.mbta.tid.mbta_app.utils.ViewportManager
import com.mbta.tid.mbta_app.utils.timer
import com.mbta.tid.mbta_app.viewModel.MapViewModel.Event
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

interface IMapViewModel {

    val models: StateFlow<MapViewModel.State>

    fun selectedStop(stop: Stop, stopFilter: StopDetailsFilter?)

    fun selectedVehicle(vehicle: Vehicle, stop: Stop?, stopFilter: StopDetailsFilter?)

    fun navChanged(currentNavEntry: SheetRoutes?)

    fun recenter()

    fun alertsChanged(alerts: AlertsStreamDataResponse?)

    fun routeCardDataChanged(routeCardData: List<RouteCardData>?)

    fun colorPaletteChanged(isDarkMode: Boolean)

    fun densityChanged(density: Float)

    fun mapStyleLoaded()

    fun layerManagerInitialized(layerManager: IMapLayerManager)

    fun locationPermissionsChanged(hasPermission: Boolean)

    fun setViewportManager(viewportManager: ViewportManager)
}

class MapViewModel(
    private val globalRepository: IGlobalRepository,
    private val railRouteShapeRepository: IRailRouteShapeRepository,
    private val stopRepository: IStopRepository,
) : MoleculeViewModel<Event, MapViewModel.State>(), IMapViewModel {

    private lateinit var viewportManager: ViewportManager

    sealed interface Event {

        data class SelectedStop(val stop: Stop, val stopFilter: StopDetailsFilter?) : Event

        data class SelectedVehicle(
            val vehicle: Vehicle,
            val stop: Stop?,
            val stopFilter: StopDetailsFilter?,
        ) : Event

        data class NavChanged(val currentNavEntry: SheetRoutes?) : Event

        data object Recenter : Event

        data class AlertsChanged(val alerts: AlertsStreamDataResponse?) : Event

        data class ColorPaletteChanged(val isDarkMode: Boolean) : Event

        data class DensityChanged(val density: Float) : Event

        data class RouteCardDataChanged(val data: List<RouteCardData>?) : Event

        data object MapStyleLoaded : Event

        data class LayerManagerInitialized(val layerManager: IMapLayerManager) : Event

        data class LocationPermissionsChanged(val hasPermission: Boolean) : Event
    }

    sealed class State {

        data object Unfiltered : State()

        data class StopSelected(val stop: Stop?, val stopFilter: StopDetailsFilter?) : State()

        data class VehicleSelected(
            val vehicle: Vehicle,
            val stop: Stop?,
            val stopFilter: StopDetailsFilter?,
        ) : State()
    }

    @Composable
    override fun runLogic(events: Flow<Event>): State {
        val now by timer(updateInterval = 300.seconds)
        val globalData by globalRepository.state.collectAsState()
        var globalMapData by remember { mutableStateOf<GlobalMapData?>(null) }
        var railRouteShapes by remember { mutableStateOf<MapFriendlyRouteResponse?>(null) }
        var railRouteSourceData by remember { mutableStateOf<List<RouteSourceData>?>(null) }
        var alerts by remember { mutableStateOf<AlertsStreamDataResponse?>(null) }
        var previousNavEntry by remember { mutableStateOf<SheetRoutes?>(null) }
        var routeCardData by remember { mutableStateOf<List<RouteCardData>?>(null) }
        var isDarkMode by remember { mutableStateOf<Boolean?>(null) }
        var density by remember { mutableStateOf<Float?>(null) }
        var layerManager by remember { mutableStateOf<IMapLayerManager?>(null) }
        var state by remember { mutableStateOf<State>(State.Unfiltered) }
        val (stopId: String?, stopFilter: StopDetailsFilter?) =
            when (state) {
                is State.StopSelected -> {
                    val currentState = (state as State.StopSelected)
                    currentState.stop?.id to currentState.stopFilter
                }
                is State.Unfiltered -> null to null

                is State.VehicleSelected -> {
                    val currentState = (state as State.VehicleSelected)
                    currentState.stop?.id to null
                }
            }
        LaunchedEffect(null) { globalRepository.getGlobalData() }
        LaunchedEffect(null) { railRouteShapes = fetchRailRouteShapes() }
        LaunchedEffect(now, globalData, alerts) {
            globalMapData = globalMapData(now, globalData, alerts)
        }
        LaunchedEffect(railRouteShapes, globalData, globalMapData) {
            railRouteSourceData = routeLineData(globalData, globalMapData, railRouteShapes)
            refreshRouteLineSource(
                globalData,
                railRouteShapes,
                isDarkMode == true,
                railRouteSourceData,
                stopId,
                stopFilter,
                layerManager,
            )
            val stopSourceData = stopSourceData(globalMapData, railRouteSourceData)
            layerManager?.updateStopSourceData(stopSourceData)
        }
        LaunchedEffect(null) {
            events.collect { event ->
                when (event) {
                    is Event.AlertsChanged -> alerts = event.alerts
                    is Event.ColorPaletteChanged -> isDarkMode = event.isDarkMode
                    is Event.DensityChanged -> density = event.density
                    is Event.RouteCardDataChanged -> routeCardData = event.data
                    is Event.NavChanged -> {
                        state =
                            handleNavChange(
                                state,
                                event.currentNavEntry,
                                previousNavEntry,
                                globalData,
                                railRouteShapes,
                                isDarkMode == true,
                                railRouteSourceData,
                                stopId,
                                stopFilter,
                                layerManager,
                            )
                        previousNavEntry = event.currentNavEntry
                    }

                    Event.Recenter -> {
                        when (state) {
                            is State.StopSelected -> {
                                viewportManager.follow(null)
                            }

                            is State.Unfiltered -> {
                                viewportManager.follow(null)
                            }

                            is State.VehicleSelected -> {
                                val currentState = state as State.VehicleSelected
                                density?.let {
                                    viewportManager.vehicleOverview(
                                        vehicle = currentState.vehicle,
                                        stop = currentState.stop,
                                        density = it,
                                    )
                                }
                            }
                        }
                    }
                    is Event.SelectedStop -> {
                        updateDisplayedRoutesBasedOnStop(
                            globalResponse = globalData,
                            railRouteShapes = railRouteShapes,
                            stopId = event.stop.id,
                            stopFilter = event.stopFilter,
                            now = now,
                            alerts = alerts,
                            routeCardData = routeCardData,
                            isDarkMode = isDarkMode,
                            layerManager,
                        )
                        viewportManager.saveNearbyTransitViewport()
                        viewportManager.stopCenter(event.stop)
                        state = State.StopSelected(event.stop, event.stopFilter)
                    }
                    is Event.SelectedVehicle -> {
                        val currentState = (state as? State.VehicleSelected)
                        if (currentState?.vehicle?.id != event.vehicle.id && density != null) {
                            viewportManager.vehicleOverview(
                                vehicle = event.vehicle,
                                stop = event.stop,
                                density = density!!,
                            )
                        }
                        updateDisplayedRoutesBasedOnStop(
                            globalResponse = globalData,
                            railRouteShapes = railRouteShapes,
                            stopId = event.stop?.id,
                            stopFilter = event.stopFilter,
                            now = now,
                            alerts = alerts,
                            routeCardData = routeCardData,
                            isDarkMode = isDarkMode,
                            layerManager,
                        )
                        state = State.VehicleSelected(event.vehicle, event.stop, event.stopFilter)
                    }
                    is Event.MapStyleLoaded -> {
                        val stopLayerGeneratorState = stopLayerGeneratorState(stopId, stopFilter)
                        layerManager?.run {
                            addLayers(
                                railRouteShapes ?: return@run,
                                stopLayerGeneratorState,
                                globalData ?: return@run,
                                if (isDarkMode == true) ColorPalette.dark else ColorPalette.light,
                            )
                        }
                    }
                    is Event.LayerManagerInitialized -> {
                        if (layerManager == null) layerManager = event.layerManager
                    }
                    is Event.LocationPermissionsChanged -> {
                        if (event.hasPermission && viewportManager.isDefault()) {
                            viewportManager.follow(0)
                            layerManager?.run { resetPuckPosition() }
                        }
                    }
                }
            }
        }
        return state
    }

    override val models
        get() = internalModels

    override fun selectedStop(stop: Stop, stopFilter: StopDetailsFilter?) =
        fireEvent(Event.SelectedStop(stop, stopFilter))

    override fun selectedVehicle(vehicle: Vehicle, stop: Stop?, stopFilter: StopDetailsFilter?) =
        fireEvent(Event.SelectedVehicle(vehicle, stop, stopFilter))

    override fun navChanged(currentNavEntry: SheetRoutes?) =
        fireEvent(Event.NavChanged(currentNavEntry))

    override fun recenter() = fireEvent(Event.Recenter)

    override fun alertsChanged(alerts: AlertsStreamDataResponse?) =
        fireEvent(Event.AlertsChanged(alerts))

    override fun routeCardDataChanged(routeCardData: List<RouteCardData>?) =
        fireEvent(Event.RouteCardDataChanged(routeCardData))

    override fun colorPaletteChanged(isDarkMode: Boolean) =
        fireEvent(Event.ColorPaletteChanged(isDarkMode))

    override fun densityChanged(density: Float) = fireEvent(Event.DensityChanged(density))

    override fun mapStyleLoaded() = fireEvent(Event.MapStyleLoaded)

    override fun layerManagerInitialized(layerManager: IMapLayerManager) =
        fireEvent(Event.LayerManagerInitialized(layerManager))

    override fun locationPermissionsChanged(hasPermission: Boolean) =
        fireEvent(Event.LocationPermissionsChanged(hasPermission))

    override fun setViewportManager(viewportManager: ViewportManager) {
        this.viewportManager = viewportManager
    }

    private suspend fun handleNavChange(
        currentState: State,
        currentNavEntry: SheetRoutes?,
        previousNavEntry: SheetRoutes?,
        globalResponse: GlobalResponse?,
        railRouteShapes: MapFriendlyRouteResponse?,
        isDarkMode: Boolean,
        railRouteSourceData: List<RouteSourceData>?,
        stopId: String?,
        stopFilter: StopDetailsFilter?,
        layerManager: IMapLayerManager?,
    ): State {
        val stopDetails =
            when (currentNavEntry) {
                is SheetRoutes.StopDetails -> currentNavEntry
                else -> null
            }
        val stop = globalResponse?.getStop(stopDetails?.stopId)
        val routePickerOrDetails =
            currentNavEntry is SheetRoutes.RoutePicker ||
                currentNavEntry is SheetRoutes.RouteDetails
        val newState =
            if (routePickerOrDetails && currentState is State.VehicleSelected) {
                State.StopSelected(currentState.stop, currentState.stopFilter)
            } else if (stopDetails == null) {
                State.Unfiltered
            } else {
                State.StopSelected(stop, stopDetails.stopFilter)
            }
        // If we're already in this state, there's no need to perform these actions again
        if (newState == currentState) return currentState
        handleViewportRestoration(
            currentNavEntry,
            previousNavEntry,
            globalResponse,
            railRouteShapes,
            isDarkMode,
            railRouteSourceData,
            stopId,
            stopFilter,
            layerManager,
        )
        return newState
    }

    private suspend fun handleViewportRestoration(
        currentNavEntry: SheetRoutes?,
        previousNavEntry: SheetRoutes?,
        globalResponse: GlobalResponse?,
        railRouteShapes: MapFriendlyRouteResponse?,
        isDarkMode: Boolean,
        railRouteSourceData: List<RouteSourceData>?,
        stopId: String?,
        stopFilter: StopDetailsFilter?,
        layerManager: IMapLayerManager?,
    ) {
        if (
            (previousNavEntry is SheetRoutes.NearbyTransit ||
                previousNavEntry is SheetRoutes.Favorites) &&
                currentNavEntry is SheetRoutes.StopDetails
        ) {
            viewportManager.saveNearbyTransitViewport()
        } else if (
            previousNavEntry is SheetRoutes.StopDetails &&
                (currentNavEntry is SheetRoutes.NearbyTransit ||
                    currentNavEntry is SheetRoutes.Favorites)
        ) {
            refreshRouteLineSource(
                globalResponse,
                railRouteShapes,
                isDarkMode,
                railRouteSourceData,
                stopId,
                stopFilter,
                layerManager,
            )
            viewportManager.restoreNearbyTransitViewport()
        }
    }

    private suspend fun refreshRouteLineSource(
        globalResponse: GlobalResponse?,
        railRouteShapes: MapFriendlyRouteResponse?,
        isDarkMode: Boolean,
        railRouteSourceData: List<RouteSourceData>?,
        stopId: String?,
        stopFilter: StopDetailsFilter?,
        layerManager: IMapLayerManager?,
    ) {
        val routeData = railRouteSourceData ?: return
        val stopLayerGeneratorState = stopLayerGeneratorState(stopId, stopFilter)
        layerManager?.run {
            updateRouteSourceData(routeData)
            addLayers(
                railRouteShapes ?: return@run,
                stopLayerGeneratorState,
                globalResponse ?: return@run,
                if (isDarkMode) ColorPalette.dark else ColorPalette.light,
            )
        }
    }

    private suspend fun updateDisplayedRoutesBasedOnStop(
        globalResponse: GlobalResponse?,
        railRouteShapes: MapFriendlyRouteResponse?,
        stopId: String?,
        stopFilter: StopDetailsFilter?,
        now: Instant,
        alerts: AlertsStreamDataResponse?,
        routeCardData: List<RouteCardData>?,
        isDarkMode: Boolean?,
        layerManager: IMapLayerManager?,
    ) {
        if (globalResponse == null || railRouteShapes == null) return
        val stopMapData = stopId?.let { getStopMapData(stopId = it) } ?: return

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
            RouteFeaturesBuilder.generateRouteSources(
                filteredRoutes,
                globalResponse,
                globalMapData(now, globalResponse, alerts),
            )
        val stopLayerGeneratorState = stopLayerGeneratorState(stopId, stopFilter)
        layerManager?.run {
            updateRouteSourceData(newRailData)
            addLayers(
                filteredRoutes,
                stopLayerGeneratorState,
                globalResponse,
                if (isDarkMode == true) ColorPalette.dark else ColorPalette.light,
            )
        }
    }

    private suspend fun stopSourceData(
        globalMapData: GlobalMapData?,
        railRouteSourceData: List<RouteSourceData>?,
    ) =
        withContext(Dispatchers.Default) {
            StopFeaturesBuilder.buildCollection(globalMapData, railRouteSourceData.orEmpty())
        }

    private suspend fun stopLayerGeneratorState(
        stopId: String?,
        stopFilter: StopDetailsFilter?,
    ): StopLayerGenerator.State =
        withContext(Dispatchers.Default) { StopLayerGenerator.State(stopId, stopFilter) }

    private suspend fun globalMapData(
        now: Instant,
        globalResponse: GlobalResponse?,
        alertsData: AlertsStreamDataResponse?,
    ): GlobalMapData? =
        withContext(Dispatchers.Default) {
            globalResponse?.let { GlobalMapData(it, alertsData, now) }
        }

    private suspend fun getStopMapData(stopId: String): StopMapResponse? =
        withContext(Dispatchers.IO) {
            when (val data = stopRepository.getStopMapData(stopId)) {
                is ApiResult.Ok -> data.data
                is ApiResult.Error -> null
            }
        }

    private suspend fun routeLineData(
        globalResponse: GlobalResponse?,
        globalMapData: GlobalMapData?,
        railRouteShapes: MapFriendlyRouteResponse?,
    ): List<RouteSourceData>? {
        if (globalResponse == null || railRouteShapes == null) return null
        return RouteFeaturesBuilder.generateRouteSources(
            railRouteShapes.routesWithSegmentedShapes,
            globalResponse,
            globalMapData,
        )
    }

    private suspend fun fetchRailRouteShapes(): MapFriendlyRouteResponse? =
        withContext(Dispatchers.IO) {
            when (val data = railRouteShapeRepository.getRailRouteShapes()) {
                is ApiResult.Ok -> data.data
                is ApiResult.Error -> null
            }
        }
}
