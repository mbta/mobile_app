package com.mbta.tid.mbta_app.repositories

interface PinnedRoutesRepository {
    suspend fun getPinnedRoutes(): Set<String>

    suspend fun setPinnedRoutes(routes: Set<String>)
}
