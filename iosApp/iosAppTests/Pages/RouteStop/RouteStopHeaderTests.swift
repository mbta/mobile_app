//
//  RouteStopHeaderTests.swift
//  iosAppTests
//
//  Created by esimon on 12/2/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class RouteStopHeaderTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testDisplaysStopName() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let sut = RouteStopHeader(route: route, stop: stop, direction: 0, navCallbacks: .companion.empty)
        XCTAssertNotNil(try sut.inspect().find(text: "at **\(stop.name)**"))
    }

    func testDisplaysRoutePill() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let sut = RouteStopHeader(route: route, stop: stop, direction: 0, navCallbacks: .companion.empty)
        XCTAssertNotNil(try sut.inspect().find(RoutePill.self))
    }

    func testStarTap() throws {
        let tapExpectation = XCTestExpectation(description: "Star button tap callback")
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let sut = RouteStopHeader(
            route: route,
            stop: stop,
            direction: 0,
            onFavorite: { tapExpectation.fulfill() },
            navCallbacks: .companion.empty
        )
        XCTAssertNoThrow(try sut.inspect().find(StarButton.self).find(ViewType.Button.self).tap())
        wait(for: [tapExpectation], timeout: 1)
    }

    func testCloseTap() throws {
        let tapExpectation = XCTestExpectation(description: "Close button tap callback")
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let sut = RouteStopHeader(
            route: route,
            stop: stop,
            direction: 0,
            navCallbacks: .init(onBack: nil, onClose: { tapExpectation.fulfill() }, backButtonPresentation: .floating)
        )
        XCTAssertNoThrow(try sut.inspect().find(ActionButton.self).find(ViewType.Button.self).tap())
        wait(for: [tapExpectation], timeout: 1)
    }
}
