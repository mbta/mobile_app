package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.Favorites
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.SubscriptionRequest
import com.mbta.tid.mbta_app.repositories.IFavoritesRepository
import com.mbta.tid.mbta_app.repositories.ISubscriptionsRepository
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.ShouldRefineInSwift
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

public class FavoritesUsecases(
    private val repository: IFavoritesRepository,
    private val subscriptionsRepository: ISubscriptionsRepository,
    private val ioDispatcher: CoroutineDispatcher,
    private val analytics: Analytics,
) : KoinComponent {
    private val flow = MutableStateFlow<Map<RouteStopDirection, FavoriteSettings>>(emptyMap())
    public val state: StateFlow<Map<RouteStopDirection, FavoriteSettings>> = flow.asStateFlow()

    init {
        CoroutineScope(ioDispatcher).launch {
            repository.state
                .map { it?.routeStopDirection }
                .collect { flow.value = it ?: emptyMap() }
        }
    }

    public suspend fun getRouteStopDirectionFavorites(): Map<RouteStopDirection, FavoriteSettings> {
        val storedFavorites = repository.getFavorites()
        //        flow.update { storedFavorites.routeStopDirection }
        return storedFavorites.routeStopDirection
    }

    @OptIn(ExperimentalObjCRefinement::class)
    @ShouldRefineInSwift
    public suspend fun updateRouteStopDirections(
        newValues: Map<RouteStopDirection, FavoriteSettings?>,
        context: EditFavoritesContext,
        defaultDirection: Int,
        fcmToken: String?,
        includeAccessibility: Boolean,
    ) {
        //        println("~~~ usecase update")
        val storedFavorites = repository.state.value ?: Favorites(emptyMap())
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
                currentFavorites.put(routeStopDirection, settings)
            } else {
                currentFavorites.remove(routeStopDirection)
            }
        }
        repository.setFavorites(storedFavorites.copy(routeStopDirection = currentFavorites))
        fcmToken?.let {
            val subs = SubscriptionRequest.fromFavorites(currentFavorites, includeAccessibility)
            subscriptionsRepository.updateSubscriptions(it, subs)
        }
        //        getRouteStopDirectionFavorites()
    }
}

public enum class EditFavoritesContext {
    Favorites,
    StopDetails,
    RouteDetails,
}
