package com.mbta.tid.mbta_app.android.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavBackStackEntry
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.search.results.StopResultsView
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.state.getSearchResultsVm

@ExperimentalMaterial3Api
@Composable
fun SearchBarOverlay(
    onStopNavigation: (stopId: String) -> Unit,
    currentNavEntry: NavBackStackEntry?,
    inputFieldFocusRequester: FocusRequester,
    content: @Composable () -> Unit
) {
    var visible =
        remember(currentNavEntry) {
            currentNavEntry?.arguments?.getString("stopId")?.isBlank() ?: true
        }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var searchInputState by rememberSaveable { mutableStateOf("") }
    val globalResponse = getGlobalData()
    val searchResultsVm = getSearchResultsVm(globalResponse = globalResponse)
    val searchResults = searchResultsVm.searchResults.collectAsState(initial = null).value

    val buttonColors =
        ButtonColors(
            containerColor = colorResource(R.color.fill3),
            disabledContainerColor = colorResource(R.color.fill3),
            contentColor = colorResource(R.color.deemphasized),
            disabledContentColor = colorResource(R.color.deemphasized),
        )
    LaunchedEffect(searchInputState, expanded) {
        searchResultsVm.getSearchResults(searchInputState)
    }

    Box(contentAlignment = Alignment.TopCenter) {
        Box(
            modifier =
                Modifier.absoluteOffset {
                        if (expanded) IntOffset(0, 0) else IntOffset(0, 12.dp.roundToPx())
                    }
                    .zIndex(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (visible) {
                Box(
                    modifier =
                        Modifier.absoluteOffset(y = 3.5.dp)
                            .height(64.dp)
                            .width(364.dp)
                            .border(2.dp, colorResource(R.color.halo), RoundedCornerShape(10.dp))
                )
                SearchBar(
                    shape = RoundedCornerShape(10.dp),
                    colors =
                        SearchBarDefaults.colors(containerColor = colorResource(R.color.fill3)),
                    inputField = {
                        SearchBarDefaults.InputField(
                            colors =
                                SearchBarDefaults.inputFieldColors(
                                    focusedTextColor = colorResource(R.color.deemphasized),
                                    unfocusedTextColor = colorResource(R.color.deemphasized),
                                    focusedPlaceholderColor = colorResource(R.color.deemphasized),
                                    unfocusedPlaceholderColor = colorResource(R.color.deemphasized),
                                ),
                            query = searchInputState,
                            placeholder = { Text(stringResource(R.string.search_by_stop)) },
                            expanded = expanded,
                            onQueryChange = { searchInputState = it },
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.focusRequester(inputFieldFocusRequester),
                            onSearch = {},
                            leadingIcon = {
                                Icon(
                                    painterResource(R.drawable.magnifying_glass),
                                    "Search",
                                    tint = colorResource(R.color.deemphasized)
                                )
                            },
                            trailingIcon = {
                                if (expanded) {
                                    Button(
                                        colors = buttonColors,
                                        onClick = { expanded = false },
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.fa_xmark),
                                            "Voice Search",
                                            tint = colorResource(R.color.deemphasized)
                                        )
                                    }
                                }
                            }
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = {},
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().background(colorResource(R.color.fill2)),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        if (searchInputState.isEmpty()) {
                            item {
                                Text(
                                    modifier = Modifier.padding(bottom = 10.dp),
                                    text = "Recently Visited",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        items(searchResults?.stops ?: emptyList()) { stop ->
                            val shape =
                                if (searchResults?.stops?.first() == stop) {
                                    RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                                } else if (searchResults?.stops?.last() == stop) {
                                    RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                                } else {
                                    RoundedCornerShape(0.dp)
                                }
                            StopResultsView(shape, stop, globalResponse, onStopNavigation)
                        }
                    }
                }
            }
        }

        content()
    }
}
