package com.mbta.tid.mbta_app.android.search

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.min
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.search.results.StopResultsView
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.state.getSearchResultsVm
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer

@ExperimentalMaterial3Api
@Composable
fun SearchBarOverlay(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onStopNavigation: (stopId: String) -> Unit,
    currentNavEntry: SheetRoutes?,
    inputFieldFocusRequester: FocusRequester,
    content: @Composable () -> Unit
) {
    var visible =
        remember(currentNavEntry) {
            currentNavEntry?.let { it is SheetRoutes.NearbyTransit } ?: true
        }
    var searchInputState by rememberSaveable { mutableStateOf("") }
    val globalResponse = getGlobalData("SearchBar.getGlobalData")
    val searchResultsVm = getSearchResultsVm(globalResponse = globalResponse)
    val searchResults = searchResultsVm.searchResults.collectAsState(initial = null).value

    var searchBarHeight by rememberSaveable { mutableStateOf(0) }

    val buttonColors =
        ButtonColors(
            containerColor = colorResource(R.color.fill3),
            disabledContainerColor = colorResource(R.color.fill3),
            contentColor = colorResource(R.color.deemphasized),
            disabledContentColor = colorResource(R.color.deemphasized),
        )
    LaunchedEffect(searchInputState, visible) {
        Log.i("KB", "Launching search with input: ${searchInputState}")
        searchResultsVm.getSearchResults(searchInputState)
    }
    LaunchedEffect(visible, expanded) {
        if (visible) {
            onExpandedChange(expanded)
            if (!expanded) {
                searchInputState = ""
            }
        }
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
                        if (expanded) {
                            Modifier
                        } else {
                            Modifier.padding(horizontal = 14.dp)
                        }
                ) {
                    if (!expanded) {

                        // Workaround for adding border
                        Box(
                            modifier =
                                Modifier.offset(y = 6.dp)
                                    .haloContainer(
                                        2.dp,
                                        borderRadius = 10.dp,
                                        backgroundColor = Color.Transparent
                                    )
                                    .fillMaxWidth()
                                    .height(
                                        with(LocalDensity.current) {
                                            androidx.compose.ui.unit.min(
                                                (searchBarHeight).toDp() - 8.dp,
                                                64.dp
                                            )
                                        }
                                    )
                        ) {}
                    }

                    Column(
                        modifier =
                            Modifier.padding(
                                    horizontal =
                                        if (expanded) {
                                            0.dp
                                        } else {
                                            2.dp
                                        }
                                )
                                .onSizeChanged {
                                    if (!expanded) {
                                        Log.i("KB", "SIZE CHANGED ${it.height}")

                                        searchBarHeight = it.height
                                    }
                                }
                    ) {
                        SearchBar(
                            shape = RoundedCornerShape(10.dp),
                            colors =
                                SearchBarDefaults.colors(
                                    containerColor = colorResource(R.color.fill3)
                                ),
                            inputField = {
                                SearchBarDefaults.InputField(
                                    colors =
                                        SearchBarDefaults.inputFieldColors(
                                            focusedTextColor = colorResource(R.color.text),
                                            unfocusedTextColor = colorResource(R.color.text),
                                            focusedPlaceholderColor =
                                                colorResource(R.color.deemphasized),
                                            unfocusedPlaceholderColor =
                                                colorResource(R.color.deemphasized),
                                        ),
                                    query = searchInputState,
                                    placeholder = {
                                        Text(
                                            stringResource(R.string.stops),
                                            // This will be drawn in bodyLarge if we don't
                                            // re-override it
                                            // here
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    expanded = expanded,
                                    onQueryChange = { searchInputState = it },
                                    onExpandedChange = onExpandedChange,
                                    modifier = Modifier.focusRequester(inputFieldFocusRequester),
                                    onSearch = {},
                                    leadingIcon = {
                                        Icon(
                                            painterResource(R.drawable.magnifying_glass),
                                            null,
                                            tint = colorResource(R.color.deemphasized)
                                        )
                                    },
                                    trailingIcon = {
                                        if (expanded) {
                                            Button(
                                                colors = buttonColors,
                                                onClick = { onExpandedChange(false) },
                                            ) {
                                                Icon(
                                                    painterResource(R.drawable.fa_xmark),
                                                    stringResource(R.string.close_button_label),
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
                                modifier =
                                    Modifier.fillMaxSize().background(colorResource(R.color.fill1)),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                if (searchInputState.isEmpty()) {
                                    item {
                                        Text(
                                            modifier = Modifier.padding(bottom = 10.dp),
                                            text = stringResource(R.string.recently_viewed),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                itemsIndexed(searchResults?.stops ?: emptyList()) { index, stop ->
                                    val shape =
                                        if (searchResults?.stops?.size == 1) {
                                            RoundedCornerShape(10.dp)
                                        } else if (searchResults?.stops?.first() == stop) {
                                            RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                                        } else if (searchResults?.stops?.last() == stop) {
                                            RoundedCornerShape(
                                                bottomStart = 10.dp,
                                                bottomEnd = 10.dp
                                            )
                                        } else {
                                            RoundedCornerShape(0.dp)
                                        }
                                    if (index != 0) {
                                        HorizontalDivider(color = colorResource(R.color.fill1))
                                    }
                                    StopResultsView(shape, stop, globalResponse, onStopNavigation)
                                }
                            }
                        }
                    }
                }
            }
        }

        content()
    }
}
