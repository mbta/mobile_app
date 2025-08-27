package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.model.TripDetailsStopList
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.stopDetailsPage.TripData
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.ITripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ITripRepository
import com.mbta.tid.mbta_app.repositories.IVehicleRepository
import com.mbta.tid.mbta_app.viewModel.StopDetailsViewModel.Event
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getGlobalData
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getTripData
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getTripDetailsStopList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public interface ITripDetailsViewModel {
    public val models: StateFlow<TripDetailsViewModel.State>
    public val selectedVehicleUpdates: StateFlow<Vehicle?>

    public fun setActive(active: Boolean, wasSentToBackground: Boolean = false)

    public fun setAlerts(alerts: AlertsStreamDataResponse?)

    public fun setContext(context: TripDetailsViewModel.Context)

    public fun setFilters(filters: TripDetailsPageFilter?)
}

public class TripDetailsViewModel(
    private val errorBannerRepository: IErrorBannerStateRepository,
    private val tripPredictionsRepository: ITripPredictionsRepository,
    private val tripRepository: ITripRepository,
    private val vehicleRepository: IVehicleRepository,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) :
    MoleculeViewModel<TripDetailsViewModel.Event, TripDetailsViewModel.State>(),
    ITripDetailsViewModel {
    public sealed class Event {
        public data class SetActive(val active: Boolean, val wasSentToBackground: Boolean) :
            Event()

        public data class SetAlerts(val alerts: AlertsStreamDataResponse?) : Event()

        public data class SetContext(val context: Context) : Event()

        public data class SetFilters(val filters: TripDetailsPageFilter?) : Event()
    }

    public data class State(
        val tripData: TripData? = null,
        val stopList: TripDetailsStopList? = null,
        val awaitingPredictionsAfterBackground: Boolean = false,
    )

    public sealed class Context {
        public data object StopDetails : Context()

        public data object TripDetails : Context()
    }

    @Composable
    override fun runLogic(events: Flow<TripDetailsViewModel.Event>): State {
        var context: Context? by remember { mutableStateOf(null) }
        var awaitingPredictionsAfterBackground: Boolean by remember { mutableStateOf(false) }

        var active: Boolean by remember { mutableStateOf(true) }
        var alerts: AlertsStreamDataResponse? by remember { mutableStateOf(null) }
        var filters: TripDetailsPageFilter? by remember { mutableStateOf(null) }

        val errorKey = "TripDetailsViewModel"
        val globalData =
            getGlobalData("$errorKey.getGlobalData", coroutineDispatcher = coroutineDispatcher)

        val tripData =
            getTripData(
                filters,
                active,
                context,
                { awaitingPredictionsAfterBackground = false },
                errorKey,
                coroutineDispatcher,
                errorBannerRepository,
                tripPredictionsRepository,
                tripRepository,
                vehicleRepository,
            )

        val stopList = getTripDetailsStopList(filters, tripData, alerts, globalData)

        val state =
            remember(context, tripData, stopList, awaitingPredictionsAfterBackground) {
                State(
                    tripData,
                    stopList,
                    context is Context.TripDetails && awaitingPredictionsAfterBackground,
                )
            }

        LaunchedEffect(Unit) {
            events.collect { event ->
                when (event) {
                    is Event.SetActive -> {
                        active = event.active
                        if (event.wasSentToBackground) {
                            awaitingPredictionsAfterBackground = true
                        }
                    }
                    is Event.SetAlerts -> alerts = event.alerts
                    is Event.SetContext -> context = event.context
                    is Event.SetFilters -> filters = event.filters
                }
            }
        }

        LaunchedEffect(filters, tripData?.vehicle) {
            _selectedVehicleUpdates.tryEmit(
                tripData?.vehicle?.takeIf { it.id == filters?.vehicleId }
            )
        }

        return state
    }

    override val models: StateFlow<State>
        get() = internalModels

    private val _selectedVehicleUpdates = MutableStateFlow<Vehicle?>(null)
    public override val selectedVehicleUpdates: StateFlow<Vehicle?> = _selectedVehicleUpdates

    override fun setActive(active: Boolean, wasSentToBackground: Boolean): Unit =
        fireEvent(Event.SetActive(active, wasSentToBackground))

    override fun setAlerts(alerts: AlertsStreamDataResponse?): Unit =
        fireEvent(Event.SetAlerts(alerts))

    override fun setContext(context: Context): Unit = fireEvent(Event.SetContext(context))

    override fun setFilters(filters: TripDetailsPageFilter?): Unit =
        fireEvent(Event.SetFilters(filters))
}

public class MockTripDetailsViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: TripDetailsViewModel.State = TripDetailsViewModel.State()) :
    ITripDetailsViewModel {
    public var onSetActive: (Boolean, Boolean) -> Unit = { _, _ -> }
    public var onSetAlerts: (AlertsStreamDataResponse?) -> Unit = {}
    public var onSetContext: (TripDetailsViewModel.Context) -> Unit = {}
    public var onSetFilters: (TripDetailsPageFilter?) -> Unit = {}

    override val models: MutableStateFlow<TripDetailsViewModel.State> =
        MutableStateFlow(initialState)
    override val selectedVehicleUpdates: MutableStateFlow<Vehicle?> = MutableStateFlow(null)

    override fun setActive(active: Boolean, wasSentToBackground: Boolean): Unit =
        onSetActive(active, wasSentToBackground)

    override fun setAlerts(alerts: AlertsStreamDataResponse?): Unit = onSetAlerts(alerts)

    override fun setContext(context: TripDetailsViewModel.Context): Unit = onSetContext(context)

    override fun setFilters(filters: TripDetailsPageFilter?): Unit = onSetFilters(filters)
}
