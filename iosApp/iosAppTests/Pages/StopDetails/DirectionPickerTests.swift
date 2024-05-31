//
//  DirectionPickerTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 2024-04-24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class DirectionPickerTests: XCTestCase {
    private func testData() -> PatternsByStop {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }

        let patternNorth = objects.routePattern(route: route) { pattern in
            pattern.directionId = 0
            pattern.representativeTrip { $0.headsign = "North" }
        }
        let patternSouth = objects.routePattern(route: route) { pattern in
            pattern.directionId = 1
            pattern.representativeTrip { $0.headsign = "South" }
        }

        let patternsByStop = PatternsByStop(
            route: route,
            stop: stop,
            patternsByHeadsign: [
                .init(route: route, headsign: "North", patterns: [patternNorth], upcomingTrips: [], alertsHere: nil),
                .init(route: route, headsign: "South", patterns: [patternSouth], upcomingTrips: [], alertsHere: nil),
            ],
            directions: [
                Direction(name: "North", destination: "Selected Destination"),
                Direction(name: "South", destination: "Other Destination"),
            ]
        )

        return patternsByStop
    }

    func testDirectionFilter() throws {
        let patternsByStop = testData()

        let filter: Binding<StopDetailsFilter?> = .init(wrappedValue: .init(
            routeId: patternsByStop.route.id,
            directionId: 0
        ))

        let sut = DirectionPicker(patternsByStop: patternsByStop, filter: filter)
        XCTAssertEqual(0, filter.wrappedValue?.directionId)
        XCTAssertNotNil(try sut.inspect().find(text: "Selected Destination"))
        XCTAssertNotNil(try? sut.inspect().find(text: "Other Destination"))
        try sut.inspect().find(button: "Other Destination").tap()
        XCTAssertEqual(1, filter.wrappedValue?.directionId)
        try sut.inspect().find(button: "Selected Destination").tap()
        XCTAssertEqual(0, filter.wrappedValue?.directionId)
    }
}
