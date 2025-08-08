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
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.MapFriendlyRouteResponse
import com.mbta.tid.mbta_app.model.response.StopMapResponse
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.IStopRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.IMapLayerManager
import com.mbta.tid.mbta_app.utils.ViewportManager
import com.mbta.tid.mbta_app.utils.timer
import com.mbta.tid.mbta_app.viewModel.MapViewModel.Event
import com.mbta.tid.mbta_app.viewModel.MapViewModel.Event.RecenterType
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

public interface IMapViewModel {

    public val models: StateFlow<MapViewModel.State>

    public fun selectedStop(stop: Stop, stopFilter: StopDetailsFilter?)

    public fun selectedTrip(
        stopFilter: StopDetailsFilter?,
        stop: Stop?,
        tripFilter: TripDetailsFilter,
        vehicle: Vehicle?,
    )

    public fun navChanged(currentNavEntry: SheetRoutes?)

    public fun recenter(type: RecenterType = RecenterType.CurrentLocation)

    public fun alertsChanged(alerts: AlertsStreamDataResponse?)

    public fun routeCardDataChanged(routeCardData: List<RouteCardData>?)

    public fun colorPaletteChanged(isDarkMode: Boolean)

    public fun densityChanged(density: Float)

    public fun mapStyleLoaded()

    public fun layerManagerInitialized(layerManager: IMapLayerManager)

    public fun locationPermissionsChanged(hasPermission: Boolean)

    public fun setViewportManager(viewportManager: ViewportManager)
}

public class MapViewModel(
    private val globalRepository: IGlobalRepository,
    private val railRouteShapeRepository: IRailRouteShapeRepository,
    private val stopRepository: IStopRepository,
    private val defaultCoroutineDispatcher: CoroutineDispatcher,
    private val iOCoroutineDispatcher: CoroutineDispatcher,
) : MoleculeViewModel<Event, MapViewModel.State>(), IMapViewModel {

    private lateinit var viewportManager: ViewportManager
    private val routeCardDataUpdates = MutableStateFlow<List<RouteCardData>?>(null)

    public sealed interface Event {

        public enum class RecenterType {
            CurrentLocation,
            Trip,
        }

        public data class SelectedStop
        internal constructor(internal val stop: Stop, internal val stopFilter: StopDetailsFilter?) :
            Event

        public data class SelectedTrip
        internal constructor(
            internal val stop: Stop?,
            internal val stopFilter: StopDetailsFilter?,
            internal val tripFilter: TripDetailsFilter,
            internal val vehicle: Vehicle?,
        ) : Event

        public data class NavChanged
        internal constructor(internal val currentNavEntry: SheetRoutes?) : Event

        public data class Recenter(val type: RecenterType) : Event

        public data class AlertsChanged(val alerts: AlertsStreamDataResponse?) : Event

        public data class ColorPaletteChanged(val isDarkMode: Boolean) : Event

        public data class DensityChanged(val density: Float) : Event

        public data object MapStyleLoaded : Event

        public data class LayerManagerInitialized
        internal constructor(internal val layerManager: IMapLayerManager) : Event

        public data class LocationPermissionsChanged(val hasPermission: Boolean) : Event
    }

    public sealed class State {

        internal abstract val stop: Stop?
        internal abstract val stopFilter: StopDetailsFilter?

        public data object Overview : State() {
            override val stop: Stop? = null
            override val stopFilter: StopDetailsFilter? = null
        }

        public data class StopSelected(
            override val stop: Stop,
            override val stopFilter: StopDetailsFilter?,
        ) : State()

        public data class TripSelected
        internal constructor(
            override val stop: Stop?,
            override val stopFilter: StopDetailsFilter?,
            internal val tripFilter: TripDetailsFilter,
            val vehicle: Vehicle?,
        ) : State()
    }

    @Composable
    override fun runLogic(events: Flow<Event>): State {
        val now by timer(updateInterval = 300.seconds)
        val globalData by globalRepository.state.collectAsState()
        var globalMapData by remember { mutableStateOf<GlobalMapData?>(null) }
        val routeCardData by routeCardDataUpdates.collectAsState()

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
        var isDarkMode by remember { mutableStateOf<Boolean?>(null) }
        var density by remember { mutableStateOf<Float?>(null) }
        var layerManager by remember { mutableStateOf<IMapLayerManager?>(null) }
        var state by remember { mutableStateOf<State>(State.Overview) }
        val (stopId: String?, stopFilter: StopDetailsFilter?) = state.stop?.id to state.stopFilter

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
                    is Event.NavChanged -> {
                        state =
                            handleNavChange(
                                state,
                                event.currentNavEntry,
                                previousNavEntry,
                                globalData,
                                density,
                            )
                        previousNavEntry = event.currentNavEntry
                    }
                    is Event.Recenter -> {
                        when (state) {
                            is State.Overview,
                            is State.StopSelected -> {
                                viewportManager.follow(null)
                            }
                            is State.TripSelected -> {
                                when (event.type) {
                                    RecenterType.CurrentLocation -> viewportManager.follow(null)
                                    RecenterType.Trip -> {
                                        val currentState = state as State.TripSelected
                                        handleViewportCentering(currentState, density)
                                    }
                                }
                            }
                        }
                    }
                    is Event.SelectedStop -> {
                        viewportManager.saveNearbyTransitViewport()
                        val newState = State.StopSelected(event.stop, event.stopFilter)
                        handleViewportCentering(newState, density)
                        state = newState
                    }
                    is Event.SelectedTrip -> {
                        val currentState = (state as? State.TripSelected)
                        val newState =
                            State.TripSelected(
                                event.stop,
                                event.stopFilter,
                                event.tripFilter,
                                event.vehicle,
                            )
                        if (currentState?.vehicle?.id != newState.vehicle?.id) {
                            handleViewportCentering(newState, density)
                        }
                        state = newState
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

    override val models: StateFlow<State>
        get() = internalModels

    override fun selectedStop(stop: Stop, stopFilter: StopDetailsFilter?): Unit =
        fireEvent(Event.SelectedStop(stop, stopFilter))

    override fun selectedTrip(
        stopFilter: StopDetailsFilter?,
        stop: Stop?,
        tripFilter: TripDetailsFilter,
        vehicle: Vehicle?,
    ): Unit = fireEvent(Event.SelectedTrip(stop, stopFilter, tripFilter, vehicle))

    override fun navChanged(currentNavEntry: SheetRoutes?): Unit =
        fireEvent(Event.NavChanged(currentNavEntry))

    override fun recenter(type: RecenterType): Unit = fireEvent(Event.Recenter(type))

    override fun alertsChanged(alerts: AlertsStreamDataResponse?): Unit =
        fireEvent(Event.AlertsChanged(alerts))

    // Route card data is sent through a separate StateFlow rather than the event flow because
    // frequent updates were causing buffer overflows, and we only care about the latest value.
    override fun routeCardDataChanged(routeCardData: List<RouteCardData>?) {
        routeCardDataUpdates.tryEmit(routeCardData)
    }

    override fun colorPaletteChanged(isDarkMode: Boolean): Unit =
        fireEvent(Event.ColorPaletteChanged(isDarkMode))

    override fun densityChanged(density: Float): Unit = fireEvent(Event.DensityChanged(density))

    override fun mapStyleLoaded(): Unit = fireEvent(Event.MapStyleLoaded)

    override fun layerManagerInitialized(layerManager: IMapLayerManager): Unit =
        fireEvent(Event.LayerManagerInitialized(layerManager))

    override fun locationPermissionsChanged(hasPermission: Boolean): Unit =
        fireEvent(Event.LocationPermissionsChanged(hasPermission))

    override fun setViewportManager(viewportManager: ViewportManager) {
        this.viewportManager = viewportManager
    }

    private suspend fun handleNavChange(
        currentState: State,
        newNavEntry: SheetRoutes?,
        previousNavEntry: SheetRoutes?,
        globalResponse: GlobalResponse?,
        density: Float?,
    ): State {
        val currentNavEntryStopDetails =
            when (newNavEntry) {
                is SheetRoutes.StopDetails -> newNavEntry
                else -> null
            }
        val stop = globalResponse?.getStop(currentNavEntryStopDetails?.stopId)
        val routePickerOrDetails =
            newNavEntry is SheetRoutes.RoutePicker || newNavEntry is SheetRoutes.RouteDetails
        val newState =
            if (routePickerOrDetails && currentState is State.TripSelected) {
                currentState.stop?.let { State.StopSelected(it, currentState.stopFilter) }
                    ?: State.Overview
            } else if (currentNavEntryStopDetails == null) {
                State.Overview
            } else {
                if (stop == null) {
                    State.Overview
                } else {
                    if (currentNavEntryStopDetails.tripFilter != null) {
                        State.TripSelected(
                            stop,
                            currentNavEntryStopDetails.stopFilter,
                            currentNavEntryStopDetails.tripFilter,
                            null,
                        )
                    } else {

                        State.StopSelected(stop, currentNavEntryStopDetails.stopFilter)
                    }
                }
            }
        // If we're already in this state, there's no need to perform these actions again
        if (newState == currentState) return currentState
        handleViewportRestoration(newNavEntry, previousNavEntry)
        handleViewportCentering(newState, density)

        return newState
    }

    private suspend fun handleViewportCentering(state: State, density: Float?) {
        when (state) {
            State.Overview -> {}
            is State.StopSelected -> {
                viewportManager.stopCenter(state.stop)
            }
            is State.TripSelected -> {
                // there is no vehicle associated with the trip, so just center on the stop
                // if there is one
                if (state.tripFilter.vehicleId == null) {
                    state.stop?.let { viewportManager.stopCenter(it) }
                } else {
                    // if there is a vehicle id associated with the trip but there
                    // isn't a vehicle yet, wait for one to load before centering
                    state.vehicle?.let {
                        if (density != null) {
                            viewportManager.vehicleOverview(it, state.stop, density)
                        }
                    }
                }
            }
        }
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
        now: EasternTimeInstant,
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
