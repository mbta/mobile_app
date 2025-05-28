package com.mbta.tid.mbta_app.android.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.android.SheetRoutes
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.favorites.FavoritesView
import com.mbta.tid.mbta_app.android.favorites.FavoritesViewModel
import com.mbta.tid.mbta_app.android.util.toPosition
import io.github.dellisd.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.debounce

@Composable
fun FavoritesPage(
    modifier: Modifier = Modifier,
    openSheetRoute: (SheetRoutes) -> Unit,
    favoritesViewModel: FavoritesViewModel,
    errorBannerViewModel: ErrorBannerViewModel,
    nearbyTransit: NearbyTransit,
) {
    var targetLocation by remember { mutableStateOf<Position?>(null) }
    LaunchedEffect(nearbyTransit.locationDataManager) {
        nearbyTransit.locationDataManager.currentLocation.collect { location ->
            if (
                nearbyTransit.viewportProvider.isFollowingPuck &&
                    !nearbyTransit.viewportProvider.isManuallyCentering
            ) {
                targetLocation = location?.let { Position(it.longitude, it.latitude) }
            }
        }
    }
    LaunchedEffect(nearbyTransit.viewportProvider) {
        nearbyTransit.viewportProvider.cameraStateFlow.debounce(0.5.seconds).collect {
            // since this LaunchedEffect is cancelled when not on the favorites page
            // we don't need to check
            if (!nearbyTransit.viewportProvider.isFollowingPuck) {
                targetLocation = it.center.toPosition()
            }
        }
    }
    LaunchedEffect(nearbyTransit.viewportProvider.isManuallyCentering) {
        if (nearbyTransit.viewportProvider.isManuallyCentering) {
            targetLocation = null
        }
    }
    LaunchedEffect(nearbyTransit.viewportProvider.isFollowingPuck) {
        if (nearbyTransit.viewportProvider.isFollowingPuck) {
            targetLocation =
                nearbyTransit.locationDataManager.currentLocation.value?.let {
                    Position(it.longitude, it.latitude)
                }
        }
    }
    FavoritesView(
        modifier,
        openSheetRoute,
        favoritesViewModel,
        errorBannerViewModel,
        nearbyTransit.alertData,
        nearbyTransit.globalResponse,
        targetLocation,
    )
}
