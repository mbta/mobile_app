package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SearchRoutesViewModel(
    private val analytics: Analytics,
    private val globalRepository: IGlobalRepository,
    private val searchResultRepository: ISearchResultRepository,
) : MoleculeViewModel<SearchRoutesViewModel.Event, SearchRoutesViewModel.State>() {
    sealed interface Event {
        data class SetQuery(val query: String) : Event
    }

    sealed class State {
        data object Unfiltered : State()

        data class Results(val routeIds: List<String>) : State()

        data object Error : State()

        val isEmpty: Boolean
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

    val models
        get() = internalModels

    fun setQuery(query: String) = fireEvent(Event.SetQuery(query))
}
