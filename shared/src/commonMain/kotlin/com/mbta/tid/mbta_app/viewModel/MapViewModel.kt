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
import com.mbta.tid.mbta_app.map.style.FeatureCollection
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
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

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
    private val defaultCoroutineDispatcher: CoroutineDispatcher,
    private val iOCoroutineDispatcher: CoroutineDispatcher,
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

        data object Overview : State()

        data class StopSelected(val stop: Stop, val stopFilter: StopDetailsFilter?) : State()

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

        // Cached sources to display in overview mode
        var allRailRouteSourceData by remember { mutableStateOf<List<RouteSourceData>?>(null) }
        var allRailRouteShapes by remember { mutableStateOf<MapFriendlyRouteResponse?>(null) }
        var allStopSourceData by remember { mutableStateOf<FeatureCollection?>(null) }

        // The current route source & stop layer state to display
        var routeSourceData by remember { mutableStateOf<List<RouteSourceData>?>(null) }
        var routeShapes by remember {
            mutableStateOf<List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>?>(null)
        }
        var stopLayerGeneratorState by remember {
            mutableStateOf(StopLayerGenerator.State(null, null))
        }

        var alerts by remember { mutableStateOf<AlertsStreamDataResponse?>(null) }
        var previousNavEntry by remember { mutableStateOf<SheetRoutes?>(null) }
        var routeCardData by remember { mutableStateOf<List<RouteCardData>?>(null) }
        var isDarkMode by remember { mutableStateOf<Boolean?>(null) }
        var density by remember { mutableStateOf<Float?>(null) }
        var layerManager by remember { mutableStateOf<IMapLayerManager?>(null) }
        var state by remember { mutableStateOf<State>(State.Overview) }
        val (stopId: String?, stopFilter: StopDetailsFilter?) =
            when (state) {
                is State.StopSelected -> {
                    val currentState = (state as State.StopSelected)
                    currentState.stop.id to currentState.stopFilter
                }
                is State.Overview -> null to null
                is State.VehicleSelected -> {
                    val currentState = (state as State.VehicleSelected)
                    currentState.stop?.id to null
                }
            }
        LaunchedEffect(null) { globalRepository.getGlobalData() }
        LaunchedEffect(null) { allRailRouteShapes = fetchRailRouteShapes() }
        LaunchedEffect(now, globalData, alerts) {
            globalMapData = globalMapData(now, globalData, alerts)
        }
        LaunchedEffect(allRailRouteShapes, globalData, globalMapData) {
            allRailRouteSourceData = routeLineData(globalData, globalMapData, allRailRouteShapes)
            allStopSourceData = stopSourceData(globalMapData, allRailRouteSourceData)
        }

        LaunchedEffect(allStopSourceData, layerManager) {
            allStopSourceData?.let { layerManager?.updateStopSourceData(it) }
        }

        LaunchedEffect(allRailRouteSourceData, allStopSourceData, state) {
            if (state is State.Overview) {
                routeSourceData = allRailRouteSourceData
                routeShapes = allRailRouteShapes?.routesWithSegmentedShapes
                stopLayerGeneratorState = StopLayerGenerator.State(null, null)
            }
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
                            )
                        previousNavEntry = event.currentNavEntry
                    }
                    Event.Recenter -> {
                        when (state) {
                            is State.StopSelected -> {
                                viewportManager.follow(null)
                            }
                            is State.Overview -> {
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
                        state = State.VehicleSelected(event.vehicle, event.stop, event.stopFilter)
                    }
                    is Event.MapStyleLoaded -> {
                        layerManager?.run {
                            addLayers(
                                allRailRouteShapes ?: return@run,
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

        LaunchedEffect(stopId, stopFilter, allRailRouteShapes, globalData, globalMapData) {
            if (stopId != null) {
                val featuresToDisplayForStop =
                    featuresToDisplayForStop(
                        globalResponse = globalData,
                        railRouteShapes = allRailRouteShapes,
                        stopId = stopId,
                        stopFilter = stopFilter,
                        globalMapData = globalMapData,
                        routeCardData = routeCardData,
                    )
                featuresToDisplayForStop?.let {
                    routeSourceData = it.first
                    routeShapes = it.second
                    stopLayerGeneratorState = it.third
                }
            }
        }

        LaunchedEffect(
            globalData,
            routeSourceData,
            routeShapes,
            stopLayerGeneratorState,
            isDarkMode,
            layerManager,
        ) {
            updateMapDisplay(
                globalData,
                routeSourceData,
                routeShapes,
                stopLayerGeneratorState,
                isDarkMode,
                layerManager,
            )
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
                if (currentState.stop != null) {
                    State.StopSelected(currentState.stop, currentState.stopFilter)
                } else {
                    State.Overview
                }
            } else if (stopDetails == null) {
                State.Overview
            } else {
                if (stop == null) {
                    State.Overview
                } else {
                    State.StopSelected(stop, stopDetails.stopFilter)
                }
            }
        // If we're already in this state, there's no need to perform these actions again
        if (newState == currentState) return currentState
        handleViewportRestoration(currentNavEntry, previousNavEntry)
        return newState
    }

    private suspend fun handleViewportRestoration(
        currentNavEntry: SheetRoutes?,
        previousNavEntry: SheetRoutes?,
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
            viewportManager.restoreNearbyTransitViewport()
        }
    }

    /**
     * Get the features that should be displayed on the map for the selected stop. Returns null if
     * missing key data, and therefore nothing to show for the stop.
     */
    private suspend fun featuresToDisplayForStop(
        globalResponse: GlobalResponse?,
        railRouteShapes: MapFriendlyRouteResponse?,
        stopId: String,
        stopFilter: StopDetailsFilter?,
        globalMapData: GlobalMapData?,
        routeCardData: List<RouteCardData>?,
    ): Triple<
        List<RouteSourceData>,
        List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>,
        StopLayerGenerator.State,
    >? {
        if (globalResponse == null || railRouteShapes == null || globalMapData == null) return null
        val stopMapData = getStopMapData(stopId = stopId) ?: return null

        val filteredRouteShapes =
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
        val filteredRouteSourceData =
            RouteFeaturesBuilder.generateRouteSources(
                filteredRouteShapes,
                globalResponse,
                globalMapData,
            )

        return Triple(
            filteredRouteSourceData,
            filteredRouteShapes,
            StopLayerGenerator.State(stopId, stopFilter),
        )
    }

    /** Set the map's sources and layers based on the given data */
    private suspend fun updateMapDisplay(
        globalResponse: GlobalResponse?,
        routeSourceData: List<RouteSourceData>?,
        routeShapes: List<MapFriendlyRouteResponse.RouteWithSegmentedShapes>?,
        stopLayerGeneratorState: StopLayerGenerator.State,
        isDarkMode: Boolean?,
        layerManager: IMapLayerManager?,
    ) {
        if (globalResponse == null || routeSourceData == null || routeShapes == null) {
            return
        }

        layerManager?.updateRouteSourceData(routeSourceData)
        layerManager?.addLayers(
            routeShapes,
            stopLayerGeneratorState,
            globalResponse,
            if (isDarkMode == true) ColorPalette.dark else ColorPalette.light,
        )
    }

    private suspend fun stopSourceData(
        globalMapData: GlobalMapData?,
        railRouteSourceData: List<RouteSourceData>?,
    ) =
        withContext(defaultCoroutineDispatcher) {
            StopFeaturesBuilder.buildCollection(globalMapData, railRouteSourceData.orEmpty())
        }

    private suspend fun globalMapData(
        now: Instant,
        globalResponse: GlobalResponse?,
        alertsData: AlertsStreamDataResponse?,
    ): GlobalMapData? =
        withContext(defaultCoroutineDispatcher) {
            globalResponse?.let { GlobalMapData(it, alertsData, now) }
        }

    private suspend fun getStopMapData(stopId: String): StopMapResponse? =
        withContext(iOCoroutineDispatcher) {
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
            defaultCoroutineDispatcher,
        )
    }

    private suspend fun fetchRailRouteShapes(): MapFriendlyRouteResponse? =
        withContext(iOCoroutineDispatcher) {
            when (val data = railRouteShapeRepository.getRailRouteShapes()) {
                is ApiResult.Ok -> data.data
                is ApiResult.Error -> null
            }
        }
}
