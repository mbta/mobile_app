package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.RouteCardData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

public interface IRouteCardDataViewModel {
    public val models: StateFlow<RouteCardDataViewModel.State>

    public fun setRouteCardData(data: List<RouteCardData>?)
}

public class RouteCardDataViewModel :
    MoleculeViewModel<RouteCardDataViewModel.Event, RouteCardDataViewModel.State>(),
    IRouteCardDataViewModel {
    public sealed class Event {
        public data class SetRouteCardData(val data: List<RouteCardData>?) : Event()
    }

    public data class State(val data: List<RouteCardData>?)

    override val models: StateFlow<State>
        get() = internalModels

    @Composable
    override fun runLogic(events: Flow<Event>): State {
        var routeCardData: List<RouteCardData>? by remember { mutableStateOf(null) }

        LaunchedEffect(Unit) {
            events.collect { event ->
                when (event) {
                    is Event.SetRouteCardData -> routeCardData = event.data
                }
            }
        }

        return State(routeCardData)
    }

    override fun setRouteCardData(data: List<RouteCardData>?): Unit =
        fireEvent(Event.SetRouteCardData(data))
}
