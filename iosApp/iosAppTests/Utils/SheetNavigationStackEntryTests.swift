//
//  SheetNavigationStackEntryTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-04-09.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import XCTest

final class SheetNavigationStackEntryTests: XCTestCase {
    func testLastFilterEmpty() throws {
        var stack: [SheetNavigationStackEntry] = []

        XCTAssertNil(stack.lastStopDetailsFilter)

        stack.lastStopDetailsFilter = .init(routeId: "A", directionId: 1)

        XCTAssertEqual(stack, [])
    }

    func testLastFilterShallow() throws {
        let stop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        var stack: [SheetNavigationStackEntry] = [.stopDetails(stop, nil)]

        XCTAssertNil(stack.lastStopDetailsFilter)

        stack.lastStopDetailsFilter = .init(routeId: "A", directionId: 1)

        XCTAssertEqual(stack, [.stopDetails(stop, .init(routeId: "A", directionId: 1))])
        XCTAssertEqual(stack.lastStopDetailsFilter, .init(routeId: "A", directionId: 1))

        stack.lastStopDetailsFilter = nil

        XCTAssertEqual(stack, [.stopDetails(stop, nil)])
        XCTAssertEqual(stack.lastStopDetailsFilter, nil)
    }

    func testLastFilterDeep() throws {
        let stop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        let otherStop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        let previousEntry = SheetNavigationStackEntry.stopDetails(otherStop, .init(routeId: "A", directionId: 1))
        var stack: [SheetNavigationStackEntry] = [previousEntry, .stopDetails(stop, .init(routeId: "B", directionId: 0))]

        XCTAssertEqual(stack.lastStopDetailsFilter, .init(routeId: "B", directionId: 0))

        stack.lastStopDetailsFilter = nil

        XCTAssertEqual(stack, [previousEntry, .stopDetails(stop, nil)])
        XCTAssertEqual(stack.lastStopDetailsFilter, nil)
    }
}
