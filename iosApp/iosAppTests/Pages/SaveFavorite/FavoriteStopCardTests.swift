//
//  FavoriteStopCardTests.swift
//  iosApp
//
//  Created by esimon on 11/19/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class FavoriteStopCardTests: XCTestCase {
    @MainActor func testRoutePill() {
        let objects = TestData.clone()

        let route = objects.getRoute(id: "Red")
        let stop = objects.getStop(id: "place-pktrm")

        let sut = FavoriteStopCard(
            lineOrRoute: LineOrRoute.Route(route: route),
            stop: stop,
            direction: .init(name: "", destination: "", id: 0),
            toggleDirection: nil,
        )

        XCTAssertNotNil(try? sut.inspect().find(text: "RL"))
    }

    @MainActor func testDirectionLabel() {
        let objects = TestData.clone()

        let route = objects.getRoute(id: "Red")
        let stop = objects.getStop(id: "place-pktrm")

        let sut = FavoriteStopCard(
            lineOrRoute: LineOrRoute.Route(route: route),
            stop: stop,
            direction: .init(name: "North", destination: "Alewife", id: 1),
            toggleDirection: nil,
        )

        XCTAssertNotNil(try? sut.inspect().find(text: "Northbound to"))
        XCTAssertNotNil(try? sut.inspect().find(text: "Alewife"))
    }

    @MainActor func testStopLabel() {
        let objects = TestData.clone()

        let route = objects.getRoute(id: "15")
        let stop = objects.getStop(id: "1432")

        let sut = FavoriteStopCard(
            lineOrRoute: LineOrRoute.Route(route: route),
            stop: stop,
            direction: .init(name: "Inbound", destination: "University Park", id: 1),
            toggleDirection: nil,
        )

        XCTAssertNotNil(try? sut.inspect().find(imageName: "map-stop-close-BUS"))
        XCTAssertNotNil(try? sut.inspect().find(text: "Arsenal St @ Irving St"))
    }

    @MainActor func testToggleDirection() {
        let toggleExp = expectation(description: "toggles direction")
        let objects = TestData.clone()

        let route = objects.getRoute(id: "Orange")
        let stop = objects.getStop(id: "place-bbsta")

        let sut = FavoriteStopCard(
            lineOrRoute: LineOrRoute.Route(route: route),
            stop: stop,
            direction: .init(name: "Southbound", destination: "Forest Hills", id: 0),
            toggleDirection: { toggleExp.fulfill() },
        )

        try? sut.inspect().find(ActionButton.self).implicitAnyView().button().tap()
        wait(for: [toggleExp], timeout: 1)
    }
}
