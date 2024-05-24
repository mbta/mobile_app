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
    func testSetFilter() throws {
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
        let sut = StopDetailsRouteView(patternsByStop: patternsByStop, now: now, filter: filter)

        XCTAssertNil(filter.wrappedValue)
        try sut.inspect().find(button: "North").tap()
        XCTAssertEqual(filter.wrappedValue, .init(routeId: route.id, directionId: 0))
        try sut.inspect().find(button: "South").tap()
        XCTAssertEqual(filter.wrappedValue, .init(routeId: route.id, directionId: 1))
    }
}
