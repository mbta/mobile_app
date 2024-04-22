package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import org.koin.core.component.KoinComponent

class TogglePinnedRouteUsecase(private val repository: IPinnedRoutesRepository) : KoinComponent {

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
