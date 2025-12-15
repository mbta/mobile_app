package com.mbta.tid.mbta_app.android.routePicker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.ErrorCard
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.ScrollSeparatorColumn
import com.mbta.tid.mbta_app.android.component.SearchInput
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.contrastTranslucent
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.android.util.typeText
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath.Bus.routeType
import com.mbta.tid.mbta_app.utils.NavigationCallbacks
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import com.mbta.tid.mbta_app.viewModel.ISearchRoutesViewModel
import com.mbta.tid.mbta_app.viewModel.SearchRoutesViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun RoutePickerView(
    path: RoutePickerPath,
    context: RouteDetailsContext,
    onOpenPickerPath: (RoutePickerPath, RouteDetailsContext) -> Unit,
    onOpenRouteDetails: (LineOrRoute.Id, RouteDetailsContext) -> Unit,
    onRouteSearchExpandedChange: (Boolean) -> Unit,
    navCallbacks: NavigationCallbacks,
    errorBannerViewModel: IErrorBannerViewModel,
    searchRoutesViewModel: ISearchRoutesViewModel = koinInject(),
) {
    val globalData = getGlobalData("RoutePickerView")
    var searchInputState by rememberSaveable { mutableStateOf("") }
    var searchInputFocused by rememberSaveable { mutableStateOf(false) }
    val searchVMState by searchRoutesViewModel.models.collectAsState()
    val searchFocusRequester = remember { FocusRequester() }
    val routeScroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { searchInputState = "" }
    LaunchedEffect(searchInputState) { searchRoutesViewModel.setQuery(searchInputState) }
    LaunchedEffect(path) { searchRoutesViewModel.setPath(path) }

    if (globalData == null) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = "Loading" })
        return
    }

    val modes =
        listOf(
            RoutePickerPath.Bus,
            RoutePickerPath.Silver,
            RoutePickerPath.CommuterRail,
            RoutePickerPath.Ferry,
        )
    val routes = remember(globalData, path) { globalData.getRoutesForPicker(path) }

    val headerTitle =
        when (path) {
            is RoutePickerPath.Root ->
                when (context) {
                    is RouteDetailsContext.Favorites ->
                        stringResource(R.string.route_picker_header_favorites)

                    is RouteDetailsContext.Details -> TODO("Implement details header")
                }

            is RoutePickerPath.Bus -> stringResource(R.string.bus)
            is RoutePickerPath.Silver -> "Silver Line"
            is RoutePickerPath.CommuterRail -> "Commuter Rail"
            is RoutePickerPath.Ferry -> stringResource(R.string.ferry)
        }

    Column(Modifier.fillMaxWidth(), Arrangement.Top, Alignment.CenterHorizontally) {
        SheetHeader(
            title = headerTitle,
            titleColor = path.textColor,
            closeText = stringResource(R.string.done),
            navCallbacks =
                navCallbacks.copy(
                    onBack = navCallbacks.onBack.takeUnless { path == RoutePickerPath.Root }
                ),
            buttonColors = ButtonDefaults.contrastTranslucent(),
        )
        ErrorBanner(errorBannerViewModel, Modifier.padding(start = 14.dp, top = 6.dp, end = 14.dp))
        AnimatedVisibility(searchVMState is SearchRoutesViewModel.State.Error) {
            ErrorCard(
                Modifier.padding(bottom = 16.dp),
                details = {
                    Text(
                        stringResource(R.string.error_loading_data),
                        style = Typography.subheadline,
                    )
                },
            )
        }

        if (path != RoutePickerPath.Root) {
            SearchInput(
                searchInputState,
                { searchInputState = it },
                searchInputFocused,
                {
                    if (!it) {
                        searchInputState = ""
                    }
                    onRouteSearchExpandedChange(it)
                    searchInputFocused = it
                },
                searchFocusRequester,
                modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
                haloColor = path.haloColor,
            ) {
                Text(stringResource(R.string.filter_routes), style = Typography.callout)
            }
        }
        ScrollSeparatorColumn(
            Modifier.navigationBarsPadding()
                .imePadding()
                .padding(start = 14.dp, top = 10.dp, end = 14.dp, bottom = 16.dp),
            Arrangement.spacedBy(4.dp),
            haloColor = path.haloColor,
            scrollState = routeScroll,
        ) {
            if (path == RoutePickerPath.Root) {
                for (mode in modes) {
                    RoutePickerRootRow(mode) { onOpenPickerPath(mode, context) }
                }
                Text(
                    stringResource(R.string.subway),
                    style = Typography.subheadlineSemibold,
                    modifier =
                        Modifier.padding(start = 16.dp, top = 22.dp, bottom = 2.dp).semantics {
                            heading()
                        },
                )
                for (route in routes) {
                    RoutePickerRootRow(route) { onOpenRouteDetails(route.id, context) }
                }
            } else {
                val displayedRoutes =
                    remember(routes, searchVMState) {
                        when (val state = searchVMState) {
                            is SearchRoutesViewModel.State.Unfiltered,
                            is SearchRoutesViewModel.State.Error -> routes

                            is SearchRoutesViewModel.State.Results -> {
                                state.routeIds.mapNotNull {
                                    routes.firstOrNull { route -> route.id == it }
                                }
                            }
                        }
                    }

                LaunchedEffect(displayedRoutes) { scope.launch { routeScroll.animateScrollTo(0) } }
                if (displayedRoutes.isNotEmpty())
                    Column(
                        Modifier.padding(bottom = 14.dp)
                            .haloContainer(2.dp, outlineColor = path.haloColor)
                    ) {
                        for ((index, route) in displayedRoutes.withIndex()) {
                            RoutePickerRow(route) { onOpenRouteDetails(route.id, context) }
                            if (index < displayedRoutes.lastIndex) {
                                HaloSeparator()
                            }
                        }
                    }
                if (searchVMState is SearchRoutesViewModel.State.Results)
                    Column(
                        Modifier.padding(top = 6.dp).fillMaxWidth(),
                        Arrangement.spacedBy(2.dp),
                        Alignment.CenterHorizontally,
                    ) {
                        if (displayedRoutes.isEmpty())
                            Text(
                                stringResource(
                                    R.string.no_matching_routes,
                                    path.routeType.typeText(LocalContext.current, true),
                                ),
                                color = path.textColor,
                                style = Typography.bodySemibold,
                            )
                        Text(
                            stringResource(R.string.find_stops_hint),
                            color = path.textColor,
                            style = Typography.body,
                        )
                    }
            }
        }
    }
}
