package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

public interface ISearchRoutesViewModel {

    public val models: StateFlow<SearchRoutesViewModel.State>

    public fun setQuery(query: String)
}

public class SearchRoutesViewModel
internal constructor(
    private val analytics: Analytics,
    private val globalRepository: IGlobalRepository,
    private val searchResultRepository: ISearchResultRepository,
) :
    MoleculeViewModel<SearchRoutesViewModel.Event, SearchRoutesViewModel.State>(),
    ISearchRoutesViewModel {
    public sealed interface Event {
        public data class SetQuery internal constructor(internal val query: String) : Event
    }

    public sealed class State {
        public data object Unfiltered : State()

        public data class Results(val routeIds: List<String>) : State()

        public data object Error : State()

        public val isEmpty: Boolean
            get() =
                when (this) {
                    Unfiltered -> false
                    is Results -> routeIds.isEmpty()
                    Error -> false
                }
    }

    @Composable
    override fun runLogic(events: Flow<Event>): State {
        var query by remember { mutableStateOf("") }
        var state by remember { mutableStateOf<State>(State.Unfiltered) }

        LaunchedEffect(null) { globalRepository.getGlobalData() }

        LaunchedEffect(null) {
            events.collect { event -> if (event is Event.SetQuery) query = event.query }
        }

        LaunchedEffect(query) {
            analytics.performedRouteFilter(query)
            if (query.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    delay(500)
                    when (val data = searchResultRepository.getRouteFilterResults(query)) {
                        is ApiResult.Ok ->
                            state =
                                State.Results(
                                    data.data.routes.map {
                                        when {
                                            it.id == "Green" -> "line-Green"
                                            else -> it.id
                                        }
                                    }
                                )
                        is ApiResult.Error -> {
                            // Only set to error if there's a backend error code, otherwise this
                            // can catch exceptions from debounced LaunchedEffect cancellation
                            if (data.code != null) state = State.Error
                        }
                        null -> {}
                    }
                }
            } else {
                state = State.Unfiltered
            }
        }

        return state
    }

    override val models: StateFlow<State>
        get() = internalModels

    override fun setQuery(query: String): Unit = fireEvent(Event.SetQuery(query))
}

public class MockSearchRoutesViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: SearchRoutesViewModel.State) : ISearchRoutesViewModel {
    internal var onSetQuery = { _: String -> }
    override val models: MutableStateFlow<SearchRoutesViewModel.State> =
        MutableStateFlow(initialState)

    override fun setQuery(query: String) {
        onSetQuery(query)
    }
}
