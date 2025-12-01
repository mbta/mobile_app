//
//  FcmSubscriptionModifierTests.swift
//  iosApp
//
//  Created by esimon on 12/1/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class FcmSubscriptionModifierTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testUpdatesSubscriptions() throws {
        let updateExp = expectation(description: "subscriptions updated")

        let objects = ObjectCollectionBuilder()

        let route = objects.route { _ in }
        let stop = objects.stop { _ in }

        let favorites = Favorites(routeStopDirection: [
            .init(route: route.id, stop: stop.id, direction: 1):
                .init(notifications: .init(enabled: true, windows: [.init(
                    startTime: .init(hour: 8, minute: 0, second: 0, nanosecond: 0),
                    endTime: .init(hour: 9, minute: 0, second: 0, nanosecond: 0),
                    daysOfWeek: [.saturday, .sunday]
                )])),
        ])

        LoadedFavorites.last = favorites
        let expectedSubscriptions = SubscriptionRequest.companion.fromFavorites(
            favorites: favorites.routeStopDirection,
            includeAccessibility: true
        )
        let expectedToken = "token_string"

        let mockRepos = MockRepositories()
        mockRepos.subscriptions = MockSubscriptionsRepository(
            onUpdateSubscriptions: { token, subs in
                XCTAssertEqual(expectedToken, token)
                XCTAssertEqual(expectedSubscriptions, subs)
                updateExp.fulfill()
            },
            onUpdateAccessibility: { _, _ in }
        )
        loadKoinMocks(repositories: mockRepos)

        let sut = Text("test").handleFcmTokenSubscriptions(
            fcmToken: expectedToken,
            includeAccessibility: true,
            notificationsEnabled: true,
        )

        ViewHosting.host(view: sut)

        wait(for: [updateExp], timeout: 1)
    }

    func testDoesNothingWithNoToken() throws {
        let updateExp = expectation(description: "subscriptions updated")
        updateExp.isInverted = true

        let objects = ObjectCollectionBuilder()

        let route = objects.route { _ in }
        let stop = objects.stop { _ in }

        let favorites = Favorites(routeStopDirection: [
            .init(route: route.id, stop: stop.id, direction: 1):
                .init(notifications: .init(enabled: true, windows: [.init(
                    startTime: .init(hour: 8, minute: 0, second: 0, nanosecond: 0),
                    endTime: .init(hour: 9, minute: 0, second: 0, nanosecond: 0),
                    daysOfWeek: [.saturday, .sunday]
                )])),
        ])

        LoadedFavorites.last = favorites

        let mockRepos = MockRepositories()
        mockRepos.subscriptions = MockSubscriptionsRepository(
            onUpdateSubscriptions: { _, _ in
                XCTFail("Should not update when token is missing")
                updateExp.fulfill()
            },
            onUpdateAccessibility: { _, _ in }
        )
        loadKoinMocks(repositories: mockRepos)

        let sut = Text("test").handleFcmTokenSubscriptions(
            fcmToken: nil,
            includeAccessibility: true,
            notificationsEnabled: true,
        )

        ViewHosting.host(view: sut)

        wait(for: [updateExp], timeout: 1)
    }
}
