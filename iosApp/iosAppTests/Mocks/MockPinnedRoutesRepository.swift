//
//  MockPinnedRoutesRepository.swift
//  iosAppTests
//
//  Created by Brandon Rodriguez on 4/17/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared

class MockPinnedRoutesRepository: IPinnedRoutesRepository {
    var pinnedRoutes: Set<String> = []

    func __getPinnedRoutes() async throws -> Set<String> {
        pinnedRoutes
    }

    func __setPinnedRoutes(routes: Set<String>) async throws {
        pinnedRoutes = routes
    }
}
