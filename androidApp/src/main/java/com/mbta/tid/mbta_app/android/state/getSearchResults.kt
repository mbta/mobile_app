package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(kotlinx.coroutines.FlowPreview::class)
class SearchResultsViewModel(
    private val searchResultRepository: ISearchResultRepository,
) : ViewModel() {
    private var _searchResults: MutableStateFlow<SearchResults?> = MutableStateFlow(null)
    private var lastClickTime: Long? = null
    private var job: Job? = null
    val searchResults: StateFlow<SearchResults?> = _searchResults

    fun getSearchResults(query: String) {
        val currentTime = System.currentTimeMillis()
        if (lastClickTime != null && currentTime - lastClickTime!! > 500) {
            job?.cancel()
            job =
                CoroutineScope(Dispatchers.IO).launch {
                    when (val data = searchResultRepository.getSearchResults(query)) {
                        is ApiResult.Ok -> _searchResults.emit(data.data)
                        is ApiResult.Error -> _searchResults.emit(null)
                        null -> {}
                    }
                }
        } else if (lastClickTime == null) {
            job =
                CoroutineScope(Dispatchers.IO).launch {
                    when (val data = searchResultRepository.getSearchResults(query)) {
                        is ApiResult.Ok -> _searchResults.emit(data.data)
                        is ApiResult.Error -> _searchResults.emit(null)
                        null -> {}
                    }
                }
        }
        lastClickTime = currentTime
    }
}

@Composable
fun getSearchResultsVm(
    searchResultRepository: ISearchResultRepository = koinInject()
): SearchResultsViewModel {
    val viewModel = remember { SearchResultsViewModel(searchResultRepository) }
    return viewModel
}
