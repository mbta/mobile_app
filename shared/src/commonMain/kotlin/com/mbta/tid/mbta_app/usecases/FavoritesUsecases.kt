package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.repositories.IFavoritesRepository
import org.koin.core.component.KoinComponent

class FavoritesUsecases(private val repository: IFavoritesRepository) : KoinComponent {

    suspend fun getRouteStopDirectionFavorites(): Set<RouteStopDirection> {
        val storedFavorites = repository.getFavorites()
        return storedFavorites.routeStopDirection ?: emptySet()
    }

    // Boolean return value indicates saved state
    suspend fun toggleRouteStopDirection(route: String, stop: String, direction: Int): Boolean {
        val routeStopDirection = RouteStopDirection(route, stop, direction)
        return toggleRouteStopDirection(routeStopDirection)
    }

    suspend fun updateRouteStopDirections(newValues: Map<RouteStopDirection, Boolean>) {
        val storedFavorites = repository.getFavorites()
        val currentFavorites = (storedFavorites.routeStopDirection ?: emptySet()).toMutableSet()

        newValues.forEach { (routeStopDirection, isFavorite) ->
            if (isFavorite) {
                currentFavorites.add(routeStopDirection)
            } else {
                currentFavorites.remove(routeStopDirection)
            }
        }
        repository.setFavorites(storedFavorites.copy(routeStopDirection = currentFavorites))
    }

    // Boolean return value indicates saved state
    suspend fun toggleRouteStopDirection(routeStopDirection: RouteStopDirection): Boolean {
        val storedFavorites = repository.getFavorites()
        val currentFavorites = (storedFavorites.routeStopDirection ?: emptySet()).toMutableSet()
        val containsFavorite = currentFavorites.contains(routeStopDirection)
        if (containsFavorite) {
            currentFavorites.remove(routeStopDirection)
        } else {
            currentFavorites.add(routeStopDirection)
        }
        repository.setFavorites(storedFavorites.copy(routeStopDirection = currentFavorites))
        return !containsFavorite
    }

    suspend fun addRouteStopDirections(routeStopDirections: Set<RouteStopDirection>) {
        val storedFavorites = repository.getFavorites()
        val currentFavorites = (storedFavorites.routeStopDirection ?: emptySet()).toMutableSet()
        currentFavorites.addAll(routeStopDirections)
        repository.setFavorites(storedFavorites.copy(routeStopDirection = currentFavorites))
    }

    suspend fun removeRouteStopDirections(routeStopDirections: Set<RouteStopDirection>) {
        val storedFavorites = repository.getFavorites()
        val currentFavorites = (storedFavorites.routeStopDirection ?: emptySet()).toMutableSet()
        currentFavorites.removeAll(routeStopDirections)
        repository.setFavorites(storedFavorites.copy(routeStopDirection = currentFavorites))
    }
}
