package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mbta.tid.mbta_app.model.StopDetailsPageFilters
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
import com.mbta.tid.mbta_app.utils.timer
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.getGlobalData
import com.mbta.tid.mbta_app.viewModel.composeStateHelpers.subscribeToAlerts
import com.mbta.tid.mbta_app.wrapper.State
import com.mbta.tid.mbta_app.wrapper.wrapped
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.StateFlow

public interface IFilteredStopDetailsViewModel {
    public val models: StateFlow<State>

    public fun setActive(active: Boolean, wasSentToBackground: Boolean = false)

    public fun setFilters(filters: StopDetailsPageFilters)
}

public class FilteredStopDetailsViewModel(
    private val stopDetailsVM: IStopDetailsViewModel,
    private val tripDetailsVM: ITripDetailsViewModel,
) : MoleculeViewModel<FilteredStopDetailsViewModel.Event, State>(), IFilteredStopDetailsViewModel {
    public enum class Event

    @Composable
    override fun runLogic(): State {
        val stopDetailsVMState by stopDetailsVM.models.collectAsState()
        val tripDetailsVMState by tripDetailsVM.models.collectAsState()
        val alerts = subscribeToAlerts()
        val global = getGlobalData("FilteredStopDetailsViewModel")
        val now by timer(5.seconds)

        LaunchedEffect(null) { tripDetailsVM.setContext(TripDetailsViewModel.Context.StopDetails) }

        LaunchedEffect(alerts) {
            stopDetailsVM.setAlerts(alerts)
            tripDetailsVM.setAlerts(alerts)
        }

        LaunchedEffect(now) { stopDetailsVM.setNow(now) }

        LaunchedEffect(stopDetailsVM.filterUpdates) {
            stopDetailsVM.filterUpdates.collect { if (it != null) setFilters(it) }
        }

        val routeDataFiltered =
            stopDetailsVMState.routeData as? StopDetailsViewModel.RouteData.Filtered
        val selectedDirectionId =
            stopDetailsVMState.routeData?.filters?.stopFilter?.directionId ?: 0
        val directions = routeDataFiltered?.stopData?.directions?.wrapped()
        val leaf = routeDataFiltered?.stopData?.data?.getOrNull(selectedDirectionId)
        val upcomingTripTiles =
            remember(leaf, now, global) {
                leaf
                    ?.format(now, global)
                    ?.tileData(directions?.get(selectedDirectionId)?.destination)
                    ?.wrapped()
            }
        val trip = tripDetailsVMState.tripData?.trip
        val route = global?.getRoute(trip?.routeId)
        val tripHeadsign = trip?.headsign
        val tripVehicle =
            remember(tripDetailsVMState.tripData?.vehicle, global) {
                tripDetailsVMState.tripData?.vehicle?.wrapped(global)
            }

        val splitStopList =
            remember(tripDetailsVMState.tripData, tripDetailsVMState.stopList, global) {
                val tripFilter = tripDetailsVMState.tripData?.tripFilter
                val stopList = tripDetailsVMState.stopList
                if (tripFilter != null && stopList != null) {
                    stopList.splitForTarget(tripFilter.stopId, tripFilter.stopSequence, global)
                } else null
            }

        val tripStopList =
            remember(splitStopList, trip, now, route) {
                if (splitStopList != null && trip != null && route != null)
                    splitStopList.wrapped(trip, now, route)
                else null
            }

        return State(
            directions,
            upcomingTripTiles,
            trip?.id,
            tripHeadsign,
            tripVehicle,
            tripStopList,
        )
    }

    override val models: StateFlow<State>
        get() = internalModels

    override fun setActive(active: Boolean, wasSentToBackground: Boolean) {
        stopDetailsVM.setActive(active, wasSentToBackground)
        tripDetailsVM.setActive(active, wasSentToBackground)
    }

    override fun setFilters(filters: StopDetailsPageFilters) {
        stopDetailsVM.setFilters(filters)
        tripDetailsVM.setFilters(
            if (filters.stopFilter != null && filters.tripFilter != null)
                TripDetailsPageFilter(filters.stopId, filters.stopFilter, filters.tripFilter)
            else null
        )
    }
}
