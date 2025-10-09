package com.mbta.tid.mbta_app.android.routeDetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.component.SaveFavoritesFlow
import com.mbta.tid.mbta_app.android.component.ScrollSeparatorColumn
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.component.StarButton
import com.mbta.tid.mbta_app.android.component.StopListContext
import com.mbta.tid.mbta_app.android.component.StopListRow
import com.mbta.tid.mbta_app.android.component.StopPlacement
import com.mbta.tid.mbta_app.android.state.getRouteStops
import com.mbta.tid.mbta_app.android.stopDetails.DirectionPicker
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.contrastTranslucent
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.labelWithModeIfBus
import com.mbta.tid.mbta_app.android.util.manageFavorites
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.android.util.modifiers.loadingShimmer
import com.mbta.tid.mbta_app.android.util.rememberSuspend
import com.mbta.tid.mbta_app.android.util.stateJsonSaver
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.LoadingPlaceholders
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteBranchSegment
import com.mbta.tid.mbta_app.model.RouteDetailsStopList
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.silverRoutes
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.utils.TestData
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import com.mbta.tid.mbta_app.viewModel.IToastViewModel
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.koin.dsl.module

sealed class RouteDetailsRowContext {
    data class Details(val stop: Stop) : RouteDetailsRowContext()

    data class Favorites(val isFavorited: Boolean, val onTapStar: () -> Unit) :
        RouteDetailsRowContext()
}

@Composable
fun RouteStopListView(
    lineOrRoute: LineOrRoute,
    context: RouteDetailsContext,
    globalData: GlobalResponse,
    onClick: (RouteDetailsRowContext) -> Unit,
    onClickLabel: @Composable (RouteDetailsRowContext) -> String? = { null },
    onBack: () -> Unit,
    onClose: () -> Unit,
    openModal: (ModalRoutes) -> Unit,
    errorBannerViewModel: IErrorBannerViewModel,
    toastViewModel: IToastViewModel = koinInject(),
    defaultSelectedRouteId: Route.Id? = null,
    rightSideContent: @Composable RowScope.(RouteDetailsRowContext, Modifier) -> Unit,
) {
    val routes = lineOrRoute.allRoutes.sorted()
    val routeIds = routes.map { it.id }
    val parameters =
        remember(lineOrRoute, globalData) {
            RouteDetailsStopList.RouteParameters(lineOrRoute, globalData)
        }
    var selectedDirection by rememberSaveable {
        mutableIntStateOf(parameters.availableDirections.firstOrNull() ?: 0)
    }
    var selectedRouteId by
        rememberSaveable(saver = stateJsonSaver()) {
            mutableStateOf(defaultSelectedRouteId ?: routeIds.first())
        }

    val routeStops =
        getRouteStops(selectedRouteId, selectedDirection, "RouteDetailsView.routeStopIds")

    val stopList =
        rememberSuspend(selectedRouteId, selectedDirection, routeStops, globalData) {
            RouteDetailsStopList.fromPieces(
                selectedRouteId,
                selectedDirection,
                routeStops,
                globalData,
            )
        }

    RouteStopListView(
        lineOrRoute = lineOrRoute,
        parameters = parameters,
        selectedDirection = selectedDirection,
        setDirection = { selectedDirection = it },
        selectedRouteId = selectedRouteId,
        setRouteId = { selectedRouteId = it },
        routes = routes,
        stopList = stopList,
        context = context,
        globalData = globalData,
        onClick = onClick,
        onClickLabel = onClickLabel,
        onBack = onBack,
        onClose = onClose,
        openModal = openModal,
        errorBannerViewModel = errorBannerViewModel,
        toastViewModel = toastViewModel,
        rightSideContent = rightSideContent,
    )
}

@Composable
fun LoadingRouteStopListView(
    context: RouteDetailsContext,
    errorBannerViewModel: IErrorBannerViewModel,
    toastViewModel: IToastViewModel = koinInject(),
) {
    CompositionLocalProvider(IsLoadingSheetContents provides true) {
        val objects = ObjectCollectionBuilder()
        val mockRoute = LineOrRoute.Route(objects.route {})

        RouteStopListView(
            lineOrRoute = mockRoute,
            parameters = RouteDetailsStopList.RouteParameters(mockRoute, GlobalResponse(objects)),
            selectedDirection = 0,
            setDirection = {},
            selectedRouteId = mockRoute.id,
            setRouteId = {},
            routes = listOf(mockRoute.route),
            stopList = RouteDetailsStopList(0, listOf()),
            context = context,
            globalData = GlobalResponse(objects),
            onClick = {},
            onClickLabel = { null },
            onBack = {},
            onClose = {},
            openModal = {},
            errorBannerViewModel = errorBannerViewModel,
            toastViewModel = toastViewModel,
            rightSideContent = { rowContext, modifier ->
                when (rowContext) {
                    is RouteDetailsRowContext.Details ->
                        Image(
                            painterResource(id = R.drawable.baseline_chevron_right_24),
                            contentDescription = null,
                            modifier = modifier.width(8.dp),
                            colorFilter = ColorFilter.tint(colorResource(R.color.deemphasized)),
                        )
                    is RouteDetailsRowContext.Favorites ->
                        StarButton(
                            rowContext.isFavorited,
                            colorResource(R.color.text),
                            rowContext.onTapStar,
                        )
                }
            },
        )
    }
}

@Composable
fun RouteStopListView(
    lineOrRoute: LineOrRoute,
    parameters: RouteDetailsStopList.RouteParameters,
    selectedDirection: Int,
    setDirection: (Int) -> Unit,
    selectedRouteId: Route.Id,
    setRouteId: (Route.Id) -> Unit,
    routes: List<Route>,
    stopList: RouteDetailsStopList?,
    context: RouteDetailsContext,
    globalData: GlobalResponse,
    onClick: (RouteDetailsRowContext) -> Unit,
    onClickLabel: @Composable (RouteDetailsRowContext) -> String? = { null },
    onBack: () -> Unit,
    onClose: () -> Unit,
    openModal: (ModalRoutes) -> Unit,
    errorBannerViewModel: IErrorBannerViewModel,
    toastViewModel: IToastViewModel = koinInject(),
    rightSideContent: @Composable RowScope.(RouteDetailsRowContext, Modifier) -> Unit,
) {
    val (favorites, updateFavorites) = manageFavorites()

    fun isFavorite(routeStopDirection: RouteStopDirection): Boolean =
        favorites?.contains(routeStopDirection) ?: false

    val coroutineScope = rememberCoroutineScope()
    fun confirmFavorites(updatedValues: Map<RouteStopDirection, FavoriteSettings?>) {
        coroutineScope.launch {
            updateFavorites(
                updatedValues,
                if (context == RouteDetailsContext.Favorites) EditFavoritesContext.Favorites
                else EditFavoritesContext.RouteDetails,
                selectedDirection,
            )
        }
    }

    val firstTimeToastMessage = stringResource(R.string.tap_favorites_hint)

    var showFirstTimeFavoritesToast by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var firstTimeToast by remember { mutableStateOf<ToastViewModel.Toast?>(null) }
    val displayedToast by
        remember(toastViewModel) {
                toastViewModel.models.map {
                    when (it) {
                        is ToastViewModel.State.Hidden -> null
                        is ToastViewModel.State.Visible -> it.toast
                    }
                }
            }
            .collectAsState(null)

    LaunchedEffect(context, favorites) {
        // If favorites have not been loaded, we don't know whether or not to show the toast,
        // and if the bool has already been set, we want to keep that value
        if (favorites == null || showFirstTimeFavoritesToast != null) return@LaunchedEffect
        showFirstTimeFavoritesToast =
            context is RouteDetailsContext.Favorites && favorites.isEmpty()
    }

    LaunchedEffect(showFirstTimeFavoritesToast) {
        if (showFirstTimeFavoritesToast == true) {
            val toast =
                ToastViewModel.Toast(
                    message = firstTimeToastMessage,
                    action =
                        ToastViewModel.ToastAction.Close(
                            onClose = { showFirstTimeFavoritesToast = false }
                        ),
                    isTip = true,
                )
            toastViewModel.showToast(toast)
            firstTimeToast = toast
        } else if (showFirstTimeFavoritesToast == false && displayedToast == firstTimeToast) {
            toastViewModel.hideToast()
        }
    }

    var showFavoritesStopConfirmationId by rememberSaveable { mutableStateOf<String?>(null) }
    val showFavoritesStopConfirmation = globalData.getStop(showFavoritesStopConfirmationId)

    fun stopRowContext(stop: Stop) =
        when (context) {
            is RouteDetailsContext.Details -> RouteDetailsRowContext.Details(stop)
            is RouteDetailsContext.Favorites ->
                RouteDetailsRowContext.Favorites(
                    isFavorited =
                        isFavorite(RouteStopDirection(lineOrRoute.id, stop.id, selectedDirection)),
                    onTapStar = { showFavoritesStopConfirmationId = stop.id },
                )
        }

    showFavoritesStopConfirmation?.let { stop ->
        val allPatternsForStop = globalData.getPatternsFor(stop.id, lineOrRoute)
        val stopDirections =
            lineOrRoute.directions(globalData, stop, allPatternsForStop.filter { it.isTypical() })
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            SaveFavoritesFlow(
                lineOrRoute,
                stop,
                stopDirections.filter {
                    it.id in parameters.availableDirections &&
                        !stop.isLastStopForAllPatterns(it.id, allPatternsForStop, globalData)
                },
                selectedDirection = selectedDirection,
                context = EditFavoritesContext.Favorites,
                isFavorite = ::isFavorite,
                updateFavorites = ::confirmFavorites,
                onClose = { showFavoritesStopConfirmationId = null },
                openModal = openModal,
            )
        }
    }

    Column {
        SheetHeader(
            title = lineOrRoute.name,
            titleContentDescription = lineOrRoute.labelWithModeIfBus(LocalContext.current),
            titleColor = Color.fromHex(lineOrRoute.textColor),
            closeText =
                if (context is RouteDetailsContext.Favorites) stringResource(R.string.done)
                else null,
            onBack = {
                showFirstTimeFavoritesToast = false
                onBack()
            },
            onClose = {
                showFirstTimeFavoritesToast = false
                onClose()
            },
            buttonColors = ButtonDefaults.contrastTranslucent(),
        )
        ErrorBanner(errorBannerViewModel)

        DirectionPicker(
            parameters.availableDirections,
            parameters.directions,
            lineOrRoute.sortRoute,
            selectedDirection,
            updateDirectionId = { setDirection(it) },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp).padding(top = 2.dp),
        )

        if (lineOrRoute is LineOrRoute.Line && routes.size > 1) {
            LineRoutePicker(lineOrRoute.line, routes, selectedRouteId, selectedDirection) {
                setRouteId(it)
            }
        }

        RouteStops(
            lineOrRoute,
            stopList,
            selectedDirection,
            context,
            onTapStop = onClick,
            onClickLabel = onClickLabel,
            stopRowContext = ::stopRowContext,
            rightSideContent = rightSideContent,
        )
    }
}

@Composable
private fun RouteStops(
    lineOrRoute: LineOrRoute,
    stopList: RouteDetailsStopList?,
    selectedDirection: Int,
    context: RouteDetailsContext,
    onTapStop: (RouteDetailsRowContext) -> Unit,
    onClickLabel: @Composable (RouteDetailsRowContext) -> String?,
    stopRowContext: (Stop) -> RouteDetailsRowContext,
    rightSideContent: @Composable RowScope.(RouteDetailsRowContext, Modifier) -> Unit,
    loading: Boolean = false,
) {
    if (stopList == null || stopList.directionId != selectedDirection) {
        LoadingRouteStops(lineOrRoute, selectedDirection, context, rightSideContent)
        return
    }

    val haloColor =
        if (lineOrRoute.type == RouteType.BUS && !silverRoutes.contains(lineOrRoute.id))
            colorResource(R.color.halo_light)
        else colorResource(R.color.halo_dark)

    ScrollSeparatorColumn(
        Modifier.padding(horizontal = 14.dp)
            .padding(top = 8.dp, bottom = 40.dp)
            .haloContainer(
                2.dp,
                outlineColor = haloColor,
                backgroundColor = colorResource(R.color.fill2),
            )
            .then(if (loading) Modifier.loadingShimmer() else Modifier),
        haloColor = haloColor,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
        scrollEnabled = !loading,
    ) {
        val hasTypicalSegment = stopList.segments.any { it.isTypical }

        stopList.segments.forEachIndexed { segmentIndex, segment ->
            if (segment.isTypical || !hasTypicalSegment) {
                segment.stops.forEachIndexed { stopIndex, stop ->
                    val stopPlacement =
                        StopPlacement(
                            isFirst = segmentIndex == 0 && stopIndex == 0,
                            isLast =
                                segmentIndex == stopList.segments.lastIndex &&
                                    stopIndex == segment.stops.lastIndex,
                        )

                    val stopRowContext = stopRowContext(stop.stop)
                    StopListRow(
                        stop.stop,
                        stop.stopLane,
                        stop.stickConnections,
                        onClick = { onTapStop(stopRowContext) },
                        routeAccents = TripRouteAccents(lineOrRoute.sortRoute),
                        stopListContext = StopListContext.RouteDetails,
                        modifier = Modifier.minimumInteractiveComponentSize().fillMaxWidth(),
                        connectingRoutes = stop.connectingRoutes,
                        onClickLabel = onClickLabel(stopRowContext),
                        stopPlacement = stopPlacement,
                        rightSideContent = { modifier ->
                            rightSideContent(stopRowContext, modifier)
                        },
                    )
                }
            } else {
                CollapsableStopList(
                    lineOrRoute,
                    segment,
                    onClick = { onTapStop(stopRowContext(it.stop)) },
                    onClickLabel = { onClickLabel(stopRowContext(it.stop)) },
                    segmentIndex == 0,
                    segmentIndex == stopList.segments.lastIndex,
                ) { stop, modifier ->
                    rightSideContent(stopRowContext(stop.stop), modifier)
                }
            }
        }
    }
}

@Composable
private fun LineRoutePicker(
    line: Line,
    routes: List<Route>,
    selectedRouteId: Route.Id,
    selectedDirection: Int,
    onSelect: (Route.Id) -> Unit,
) {
    val backgroundColor =
        colorResource(R.color.route_color_contrast).compositeOver(Color.fromHex(line.color))

    Column(
        Modifier.padding(horizontal = 14.dp)
            .padding(top = 6.dp, bottom = 4.dp)
            .haloContainer(1.dp, backgroundColor, backgroundColor)
    ) {
        for (route in routes) {
            val selected = route.id == selectedRouteId
            val haloColor = if (selected) colorResource(R.color.halo) else Color.Transparent
            val rowColor = if (selected) colorResource(R.color.fill3) else Color.Transparent
            Tab(
                selected = selected,
                onClick = { onSelect(route.id) },
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .haloContainer(1.dp, haloColor, rowColor, 7.dp)
                        .padding(8.dp),
                selectedContentColor = colorResource(R.color.text),
                unselectedContentColor = Color.fromHex(line.textColor),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoutePill(route, line, RoutePillType.Fixed)
                    Text(
                        route.directionDestinations[selectedDirection] ?: "",
                        style = Typography.title3Semibold,
                    )
                    Spacer(Modifier.weight(1F))
                }
            }
        }
    }
}

@Composable
private fun LoadingRouteStops(
    lineOrRoute: LineOrRoute,
    selectedDirection: Int,
    context: RouteDetailsContext,
    rightSideContent: @Composable RowScope.(RouteDetailsRowContext, Modifier) -> Unit,
) {
    val loadingStops =
        remember(lineOrRoute, selectedDirection) {
            LoadingPlaceholders.routeDetailsStops(lineOrRoute, selectedDirection)
        }
    CompositionLocalProvider(IsLoadingSheetContents provides true) {
        RouteStops(
            lineOrRoute,
            stopList = loadingStops,
            selectedDirection = selectedDirection,
            context = context,
            onTapStop = { _ -> },
            onClickLabel = { null },
            stopRowContext = { stop ->
                when (context) {
                    is RouteDetailsContext.Details -> RouteDetailsRowContext.Details(stop)
                    is RouteDetailsContext.Favorites ->
                        RouteDetailsRowContext.Favorites(isFavorited = false, onTapStar = {})
                }
            },
            rightSideContent = rightSideContent,
            loading = true,
        )
    }
}

@Preview
@Composable
private fun RouteStopsPreview() {
    startKoin {
        modules(module { single<SettingsCache> { SettingsCache(MockSettingsRepository()) } })
    }
    RouteStops(
        LineOrRoute.Route(TestData.getRoute("Red")),
        RouteDetailsStopList(
            0,
            listOf(
                RouteDetailsStopList.Segment(
                    listOf(
                        RouteDetailsStopList.Entry(
                            TestData.getStop("place-alfcl"),
                            RouteBranchSegment.Lane.Center,
                            RouteBranchSegment.StickConnection.forward(
                                null,
                                "place-alfcl",
                                "place-jfk",
                                RouteBranchSegment.Lane.Center,
                            ),
                            emptyList(),
                        ),
                        RouteDetailsStopList.Entry(
                            TestData.getStop("place-jfk"),
                            RouteBranchSegment.Lane.Center,
                            listOf(
                                RouteBranchSegment.StickConnection(
                                    fromStop = "place-alfcl",
                                    fromLane = RouteBranchSegment.Lane.Center,
                                    fromVPos = RouteBranchSegment.VPos.Top,
                                    toStop = "place-jfk",
                                    toLane = RouteBranchSegment.Lane.Center,
                                    toVPos = RouteBranchSegment.VPos.Center,
                                ),
                                RouteBranchSegment.StickConnection(
                                    fromStop = "place-jfk",
                                    fromLane = RouteBranchSegment.Lane.Center,
                                    fromVPos = RouteBranchSegment.VPos.Center,
                                    toStop = "place-asmnl",
                                    toLane = RouteBranchSegment.Lane.Left,
                                    toVPos = RouteBranchSegment.VPos.Bottom,
                                ),
                                RouteBranchSegment.StickConnection(
                                    fromStop = "place-jfk",
                                    fromLane = RouteBranchSegment.Lane.Center,
                                    fromVPos = RouteBranchSegment.VPos.Center,
                                    toStop = "place-brntn",
                                    toLane = RouteBranchSegment.Lane.Right,
                                    toVPos = RouteBranchSegment.VPos.Bottom,
                                ),
                            ),
                            emptyList(),
                        ),
                    ),
                    isTypical = true,
                ),
                RouteDetailsStopList.Segment(
                    listOf(
                        RouteDetailsStopList.Entry(
                            TestData.getStop("place-asmnl"),
                            RouteBranchSegment.Lane.Left,
                            RouteBranchSegment.StickConnection.forward(
                                "place-jfk",
                                "place-asmnl",
                                null,
                                RouteBranchSegment.Lane.Left,
                            ) +
                                RouteBranchSegment.StickConnection.forward(
                                    "place-jfk",
                                    null,
                                    "place-brntn",
                                    RouteBranchSegment.Lane.Right,
                                ),
                            emptyList(),
                        )
                    ),
                    isTypical = true,
                ),
                RouteDetailsStopList.Segment(
                    listOf(
                        RouteDetailsStopList.Entry(
                            TestData.getStop("place-brntn"),
                            RouteBranchSegment.Lane.Right,
                            RouteBranchSegment.StickConnection.forward(
                                "place-jfk",
                                "place-brntn",
                                null,
                                RouteBranchSegment.Lane.Right,
                            ),
                            emptyList(),
                        )
                    ),
                    isTypical = true,
                ),
            ),
        ),
        selectedDirection = 0,
        RouteDetailsContext.Details,
        onTapStop = {},
        onClickLabel = { null },
        stopRowContext = { RouteDetailsRowContext.Details(it) },
        rightSideContent = { _, _ -> },
    )
}
