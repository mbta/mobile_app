//
//  FavoritesModifierTests.swift
//  iosApp
//
//  Created by esimon on 9/4/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class FavoritesModifierTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testLoadsFromRepo() throws {
        let repoExp = expectation(description: "favorites loaded from repo")
        let setExp = expectation(description: "favorites binding was set")

        let updatedFavorites = buildFavorites {
            $0.routeStopDirection(route: Route.Id("route"), stop: "stop", direction: 0)
        }

        let favoritesBinding = Binding(get: { Favorites(routeStopDirection: [:]) }, set: {
            guard $0 == updatedFavorites else { return }
            setExp.fulfill()
        })
        let loadingBinding = Binding(get: { true }, set: { _ in })

        var repoFulfilled = false
        let mockRepos = MockRepositories()
        mockRepos.favorites = MockFavoritesRepository(
            favorites: updatedFavorites,
            onGet: {
                guard !repoFulfilled else { return }
                repoFulfilled = true
                repoExp.fulfill()
            }
        )
        loadKoinMocks(repositories: mockRepos)

        let sut = Text("test").favorites(favoritesBinding, awaitingUpdate: loadingBinding)

        ViewHosting.host(view: sut)

        wait(for: [repoExp, setExp], timeout: 1)
    }

    func testSetsLoadingToFalseWhenDone() throws {
        let loadExp = expectation(description: "favorites loaded binding set")

        let favoritesBinding = Binding(get: { Favorites(routeStopDirection: [:]) }, set: { _ in })
        let loadingBinding = Binding(get: { true }, set: {
            XCTAssertFalse($0)
            loadExp.fulfill()
        })

        let sut = Text("test").favorites(favoritesBinding, awaitingUpdate: loadingBinding)

        ViewHosting.host(view: sut)

        wait(for: [loadExp], timeout: 1)
    }

    func testSetsPreviouslyLoadedValueWhileFetching() throws {
        let setExp = expectation(description: "favorites binding set to previously loaded value")

        let previouslyLoaded = buildFavorites {
            $0.routeStopDirection(route: Route.Id("route1"), stop: "stop1", direction: 0)
        }
        LoadedFavorites.last = previouslyLoaded

        let favoritesBinding = Binding(get: { Favorites(routeStopDirection: [:]) }, set: { setFavorites in
            if setFavorites == previouslyLoaded {
                setExp.fulfill()
            }
        })
        let loadingBinding = Binding(get: { true }, set: { _ in })

        let sut = Text("test").favorites(favoritesBinding, awaitingUpdate: loadingBinding)

        ViewHosting.host(view: sut)

        wait(for: [setExp], timeout: 1)
    }
}
