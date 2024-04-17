package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.repositories.PinnedRoutesRepository

class TogglePinnedRouteUsecase(private val repository: PinnedRoutesRepository) {

    suspend fun execute(route: String) {
        val currentRoutes = repository.getPinnedRoutes().toMutableSet()
        if (currentRoutes.contains(route)) {
            currentRoutes.remove(route)
        } else {
            currentRoutes.add(route)
        }
        repository.setPinnedRoutes(currentRoutes)
    }
}
