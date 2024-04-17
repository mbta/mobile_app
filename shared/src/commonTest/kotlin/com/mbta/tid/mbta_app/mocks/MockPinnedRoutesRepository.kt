package com.mbta.tid.mbta_app.mocks

import com.mbta.tid.mbta_app.repositories.PinnedRoutesRepository

class MockPinnedRoutesRepository : PinnedRoutesRepository {
    private var pinnedRoutes: Set<String> = emptySet()

    override suspend fun getPinnedRoutes(): Set<String> {
        return pinnedRoutes
    }

    override suspend fun setPinnedRoutes(routes: Set<String>) {
        pinnedRoutes = routes
    }
}
