package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.mbta.tid.mbta_app.android.component.routeCard.RouteCardContainer
import com.mbta.tid.mbta_app.android.favorites.NoFavoritesView
import com.mbta.tid.mbta_app.android.util.IsLoadingSheetContents
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.getLabels
import com.mbta.tid.mbta_app.android.util.key
import com.mbta.tid.mbta_app.android.util.manageFavorites
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.LeafFormat
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
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
    onClose: () -> Unit,
    openModal: (ModalRoutes) -> Unit,
) {
    val notificationsFlag = SettingsCache.get(Settings.Notifications)
    val state by favoritesViewModel.models.collectAsState()
    val context = LocalContext.current
    fun getToastLabel(routeStopDirection: RouteStopDirection): String {
        val labels = routeStopDirection.getLabels(global, context)

        return labels?.let {
            context.getString(R.string.favorites_toast_remove, it.direction, it.route, it.stop)
        } ?: context.getString(R.string.favorites_toast_remove_fallback)
    }
    val toastUndoLabel = stringResource(R.string.undo)

    LaunchedEffect(Unit) { favoritesViewModel.setContext(FavoritesViewModel.Context.Edit) }
    DisposableEffect(Unit) { onDispose { toastViewModel.hideToast() } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SheetHeader(
            title = stringResource(R.string.edit_favorites),
            closeText = stringResource(R.string.done),
            buttonColors = ButtonDefaults.key(),
            onClose = onClose,
        )
        EditFavoritesList(
            state.staticRouteCardData,
            global,
            deleteFavorite = { deletedFavorite ->
                val deletedSettings = state.favorites?.get(deletedFavorite) ?: FavoriteSettings()
                favoritesViewModel.updateFavorites(
                    mapOf(deletedFavorite to null),
                    EditFavoritesContext.Favorites,
                    deletedFavorite.direction,
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
                                    )
                                    toastViewModel.hideToast()
                                },
                            ),
                    )
                )
            },
            editFavorite = { selectedFavorite ->
                openModal(
                    ModalRoutes.SaveFavorite(
                        selectedFavorite.route,
                        selectedFavorite.stop,
                        selectedFavorite.direction,
                        EditFavoritesContext.Favorites,
                    )
                )
            },
        )
    }
}

@Composable
private fun EditFavoritesList(
    routeCardData: List<RouteCardData>?,
    global: GlobalResponse?,
    deleteFavorite: (RouteStopDirection) -> Unit,
    editFavorite: (RouteStopDirection) -> Unit,
) {
    val notificationsFlag = SettingsCache.get(Settings.Notifications)
    if (routeCardData == null) {
        CompositionLocalProvider(IsLoadingSheetContents provides true) {
            ScrollSeparatorLazyColumn(
                contentPadding =
                    PaddingValues(start = 15.dp, top = 7.dp, end = 15.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(5) { LoadingRouteCard() }
            }
        }
    } else if (routeCardData.isEmpty()) {
        ScrollSeparatorColumn(Modifier.padding(8.dp), Arrangement.Center) {
            NoFavoritesView({}, false)
        }
    } else {
        ScrollSeparatorLazyColumn(
            contentPadding = PaddingValues(start = 15.dp, top = 7.dp, end = 15.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(routeCardData, key = { it.id.idText }) {
                RouteCardContainer(
                    modifier = Modifier.animateItem(),
                    data = it,
                    showStopHeader = true,
                ) { stopData ->
                    FavoriteDepartures(stopData, global) { leaf ->
                        val selectedFavorite =
                            RouteStopDirection(leaf.lineOrRoute.id, leaf.stop.id, leaf.directionId)
                        if (notificationsFlag) {
                            editFavorite(selectedFavorite)
                        } else {
                            deleteFavorite(selectedFavorite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteDepartures(
    stopData: RouteCardData.RouteStopData,
    globalData: GlobalResponse?,
    onClick: (RouteCardData.Leaf) -> Unit,
) {
    val notificationsFlag = SettingsCache.get(Settings.Notifications)
    Column {
        stopData.data.withIndex().forEach { (index, leaf) ->
            val formatted = leaf.format(EasternTimeInstant.now(), globalData)
            val direction = stopData.directions.first { it.id == leaf.directionId }
            val overriddenClickLabel = stringResource(R.string.delete)

            Row(
                modifier =
                    Modifier.padding(vertical = 10.dp, horizontal = 16.dp).semantics(
                        mergeDescendants = true
                    ) {
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
                                modifier = Modifier.semantics() {},
                            ) {
                                DirectionLabel(
                                    direction,
                                    pillDecoration =
                                        formatted.route?.let {
                                            PillDecoration.OnDirectionDestination(it)
                                        },
                                    modifier = Modifier.weight(1f),
                                )
                                if (notificationsFlag) {
                                    NotificationStatusIcon(leaf)
                                    EditIcon { onClick(leaf) }
                                } else {
                                    DeleteIcon(action = { onClick(leaf) })
                                }
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
                            if (notificationsFlag) {
                                NotificationStatusIcon(leaf)
                                EditIcon { onClick(leaf) }
                            } else {
                                DeleteIcon(action = { onClick(leaf) })
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
                .testTag("editIcon")
                .clearAndSetSemantics { hideFromAccessibility() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.fa_pencil),
            null,
            modifier = Modifier.size(24.dp),
            tint = colorResource(R.color.text),
        )
    }
}

@Composable
private fun NotificationStatusIcon(leaf: RouteCardData.Leaf) {
    val routeStopDirection = RouteStopDirection(leaf.lineOrRoute.id, leaf.stop.id, leaf.directionId)
    val (favorites, _) = manageFavorites()
    val favorite = favorites?.get(routeStopDirection)
    Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        if (favorite?.notifications?.enabled == true) {
            Icon(
                painterResource(R.drawable.fa_bell_filled),
                null,
                modifier = Modifier.size(18.dp),
                tint = colorResource(R.color.text),
            )
        } else {
            Icon(
                painterResource(R.drawable.fa_bell_slash),
                null,
                modifier = Modifier.size(18.dp),
                tint = colorResource(R.color.text),
            )
        }
    }
}
