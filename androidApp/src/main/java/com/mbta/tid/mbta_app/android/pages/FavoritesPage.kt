package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ActionButton
import com.mbta.tid.mbta_app.android.component.ActionButtonKind
import com.mbta.tid.mbta_app.android.component.DebugView
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.NavTextButton
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.component.routeCard.RouteCardList
import com.mbta.tid.mbta_app.android.component.stopCard.StopCardList
import com.mbta.tid.mbta_app.android.favorites.NoFavoritesView
import com.mbta.tid.mbta_app.android.favorites.NotificationsHint
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.contrastTranslucent
import com.mbta.tid.mbta_app.android.util.isRoughlyEqualTo
import com.mbta.tid.mbta_app.android.util.managedTargetLocation
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.android.util.toPosition
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.NavigationCallbacks
import com.mbta.tid.mbta_app.viewModel.FavoritesViewModel
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import com.mbta.tid.mbta_app.viewModel.IFavoritesViewModel
import com.mbta.tid.mbta_app.viewModel.IToastViewModel
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import kotlin.time.Duration.Companion.seconds
import org.koin.compose.koinInject

@Composable
fun FavoritesPage(
    openSheetRoute: (SheetRoutes) -> Unit,
    favoritesViewModel: IFavoritesViewModel,
    errorBannerViewModel: IErrorBannerViewModel,
    toastViewModel: IToastViewModel = koinInject(),
    nearbyTransit: NearbyTransit,
) {
    val alertData = nearbyTransit.alertData
    val globalResponse = nearbyTransit.globalResponse

    val now by timer(updateInterval = 5.seconds)
    val state by favoritesViewModel.models.collectAsState()

    val targetLocation by managedTargetLocation(nearbyTransit)
    val cameraStateUnthrottled by
        nearbyTransit.viewportProvider.cameraStateFlow.collectAsStateWithLifecycle(null)

    val notificationsEnabled = SettingsCache.get(Settings.Notifications)
    val groupByStop = SettingsCache.get(Settings.FavoritesByStop)

    fun onAddFavorites() {
        favoritesViewModel.setIsFirstExposureToNewFavorites(false)
        toastViewModel.hideToast()
        openSheetRoute(SheetRoutes.RoutePicker(RoutePickerPath.Root, RouteDetailsContext.Favorites))
    }

    LaunchedEffect(now) { favoritesViewModel.setNow(now) }
    LaunchedEffect(alertData) { favoritesViewModel.setAlerts(alertData) }
    LaunchedEffect(targetLocation, nearbyTransit.viewportProvider.isManuallyCentering) {
        if (!nearbyTransit.viewportProvider.isManuallyCentering) {
            favoritesViewModel.setLocation(targetLocation)
        }
    }

    LaunchedEffect(Unit) {
        favoritesViewModel.setContext(FavoritesViewModel.Context.Favorites)
        favoritesViewModel.setActive(active = true, wasSentToBackground = false)
        favoritesViewModel.reloadFavorites()
    }

    LifecycleResumeEffect(Unit) {
        favoritesViewModel.setActive(active = true, wasSentToBackground = false)
        onPauseOrDispose {
            favoritesViewModel.setActive(active = false, wasSentToBackground = true)
        }
    }

    LaunchedEffect(state.awaitingPredictionsAfterBackground) {
        errorBannerViewModel.setIsLoadingWhenPredictionsStale(
            state.awaitingPredictionsAfterBackground
        )
    }

    LaunchedEffect(targetLocation) {
        targetLocation?.let {
            cameraStateUnthrottled?.center?.let { currentPosition ->
                if (
                    it.isRoughlyEqualTo(currentPosition.toPosition()) &&
                        !nearbyTransit.viewportProvider.isManuallyCentering
                ) {
                    nearbyTransit.lastLoadedLocation = it
                    nearbyTransit.isTargeting = false
                }
            }
        }
    }

    LaunchedEffect(state.favorites, notificationsEnabled) {
        if (notificationsEnabled && state.favorites?.isEmpty() == true)
            favoritesViewModel.dismissNotificationsHint()
    }

    val toastText = stringResource(R.string.favorite_stops_first_time_toast_message)
    LaunchedEffect(state.shouldShowFirstTimeToast) {
        if (state.shouldShowFirstTimeToast) {
            toastViewModel.showToast(
                ToastViewModel.Toast(
                    toastText,
                    action =
                        ToastViewModel.ToastAction.Close(
                            onClose = {
                                favoritesViewModel.setIsFirstExposureToNewFavorites(false)
                                toastViewModel.hideToast()
                            }
                        ),
                )
            )
        }
    }

    val routeCardData = state.routeCardData
    val stopCardData = state.stopCardData

    val chosenCardData = if (groupByStop) stopCardData else routeCardData

    val emptyView: @Composable ColumnScope.() -> Unit = {
        NoFavoritesView(::onAddFavorites)
        Spacer(Modifier.weight(1f))
    }
    val isFavorite = { rsd: RouteStopDirection ->
        (state.favorites?.keys ?: emptySet()).contains(rsd)
    }
    val onOpenStopDetails = { stopId: String, filter: StopDetailsFilter? ->
        openSheetRoute(SheetRoutes.StopDetails(stopId, filter, null))
    }

    Column {
        SheetHeader(
            title = stringResource(R.string.favorites_link),
            navCallbacks =
                NavigationCallbacks(
                    onBack = null,
                    onClose = null,
                    backButtonPresentation = NavigationCallbacks.BackButtonPresentation.Floating,
                ),
            rightActionContents = {
                Row(Modifier, Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                    if (!chosenCardData.isNullOrEmpty()) {
                        ActionButton(
                            ActionButtonKind.Plus,
                            colors = ButtonDefaults.contrastTranslucent(),
                            action = ::onAddFavorites,
                        )
                        NavTextButton(stringResource(R.string.edit)) {
                            openSheetRoute(SheetRoutes.EditFavorites)
                        }
                    }
                }
            },
        )

        if (notificationsEnabled && state.shouldShowNotificationsHint) {
            NotificationsHint(
                onHintTap = {
                    openSheetRoute(SheetRoutes.EditFavorites)
                    favoritesViewModel.dismissNotificationsHint()
                },
                onHintDismiss = { favoritesViewModel.dismissNotificationsHint() },
            )
        }

        ErrorBanner(errorBannerViewModel, modifier = Modifier.padding(top = 8.dp))
        DebugView(content = {})
        if (groupByStop) {
            StopCardList(
                stopCardData = stopCardData,
                emptyView = { emptyView() },
                global = globalResponse,
                now = now,
                isFavorite = isFavorite,
                onOpenStopDetails = onOpenStopDetails,
            )
        } else {
            RouteCardList(
                routeCardData = routeCardData,
                emptyView = { emptyView() },
                global = globalResponse,
                now = now,
                isFavorite = isFavorite,
                onOpenStopDetails = onOpenStopDetails,
            )
        }
    }
}
