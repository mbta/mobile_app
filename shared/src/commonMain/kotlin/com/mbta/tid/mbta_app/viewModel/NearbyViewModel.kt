package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.repositories.ErrorKey
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.isRoughlyEqualTo
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getGlobalData
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getSchedules
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.subscribeToPredictions
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.jvm.JvmName
import kotlin.native.ShouldRefineInSwift
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    @set:JvmName("setAlertsState")
    private var alerts by mutableStateOf<AlertsStreamDataResponse?>(null)
    @set:JvmName("setLocationState") private var location by mutableStateOf<Position?>(null)
    @set:JvmName("setNowState") private var now by mutableStateOf(EasternTimeInstant.now())

    @Composable
    override fun runLogic(): State {
        var awaitingPredictionsAfterBackground: Boolean by remember { mutableStateOf(false) }
        var routeCardData: List<RouteCardData>? by remember { mutableStateOf(null) }
        var loadedLocation: Position? by remember { mutableStateOf(null) }
        var nearbyResponse: NearbyResponse? by remember { mutableStateOf(null) }
        var stopIds: List<String>? by remember { mutableStateOf(null) }
        var loadedStopIds: List<String>? by remember { mutableStateOf(null) }

        var active: Boolean by remember { mutableStateOf(false) }

        val errorKey = ErrorKey(setOf(SheetRoutes.NearbyTransit::class), "NearbyViewModel")
        val globalData = getGlobalData(errorKey)
        val schedules = getSchedules(stopIds, errorKey)
        val predictions =
            subscribeToPredictions(
                stopIds,
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
            val newStopIds = nearbyResponse?.filter(globalData, alerts, now)?.sorted()
            stopIds = newStopIds

            if (stopIds == newStopIds) {
                println("KB stops are the same")
            } else {
                println("KB stops are different")
            }
        }

        LaunchedEffect(stopIds) {
            if (stopIds?.toSet() != loadedStopIds?.toSet()) {
                //       routeCardData = null
                //          loadedLocation = null
                //       loadedStopIds = null
            }
        }

        LaunchedEffect(stopIds, globalData, location, schedules, predictions, alerts, now) {
            val resolvedStopIds = stopIds
            if (resolvedStopIds == null || globalData == null || location == null) {
                //      routeCardData = null
                //     loadedLocation = null
                //     loadedStopIds = null
            } else if (resolvedStopIds.isEmpty()) {
                routeCardData = emptyList()
                loadedLocation = location
                loadedStopIds = emptyList()
            } else {
                routeCardData =
                    RouteCardData.routeCardsForStopList(
                        resolvedStopIds,
                        globalData,
                        location,
                        schedules,
                        predictions,
                        alerts,
                        now,
                        RouteCardData.Context.NearbyTransit,
                        null,
                        coroutineDispatcher,
                    ) ?: routeCardData // This is sketchy and we'd want to do this a better way if
                // we actually have to do this
                loadedLocation = location
                loadedStopIds = resolvedStopIds
            }
        }

        println("KB: routecardData ${routeCardData?.size}")

        return State(
            awaitingPredictionsAfterBackground,
            routeCardData,
            loadedLocation,
            loadedStopIds,
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
