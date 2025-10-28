package com.mbta.tid.mbta_app.android.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ActionButton
import com.mbta.tid.mbta_app.android.component.ActionButtonKind
import com.mbta.tid.mbta_app.android.component.ErrorBanner
import com.mbta.tid.mbta_app.android.component.NavTextButton
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.component.routeCard.RouteCardList
import com.mbta.tid.mbta_app.android.util.contrastTranslucent
import com.mbta.tid.mbta_app.android.util.timer
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.NavigationCallbacks
import com.mbta.tid.mbta_app.viewModel.FavoritesViewModel
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import com.mbta.tid.mbta_app.viewModel.IFavoritesViewModel
import com.mbta.tid.mbta_app.viewModel.IToastViewModel
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds

@Composable
fun FavoritesView(
    openSheetRoute: (SheetRoutes) -> Unit,
    setLastLocation: (Position) -> Unit,
    setIsTargeting: (Boolean) -> Unit,
    favoritesViewModel: IFavoritesViewModel,
    errorBannerViewModel: IErrorBannerViewModel,
    toastViewModel: IToastViewModel,
    alertData: AlertsStreamDataResponse?,
    globalResponse: GlobalResponse?,
    targetLocation: Position?,
) {
    val now by timer(updateInterval = 5.seconds)
    val state by favoritesViewModel.models.collectAsState()
    val context = LocalContext.current

    fun onAddFavorites() {
        favoritesViewModel.setIsFirstExposureToNewFavorites(false)
        toastViewModel.hideToast()
        openSheetRoute(SheetRoutes.RoutePicker(RoutePickerPath.Root, RouteDetailsContext.Favorites))
    }

    LaunchedEffect(now) { favoritesViewModel.setNow(now) }
    LaunchedEffect(alertData) { favoritesViewModel.setAlerts(alertData) }
    LaunchedEffect(targetLocation) { favoritesViewModel.setLocation(targetLocation) }

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

    LaunchedEffect(state.loadedLocation) {
        state.loadedLocation?.let { setLastLocation(it) }
        setIsTargeting(false)
    }
    LaunchedEffect(state.shouldShowFirstTimeToast) {
        if (state.shouldShowFirstTimeToast) {
            toastViewModel.showToast(
                ToastViewModel.Toast(
                    context.getString(R.string.favorite_stops_first_time_toast_message),
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

    Column() {
        SheetHeader(
            title = stringResource(R.string.favorites_link),
            navCallbacks =
                NavigationCallbacks(
                    onBack = null,
                    onClose = null,
                    sheetBackState = NavigationCallbacks.SheetBackState.Hidden,
                ),
            rightActionContents = {
                Row(Modifier, Arrangement.spacedBy(16.dp), Alignment.CenterVertically) {
                    if (!routeCardData.isNullOrEmpty()) {
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

        ErrorBanner(errorBannerViewModel, modifier = Modifier.padding(top = 8.dp))
        RouteCardList(
            routeCardData = routeCardData,
            emptyView = {
                NoFavoritesView(::onAddFavorites)
                Spacer(Modifier.weight(1f))
            },
            global = globalResponse,
            now = now,
            isFavorite = { rsd -> (state.favorites?.keys ?: emptySet()).contains(rsd) },
            onOpenStopDetails = { stopId, filter ->
                openSheetRoute(SheetRoutes.StopDetails(stopId, filter, null))
            },
        )
    }
}
