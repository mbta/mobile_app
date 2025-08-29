package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public interface IRouteCardDataViewModel {
    public val models: StateFlow<RouteCardDataViewModel.State>

    public fun setRouteCardData(data: List<RouteCardData>?)
}

public class RouteCardDataViewModel(private val sentryRepository: ISentryRepository) :
    MoleculeViewModel<RouteCardDataViewModel.Event, RouteCardDataViewModel.State>(),
    IRouteCardDataViewModel {
    public sealed class Event {
        public data class SetRouteCardData(val data: List<RouteCardData>?) : Event()
    }

    public data class State(val data: List<RouteCardData>?)

    override val models: StateFlow<State>
        get() = internalModels

    @Composable
    override fun runLogic(): State {
        var routeCardData: List<RouteCardData>? by remember { mutableStateOf(null) }

        EventSink(eventHandlingTimeout = 1.seconds, sentryRepository = sentryRepository) { event ->
            when (event) {
                is Event.SetRouteCardData -> routeCardData = event.data
            }
        }

        return State(routeCardData)
    }

    override fun setRouteCardData(data: List<RouteCardData>?): Unit =
        fireEvent(Event.SetRouteCardData(data))
}

public class MockRouteCardDataViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: RouteCardDataViewModel.State = RouteCardDataViewModel.State(null)) :
    IRouteCardDataViewModel {
    public var onSetRouteCardData: (List<RouteCardData>?) -> Unit = {}

    override val models: MutableStateFlow<RouteCardDataViewModel.State> =
        MutableStateFlow(initialState)

    override fun setRouteCardData(data: List<RouteCardData>?): Unit = onSetRouteCardData(data)
}
