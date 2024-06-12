//
//  StopDetailsRouteViewTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-04-09.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsRouteViewTests: XCTestCase {
    func testCallsPushNavEntry() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }

        let now = Date.now.toKotlinInstant()

        let filter = Binding<StopDetailsFilter?>(wrappedValue: nil)

        let northPattern = objects.routePattern(route: route) { $0.directionId = 0 }
        let southPattern = objects.routePattern(route: route) { $0.directionId = 1 }
        let patternsByHeadsignNorth = PatternsByHeadsign(
            route: route,
            headsign: "North",
            patterns: [northPattern],
            upcomingTrips: nil,
            alertsHere: nil
        )
        let patternsByHeadsignSouth = PatternsByHeadsign(
            route: route,
            headsign: "South",
            patterns: [southPattern],
            upcomingTrips: nil,
            alertsHere: nil
        )
        let patternsByStop = PatternsByStop(
            route: route,
            stop: stop,
            patternsByHeadsign: [patternsByHeadsignNorth, patternsByHeadsignSouth]
        )

        let pushExpNorth = XCTestExpectation(description: "Push Nav Entry called for north")

        let pushExpSouth = XCTestExpectation(description: "Push Nav Entry called for south")

        func pushExpFullfill(entry: SheetNavigationStackEntry) {
            if entry == .stopDetails(stop, .init(routeId: route.id, directionId: 0)) {
                pushExpNorth.fulfill()
            }

            if entry == .stopDetails(stop, .init(routeId: route.id, directionId: 1)) {
                pushExpSouth.fulfill()
            }
        }

        let sut = StopDetailsRouteView(patternsByStop: patternsByStop,
                                       now: now,
                                       filter: filter,
                                       pushNavEntry: pushExpFullfill)

        XCTAssertNil(filter.wrappedValue)
        try sut.inspect().find(button: "North").tap()

        wait(for: [pushExpNorth], timeout: 1)

        try sut.inspect().find(button: "South").tap()
        wait(for: [pushExpSouth], timeout: 1)
    }
}
