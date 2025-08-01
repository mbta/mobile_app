package com.mbta.tid.mbta_app.android.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.favorites.FavoritesView
import com.mbta.tid.mbta_app.android.util.managedTargetLocation
import com.mbta.tid.mbta_app.model.SheetRoutes
import com.mbta.tid.mbta_app.viewModel.IFavoritesViewModel
import com.mbta.tid.mbta_app.viewModel.IToastViewModel
import org.koin.compose.koinInject

@Composable
fun FavoritesPage(
    openSheetRoute: (SheetRoutes) -> Unit,
    favoritesViewModel: IFavoritesViewModel,
    errorBannerViewModel: ErrorBannerViewModel,
    toastViewModel: IToastViewModel = koinInject(),
    nearbyTransit: NearbyTransit,
) {
    val targetLocation by managedTargetLocation(nearbyTransit)
    FavoritesView(
        openSheetRoute,
        { nearbyTransit.lastLoadedLocation = it },
        { nearbyTransit.isTargeting = it },
        favoritesViewModel,
        errorBannerViewModel,
        toastViewModel,
        nearbyTransit.alertData,
        nearbyTransit.globalResponse,
        targetLocation,
    )
}
