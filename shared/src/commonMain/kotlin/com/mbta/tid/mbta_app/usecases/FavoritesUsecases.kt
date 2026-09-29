package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.SubscriptionRequest
import com.mbta.tid.mbta_app.model.filterValidFavorites
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.IFavoritesRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.ISubscriptionsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.ShouldRefineInSwift
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

public class FavoritesUsecases(
    private val repository: IFavoritesRepository,
    private val globalRepository: IGlobalRepository,
    private val settingsRepository: ISettingsRepository,
    private val subscriptionsRepository: ISubscriptionsRepository,
    private val analytics: Analytics,
) : KoinComponent {

    private val flow = MutableStateFlow<Map<RouteStopDirection, FavoriteSettings>?>(null)
    public val state: StateFlow<Map<RouteStopDirection, FavoriteSettings>?> = flow.asStateFlow()

    private suspend fun filteredFavorites(
        favorites: Map<RouteStopDirection, FavoriteSettings>
    ): Map<RouteStopDirection, FavoriteSettings>? =
        (globalRepository.getGlobalData() as? ApiResult.Ok)?.data?.let { globalData ->
            favorites.filterValidFavorites(globalData)
        }

    public suspend fun getRouteStopDirectionFavorites(): Map<RouteStopDirection, FavoriteSettings> =
        filteredFavorites(repository.getFavorites().routeStopDirection)?.let { favorites ->
            flow.update { favorites }
            return@let favorites
        } ?: emptyMap()

    @OptIn(ExperimentalObjCRefinement::class)
    @ShouldRefineInSwift
    public suspend fun updateRouteStopDirections(
        newValues: Map<RouteStopDirection, FavoriteSettings?>,
        context: EditFavoritesContext,
        defaultDirection: Int?,
        fcmToken: String?,
        locale: String?,
    ) {
        val storedFavorites = repository.getFavorites()
        val currentFavorites = storedFavorites.routeStopDirection.toMutableMap()

        val changedFavorites =
            newValues
                .filter {
                    (it.value == null && currentFavorites.containsKey(it.key)) ||
                        (it.value != null && !(currentFavorites.containsKey(it.key)))
                }
                .mapValues { it.value != null }

        analytics.favoritesUpdated(changedFavorites, context, defaultDirection)

        newValues.forEach { (routeStopDirection, settings) ->
            if (settings != null) {
                currentFavorites[routeStopDirection] = settings
            } else {
                currentFavorites.remove(routeStopDirection)
            }
        }
        repository.setFavorites(storedFavorites.copy(routeStopDirection = currentFavorites))
        fcmToken?.let {
            val settings = settingsRepository.getSettings()
            val subs =
                SubscriptionRequest.fromFavorites(
                    filteredFavorites(currentFavorites) ?: currentFavorites,
                    includeAccessibility = settings[Settings.StationAccessibility] ?: false,
                )
            CoroutineScope(Dispatchers.IO).launch {
                subscriptionsRepository.updateSubscriptions(
                    fcmToken,
                    subs,
                    locale,
                    notificationsEnabled = settings[Settings.Notifications] ?: false,
                )
            }
        }
        getRouteStopDirectionFavorites()
    }
}

public enum class EditFavoritesContext {
    Favorites,
    StopDetails,
    RouteDetails,
}
