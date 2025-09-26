package com.mbta.tid.mbta_app.android.pages

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.FavoriteConfirmation
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.getLabels
import com.mbta.tid.mbta_app.android.util.manageFavorites
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.viewModel.IToastViewModel
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SaveFavoritePage(
    routeId: String,
    stopId: String,
    selectedDirection: Int,
    context: EditFavoritesContext,
    goBack: () -> Unit,
    toastViewModel: IToastViewModel = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    val localContext = LocalContext.current

    val global = getGlobalData("SaveFavoritePage")
    val lineOrRoute = global?.getLineOrRoute(routeId)
    val stop = global?.getStop(stopId)

    val (favorites, updateFavorites) = manageFavorites()

    if (global == null || lineOrRoute == null || stop == null) {
        Text("Loading")
        return
    }
    val allPatternsForStop = global.getPatternsFor(stop.id, lineOrRoute)
    val stopDirections =
        lineOrRoute.directions(global, stop, allPatternsForStop.filter { it.isTypical() })

    fun isFavorite(rsd: RouteStopDirection) = favorites?.contains(rsd) ?: false

    fun updateAndToast(update: Map<RouteStopDirection, FavoriteSettings?>) {
        coroutineScope.launch {
            updateFavorites(update, context, selectedDirection)
            val favorited = update.filter { it.value != null }
            val firstFavorite = favorited.entries.firstOrNull() ?: return@launch
            val labels = firstFavorite.key.getLabels(global, localContext)
            var toastText: String? = null

            // If there's only a single favorite, show direction, route, and stop in the toast
            if (favorited.size == 1) {
                toastText =
                    labels?.let {
                        localContext.getString(
                            R.string.favorites_toast_add,
                            it.direction,
                            it.route,
                            it.stop,
                        )
                    } ?: localContext.getString(R.string.favorites_toast_add_fallback)
            }
            // If there are two favorites and they both have the same route and stop, omit direction
            else if (
                favorited.size == 2 &&
                    favorited.keys.all {
                        it.route == firstFavorite.key.route && it.stop == firstFavorite.key.stop
                    }
            ) {
                toastText =
                    labels?.let {
                        localContext.getString(
                            R.string.favorites_toast_add_multi,
                            it.route,
                            it.stop,
                        )
                    } ?: localContext.getString(R.string.favorites_toast_add_fallback)
            }

            toastText?.let {
                toastViewModel.showToast(
                    ToastViewModel.Toast(it, duration = ToastViewModel.Duration.Short)
                )
            }
        }
    }

    FavoriteConfirmation(
        lineOrRoute,
        stop,
        stopDirections,
        selectedDirection,
        context,
        proposedFavorites =
            stopDirections.associateBy({ it.id }) {
                // if selectedDirection and already a favorite, then removing favorite.
                // if not selected direction and already a favorite, then keep it.
                val isSelected = it.id == selectedDirection
                val isExistingFavorite =
                    isFavorite(RouteStopDirection(lineOrRoute.id, stop.id, it.id))
                val onlyOppositeDirectionServed =
                    stopDirections.size == 1 && it.id != selectedDirection
                val suggestingFavorite =
                    (isSelected xor isExistingFavorite) ||
                        // If the only direction is the opposite one, mark it as favorite
                        // whether or not it already is
                        onlyOppositeDirectionServed
                if (suggestingFavorite) FavoriteSettings() else null
            },
        updateFavorites = ::updateAndToast,
        onClose = goBack,
    )
}
