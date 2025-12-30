package com.mbta.tid.mbta_app.android.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.ModalRoutes
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.DirectionLabel
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.PillDecoration
import com.mbta.tid.mbta_app.android.component.RoutePill
import com.mbta.tid.mbta_app.android.component.RoutePillType
import com.mbta.tid.mbta_app.android.component.ScrollSeparatorColumn
import com.mbta.tid.mbta_app.android.component.ScrollSeparatorLazyColumn
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.component.routeCard.LoadingRouteCard
import com.mbta.tid.mbta_app.android.component.routeCard.StopSubheader
import com.mbta.tid.mbta_app.android.component.routeCard.TransitHeader
import com.mbta.tid.mbta_app.android.favorites.NoFavoritesView
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.fcmToken
import com.mbta.tid.mbta_app.android.util.getLabels
import com.mbta.tid.mbta_app.android.util.key
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.LeafFormat
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.NavigationCallbacks
import com.mbta.tid.mbta_app.viewModel.FavoritesViewModel
import com.mbta.tid.mbta_app.viewModel.IFavoritesViewModel
import com.mbta.tid.mbta_app.viewModel.IToastViewModel
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import org.koin.compose.koinInject

@Composable
fun EditFavoritesPage(
    global: GlobalResponse?,
    favoritesViewModel: IFavoritesViewModel,
    toastViewModel: IToastViewModel = koinInject(),
    navCallbacks: NavigationCallbacks,
    openModalWithCloseCallback: (ModalRoutes, () -> Unit) -> Unit,
) {
    val state by favoritesViewModel.models.collectAsState()
    val context = LocalContext.current
    fun getToastLabel(routeStopDirection: RouteStopDirection): String {
        val labels = routeStopDirection.getLabels(global, context)

        return labels?.let {
            context.getString(R.string.favorites_toast_remove, it.direction, it.route, it.stop)
        } ?: context.getString(R.string.favorites_toast_remove_fallback)
    }
    val toastUndoLabel = stringResource(R.string.undo)
    val includeAccessibility = SettingsCache.get(Settings.StationAccessibility)
    // to make deletion animations work nicely, we persist the first staticRouteCardData we saw, and
    // then we visually hide things that are no longer in the favorites
    var firstStaticRouteCardData by remember { mutableStateOf<List<RouteCardData>?>(null) }
    val firstDataFavorites =
        firstStaticRouteCardData.orEmpty().flatMap { it.routeStopDirections }.toSet()
    val currentDataFavorites =
        state.staticRouteCardData.orEmpty().flatMap { it.routeStopDirections }
    val removedFavorites = firstDataFavorites - currentDataFavorites
    // to avoid breaking creation, we reset the staticRouteCardData if anything appears that we
    // didn’t already know about
    if (
        state.staticRouteCardData != null &&
            (firstStaticRouteCardData == null ||
                !firstDataFavorites.containsAll(currentDataFavorites))
    ) {
        firstStaticRouteCardData = state.staticRouteCardData
    }

    LaunchedEffect(Unit) { favoritesViewModel.setContext(FavoritesViewModel.Context.Edit) }
    DisposableEffect(Unit) { onDispose { toastViewModel.hideToast() } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SheetHeader(
            title = stringResource(R.string.edit_favorites),
            closeText = stringResource(R.string.done),
            navCallbacks = navCallbacks,
            buttonColors = ButtonDefaults.key(),
        )
        EditFavoritesList(
            state.favorites,
            firstStaticRouteCardData,
            removedFavorites,
            global,
            deleteFavorite = { deletedFavorite ->
                val deletedSettings = state.favorites?.get(deletedFavorite) ?: FavoriteSettings()
                favoritesViewModel.updateFavorites(
                    mapOf(deletedFavorite to null),
                    EditFavoritesContext.Favorites,
                    deletedFavorite.direction,
                    fcmToken,
                    includeAccessibility,
                )

                toastViewModel.showToast(
                    ToastViewModel.Toast(
                        getToastLabel(deletedFavorite),
                        duration = ToastViewModel.Duration.Short,
                        action =
                            ToastViewModel.ToastAction.Custom(
                                actionLabel = toastUndoLabel,
                                onAction = {
                                    favoritesViewModel.updateFavorites(
                                        mapOf(deletedFavorite to deletedSettings),
                                        EditFavoritesContext.Favorites,
                                        deletedFavorite.direction,
                                        fcmToken,
                                        includeAccessibility,
                                    )
                                    toastViewModel.hideToast()
                                },
                            ),
                    )
                )
            },
            editFavorite = { selectedFavorite ->
                openModalWithCloseCallback(
                    ModalRoutes.SaveFavorite(
                        selectedFavorite.route,
                        selectedFavorite.stop,
                        selectedFavorite.direction,
                        EditFavoritesContext.Favorites,
                    )
                ) {
                    favoritesViewModel.reloadFavorites()
                }
            },
        )
    }
}

@Composable
private fun EditFavoritesList(
    favorites: Map<RouteStopDirection, FavoriteSettings>?,
    routeCardData: List<RouteCardData>?,
    removedFavorites: Set<RouteStopDirection>,
    global: GlobalResponse?,
    deleteFavorite: (RouteStopDirection) -> Unit,
    editFavorite: (RouteStopDirection) -> Unit,
) {
    val notificationsFlag = SettingsCache.get(Settings.Notifications)

    val displayedFavorites =
        routeCardData?.filter { data -> !removedFavorites.containsAll(data.routeStopDirections) }

    if (displayedFavorites == null) {
        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            ScrollSeparatorLazyColumn(
                contentPadding =
                    PaddingValues(start = 15.dp, top = 7.dp, end = 15.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(5) { LoadingRouteCard() }
                item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
            }
        }
    } else if (displayedFavorites.isEmpty()) {
        ScrollSeparatorColumn(Modifier.padding(8.dp).navigationBarsPadding(), Arrangement.Center) {
            NoFavoritesView({}, false)
        }
    } else {
        ScrollSeparatorLazyColumn(
            contentPadding = PaddingValues(start = 15.dp, top = 7.dp, end = 15.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(displayedFavorites, key = { it.id.idText }) {
                Column(Modifier.animateItem().haloContainer(1.dp)) {
                    TransitHeader(it.lineOrRoute) {}

                    it.stopData.forEach { stopData ->
                        val visible =
                            stopData.data.any { leaf ->
                                !removedFavorites.contains(leaf.routeStopDirection)
                            }
                        AnimatedVisibility(visible = visible) { StopSubheader(stopData) }

                        FavoriteDepartures(
                            favorites,
                            stopData,
                            removedFavorites,
                            global,
                            { leaf -> deleteFavorite(leaf.routeStopDirection) },
                        ) { leaf ->
                            val selectedFavorite = leaf.routeStopDirection
                            if (notificationsFlag) {
                                editFavorite(selectedFavorite)
                            } else {
                                deleteFavorite(selectedFavorite)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
    }
}

@Composable
private fun FavoriteDepartures(
    favorites: Map<RouteStopDirection, FavoriteSettings>?,
    stopData: RouteCardData.RouteStopData,
    removedFavorites: Set<RouteStopDirection>,
    globalData: GlobalResponse?,
    onDrag: (RouteCardData.Leaf) -> Unit,
    onClick: (RouteCardData.Leaf) -> Unit,
) {
    val notificationsFlag = SettingsCache.get(Settings.Notifications)

    Column {
        stopData.data.withIndex().forEach { (index, leaf) ->
            val formatted = leaf.format(EasternTimeInstant.now(), globalData)
            val direction = stopData.directions.first { it.id == leaf.directionId }
            val overriddenClickLabel = stringResource(R.string.delete)
            val visible = !removedFavorites.contains(leaf.routeStopDirection)
            AnimatedVisibility(visible = visible) {
                // frustratingly, if we retain the same state after an undo, we’ll just delete the
                // favorite again, so we need a fresh state if this was hidden and then shown again.
                // conveniently, this is as easy as remembering non-saveable
                val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
                val swipeToDismissBoxState = remember {
                    SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold)
                }

                SwipeToDismissBox(
                    state = swipeToDismissBoxState,
                    backgroundContent = {
                        if (
                            swipeToDismissBoxState.dismissDirection ==
                                SwipeToDismissBoxValue.EndToStart
                        ) {
                            Spacer(Modifier.weight(1f))
                        }
                        Column(
                            Modifier.padding(8.dp)
                                .align(Alignment.CenterVertically)
                                .clearAndSetSemantics {},
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                painterResource(R.drawable.fa_delete),
                                null,
                                modifier = Modifier.size(24.dp),
                                tint = colorResource(R.color.delete_background),
                            )
                            Text(
                                stringResource(R.string.remove),
                                color = colorResource(R.color.delete_background),
                                style = Typography.footnote,
                            )
                        }
                    },
                    Modifier.background(colorResource(R.color.delete)),
                    onDismiss = { onDrag(leaf) },
                ) {
                    @Composable
                    fun rightContent() {
                        if (notificationsFlag) {
                            NotificationStatusIcon(favorites?.get(leaf.routeStopDirection))
                            EditIcon { onClick(leaf) }
                        } else {
                            DeleteIcon(action = { onClick(leaf) })
                        }
                    }

                    Row(
                        modifier =
                            Modifier.background(colorResource(R.color.fill3))
                                .fillMaxHeight()
                                .padding(vertical = 10.dp, horizontal = 16.dp)
                                .semantics(mergeDescendants = true) {
                                    role = Role.Button
                                    onClick(overriddenClickLabel) {
                                        onClick(leaf)
                                        true
                                    }
                                }
                    ) {
                        when (formatted) {
                            is LeafFormat.Single -> {
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement =
                                        Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.semantics {},
                                    ) {
                                        DirectionLabel(
                                            direction,
                                            pillDecoration =
                                                formatted.route?.let {
                                                    PillDecoration.OnDirectionDestination(it)
                                                },
                                            modifier = Modifier.weight(1f),
                                        )
                                        rightContent()
                                    }
                                }
                            }

                            is LeafFormat.Branched -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.Start,
                                        verticalArrangement =
                                            Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        DirectionLabel(direction, showDestination = false)
                                        BranchRows(formatted)
                                    }
                                    rightContent()
                                }
                            }
                        }
                    }
                }

                if (index < stopData.data.lastIndex) {
                    HaloSeparator()
                }
            }
        }
    }
}

@Composable
private fun BranchRows(formatted: LeafFormat.Branched) {
    for (branch in formatted.branchRows) {
        val pillDecoration = branch.route?.let { PillDecoration.OnRow(it) }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (pillDecoration is PillDecoration.OnRow) {
                RoutePill(
                    pillDecoration.route,
                    line = null,
                    RoutePillType.Flex,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Text(
                branch.headsign,
                style = Typography.bodySemibold,
                modifier = Modifier.placeholderIfLoading(),
            )
        }
    }
}

@Composable
private fun DeleteIcon(action: () -> Unit) {
    Box(
        modifier =
            Modifier.size(44.dp)
                .clip(CircleShape)
                .background(colorResource(R.color.delete_background))
                .clickable { action() }
                .testTag("trashCan")
                .clearAndSetSemantics { hideFromAccessibility() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.trash_can),
            null,
            modifier = Modifier.size(16.dp),
            tint = colorResource(R.color.delete),
        )
    }
}

@Composable
private fun EditIcon(action: () -> Unit) {
    Box(
        modifier =
            Modifier.size(44.dp)
                .clip(CircleShape)
                .background(colorResource(R.color.halo))
                .clickable { action() }
                .testTag("editIcon"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.fa_pencil),
            stringResource(R.string.edit_favorites),
            modifier = Modifier.size(24.dp),
            tint = colorResource(R.color.text),
        )
    }
}

@Composable
private fun NotificationStatusIcon(favorite: FavoriteSettings?) {
    if (favorite?.notifications?.enabled != true) return

    Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        Icon(
            painterResource(R.drawable.fa_bell_filled),
            stringResource(R.string.notifications_enabled),
            modifier = Modifier.size(18.dp),
            tint = colorResource(R.color.text),
        )
    }
}
