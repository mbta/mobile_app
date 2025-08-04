package com.mbta.tid.mbta_app.android.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.SearchInput
import com.mbta.tid.mbta_app.android.search.results.SearchResultsView
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.viewModel.SearchViewModel
import org.koin.androidx.compose.koinViewModel

@ExperimentalMaterial3Api
@Composable
fun SearchBarOverlay(
    expanded: Boolean,
    showSearchBar: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onStopNavigation: (stopId: String) -> Unit,
    onRouteNavigation: (routeId: String) -> Unit,
    inputFieldFocusRequester: FocusRequester,
    searchVM: SearchViewModel = koinViewModel(),
    onBarGloballyPositioned: (LayoutCoordinates) -> Unit = {},
    content: @Composable () -> Unit,
) {
    var searchInputState by rememberSaveable { mutableStateOf("") }
    val searchVMState by searchVM.models.collectAsState()
    var previouslyExpanded: Boolean by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(searchInputState) { searchVM.setQuery(searchInputState) }
    LaunchedEffect(showSearchBar, expanded) {
        if (showSearchBar) {
            searchVM.refreshHistory()
            if (expanded != previouslyExpanded) {
                // don't call onExpandedChange on initial load
                onExpandedChange(expanded)
                previouslyExpanded = expanded
            }
            if (!expanded) {
                searchInputState = ""
            }
        }
    }

    Box(contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.imePadding().zIndex(1f), contentAlignment = Alignment.Center) {
            if (showSearchBar) {
                SearchBar(
                    shape = RoundedCornerShape(10.dp),
                    colors =
                        SearchBarDefaults.colors(
                            containerColor =
                                if (expanded) colorResource(R.color.fill1) else Color.Transparent
                        ),
                    inputField = {
                        SearchInput(
                            searchInputState,
                            { searchInputState = it },
                            expanded,
                            onExpandedChange,
                            inputFieldFocusRequester,
                            onBarGloballyPositioned = onBarGloballyPositioned,
                        ) {
                            Text(stringResource(R.string.stops), style = Typography.callout)
                        }
                    },
                    expanded = expanded,
                    onExpandedChange = onExpandedChange,
                ) {
                    SearchResultsView(
                        state = searchVMState,
                        handleStopTap = onStopNavigation,
                        handleRouteTap = onRouteNavigation,
                    )
                }
            }
        }

        content()
    }
}
