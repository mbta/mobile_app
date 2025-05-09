package com.mbta.tid.mbta_app.android.state

import android.util.Log
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.history.Visit
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.StopResult
import com.mbta.tid.mbta_app.model.StopResultRoute
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.FlowPreview::class)
class SearchResultsViewModel(
    private val analytics: Analytics,
    private val searchResultRepository: ISearchResultRepository,
    private val visitHistoryUsecase: VisitHistoryUsecase,
) : ViewModel() {
    private var _searchResults: MutableStateFlow<SearchResults?> = MutableStateFlow(null)
    private var job: Job? = null
    val searchResults: StateFlow<SearchResults?> = _searchResults
    var expanded = false

    fun getSearchResults(query: String, globalResponse: GlobalResponse?) {
        analytics.performedSearch(query)
        job?.cancel()
        job =
            CoroutineScope(Dispatchers.IO).launch {
                if (query.isNotEmpty()) {
                    delay(500)
                    when (val data = searchResultRepository.getSearchResults(query)) {
                        is ApiResult.Ok -> _searchResults.emit(data.data)
                        is ApiResult.Error -> {
                            Log.e("SearchResultsViewModel", "getSearchResults failed: $data")
                            _searchResults.emit(null)
                        }
                        null -> {}
                    }
                } else {
                    val latestVisits = visitHistoryUsecase.getLatestVisits()

                    _searchResults.emit(
                        SearchResults(
                            stops =
                                latestVisits.mapIndexedNotNull { index, visit ->
                                    mapLatestVisitsToResult(index, visit, globalResponse)
                                },
                            routes = emptyList(),
                        )
                    )
                }
            }
    }

    private fun mapLatestVisitsToResult(
        index: Int,
        visit: Visit,
        globalResponse: GlobalResponse?,
    ): StopResult? {
        if (visit is Visit.StopVisit && globalResponse != null) {
            val stop = globalResponse.getStop(visit.stopId)
            if (stop == null) {
                return null
            }

            val routes = globalResponse.getTypicalRoutesFor(stop.id)
            val isStation = stop.locationType == LocationType.STATION
            return StopResult(
                id = stop.id,
                name = stop.name,
                isStation = isStation,
                routes = routes.map { StopResultRoute(type = it.type, icon = "") },
                rank = index,
                zone = null,
            )
        } else {
            return null
        }
    }
}
