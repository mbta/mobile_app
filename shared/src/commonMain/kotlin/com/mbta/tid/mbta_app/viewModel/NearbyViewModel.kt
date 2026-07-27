package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.repositories.ErrorKey
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.isRoughlyEqualTo
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.LoadedPredictions
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.LoadedSchedules
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getGlobalData
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getSchedules
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.subscribeToPredictions
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.jvm.JvmName
import kotlin.native.ShouldRefineInSwift
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.sample
import org.maplibre.spatialk.geojson.Position

@OptIn(ExperimentalObjCRefinement::class)
public interface INearbyViewModel {
    public val models: StateFlow<NearbyViewModel.State>

    public fun setActive(active: Boolean, wasSentToBackground: Boolean = false)

    public fun setAlerts(alerts: AlertsStreamDataResponse?)

    public fun setLocation(location: Position?)

    public fun setNow(now: EasternTimeInstant)
}

@OptIn(ExperimentalObjCRefinement::class)
public class NearbyViewModel(
    private val nearbyRepository: INearbyRepository,
    private val sentryRepository: ISentryRepository,
    private val coroutineDispatcher: CoroutineDispatcher,
) : MoleculeViewModel<NearbyViewModel.Event, NearbyViewModel.State>(), INearbyViewModel {

    public sealed class Context {
        public data object Favorites : Context()

        public data object Edit : Context()
    }

    public sealed interface Event {
        public data class SetActive(val active: Boolean, val wasSentToBackground: Boolean) : Event

        public data class SetLocation(val location: Position?) : Event
    }

    public data class State(
        val awaitingPredictionsAfterBackground: Boolean,
        val routeCardData: List<RouteCardData>?,
        val loadedLocation: Position?,
        val loadedStopIds: List<String>?,
    ) {
        public constructor() : this(false, null, null, null)
    }

    internal data class StopsAtLocation(val stopIds: List<String>, val location: Position?)

    @set:JvmName("setAlertsState")
    private var alerts by mutableStateOf<AlertsStreamDataResponse?>(null)
    @set:JvmName("setLocationState") private var location by mutableStateOf<Position?>(null)
    @set:JvmName("setNowState") private var now by mutableStateOf(EasternTimeInstant.now())

    @OptIn(FlowPreview::class)
    @Composable
    override fun runLogic(): State {
        var awaitingPredictionsAfterBackground: Boolean by remember { mutableStateOf(false) }
        var routeCardData: List<RouteCardData>? by remember { mutableStateOf(null) }
        var loadedLocation: Position? by remember { mutableStateOf(null) }
        var nearbyResponse: NearbyResponse? by remember { mutableStateOf(null) }
        var locationStops: StopsAtLocation? by remember { mutableStateOf(null) }
        var loadedLocationStops: StopsAtLocation? by remember { mutableStateOf(null) }

        var active: Boolean by remember { mutableStateOf(false) }

        val errorKey = ErrorKey(setOf(SheetRoutes.NearbyTransit::class), "NearbyViewModel")
        val globalData = getGlobalData(errorKey)
        val schedules = getSchedules(locationStops?.stopIds?.toSet(), errorKey)
        val predictions =
            subscribeToPredictions(
                locationStops?.stopIds?.toSet(),
                SheetRoutes.NearbyTransit,
                active,
                errorKey,
                onAnyMessageReceived = { awaitingPredictionsAfterBackground = false },
            )

        EventSink(eventHandlingTimeout = 2.seconds, sentryRepository = sentryRepository) { event ->
            when (event) {
                is Event.SetLocation -> {
                    if (!(event.location?.let { loadedLocation?.isRoughlyEqualTo(it) } ?: false)) {
                        location = event.location
                    }
                }
                is Event.SetActive -> {
                    active = event.active
                    if (event.wasSentToBackground) {
                        awaitingPredictionsAfterBackground = true
                    }
                }
            }
        }

        LaunchedEffect(globalData, location) {
            val resolvedLocation = location
            if (globalData == null || resolvedLocation == null) return@LaunchedEffect
            nearbyResponse = nearbyRepository.getStopIdsNearby(globalData, resolvedLocation)
            locationStops =
                nearbyResponse?.filter(globalData, alerts, now)?.let {
                    StopsAtLocation(it, resolvedLocation)
                }
        }

        data class RouteCardDataParams(
            val location: Position?,
            val locationStops: StopsAtLocation?,
            val globalData: GlobalResponse?,
            val schedules: LoadedSchedules?,
            val predictions: LoadedPredictions?,
            val alerts: AlertsStreamDataResponse?,
            val now: EasternTimeInstant,
        )

        // Put all route card params into a single debounceable value. If we just put all the params
        // as keys to a LaunchedEffect, then routeCardData setting can get interrupted by frequent
        // changes to predictions or now, which can chain and significantly delay updates.
        var params: RouteCardDataParams? by remember { mutableStateOf(null) }
        LaunchedEffect(location, locationStops, globalData, schedules, predictions, alerts, now) {
            params =
                RouteCardDataParams(
                    location,
                    locationStops,
                    globalData,
                    schedules,
                    predictions,
                    alerts,
                    now,
                )
        }

        LaunchedEffect(Unit) {
            snapshotFlow { params }
                .sample(100.milliseconds)
                .conflate()
                .collect {
                    if (it == null) return@collect
                    val resolvedLocationStops = it.locationStops
                    val resolvedStopIds = resolvedLocationStops?.stopIds
                    val stopIdSet = resolvedStopIds?.toSet()
                    if (stopIdSet == null || it.globalData == null) {
                        routeCardData = null
                        loadedLocationStops = null
                    } else if (stopIdSet.isEmpty()) {
                        routeCardData = emptyList()
                        loadedLocationStops = resolvedLocationStops
                    } else if (
                        it.location == resolvedLocationStops.location &&
                            it.schedules?.stopIds == stopIdSet &&
                            it.predictions?.stopIds == stopIdSet
                    ) {
                        routeCardData =
                            RouteCardData.routeCardsForStopList(
                                resolvedLocationStops.stopIds,
                                it.globalData,
                                resolvedLocationStops.location,
                                it.schedules.response,
                                it.predictions.response,
                                it.alerts,
                                it.now,
                                RouteCardData.Context.NearbyTransit,
                                null,
                                coroutineDispatcher,
                            )
                        loadedLocationStops = resolvedLocationStops
                    }
                }
        }

        return State(
            awaitingPredictionsAfterBackground,
            routeCardData,
            loadedLocationStops?.location,
            loadedLocationStops?.stopIds,
        )
    }

    override val models: StateFlow<State>
        get() = internalModels

    override fun setActive(active: Boolean, wasSentToBackground: Boolean): Unit =
        fireEvent(Event.SetActive(active, wasSentToBackground))

    override fun setAlerts(alerts: AlertsStreamDataResponse?) {
        this.alerts = alerts
    }

    override fun setLocation(location: Position?): Unit = fireEvent(Event.SetLocation(location))

    override fun setNow(now: EasternTimeInstant) {
        this.now = now
    }
}

@OptIn(ExperimentalObjCRefinement::class)
public class MockNearbyViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: NearbyViewModel.State = NearbyViewModel.State()) : INearbyViewModel {
    public var onSetActive: (Boolean, Boolean) -> Unit = { _, _ -> }
    public var onSetAlerts: (AlertsStreamDataResponse?) -> Unit = {}
    public var onSetLocation: (Position?) -> Unit = {}
    public var onSetNow: (EasternTimeInstant) -> Unit = { _ -> }
    @ShouldRefineInSwift
    public var onUpdateFavorites: (Map<RouteStopDirection, FavoriteSettings?>) -> Unit = { _ -> }

    override val models: MutableStateFlow<NearbyViewModel.State> = MutableStateFlow(initialState)

    override fun setActive(active: Boolean, wasSentToBackground: Boolean) {
        onSetActive(active, wasSentToBackground)
    }

    override fun setAlerts(alerts: AlertsStreamDataResponse?) {
        onSetAlerts(alerts)
    }

    override fun setLocation(location: Position?) {
        onSetLocation(location)
    }

    override fun setNow(now: EasternTimeInstant) {
        onSetNow(now)
    }
}
