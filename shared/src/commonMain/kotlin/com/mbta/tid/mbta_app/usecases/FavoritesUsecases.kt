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
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
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

    private fun breadcrumb(message: String) {
        Sentry.addBreadcrumb(Breadcrumb(message = "FavoritesUsecases: $message"))
    }

    private suspend fun filteredFavorites(
        favorites: Map<RouteStopDirection, FavoriteSettings>,
        skipWithoutGlobal: Boolean = false,
    ): Map<RouteStopDirection, FavoriteSettings>? {
        val cachedGlobal = globalRepository.state.value
        val globalData =
            if (cachedGlobal != null) {
                breadcrumb("using cached global data")
                cachedGlobal
            } else {
                if (skipWithoutGlobal) {
                    breadcrumb("no cached global data, skipping filter")
                    return favorites
                }
                breadcrumb("no cached global data, fetching")
                val result = globalRepository.getGlobalData()
                breadcrumb("global data fetch finished: ${result::class.simpleName}")
                (result as? ApiResult.Ok)?.data
            }
        return globalData?.let { favorites.filterValidFavorites(it) }
    }

    /**
     * Filters with already-loaded global data or returns unfiltered favorites if global data is not
     * yet available.
     */
    public suspend fun getRouteStopDirectionFavorites(): Map<RouteStopDirection, FavoriteSettings> {
        breadcrumb("reading stored favorites")
        val stored = repository.getFavorites().routeStopDirection
        val favorites = filteredFavorites(stored, skipWithoutGlobal = true) ?: stored
        flow.update { favorites }
        return favorites
    }

    @OptIn(ExperimentalObjCRefinement::class)
    @ShouldRefineInSwift
    public suspend fun updateRouteStopDirections(
        newValues: Map<RouteStopDirection, FavoriteSettings?>,
        context: EditFavoritesContext,
        defaultDirection: Int?,
        fcmToken: String?,
        locale: String?,
    ) {
        breadcrumb("updating favorites: reading stored favorites")
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
        breadcrumb("updating favorites: writing ${currentFavorites.size} favorites")
        repository.setFavorites(storedFavorites.copy(routeStopDirection = currentFavorites))
        fcmToken?.let {
            breadcrumb("updating favorites: reading settings")
            val settings = settingsRepository.getSettings()
            CoroutineScope(Dispatchers.IO).launch {
                val subs =
                    SubscriptionRequest.fromFavorites(
                        filteredFavorites(currentFavorites) ?: currentFavorites,
                        includeAccessibility = settings[Settings.StationAccessibility] ?: false,
                    )
                breadcrumb("updating subscriptions")
                subscriptionsRepository.updateSubscriptions(
                    fcmToken,
                    subs,
                    locale,
                    notificationsEnabled = settings[Settings.Notifications] ?: false,
                )
                breadcrumb("updated subscriptions")
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
