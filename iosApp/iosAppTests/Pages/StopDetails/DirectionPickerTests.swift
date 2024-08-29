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
            pattern.representativeTrip {
                $0.headsign = "North"
                $0.routePatternId = "test-north"
            }
        }
        let patternSouth = objects.routePattern(route: route) { pattern in
            pattern.directionId = 1
            pattern.representativeTrip {
                $0.headsign = "South"
                $0.routePatternId = "test-south"
            }
        }

        let patternsByStop = PatternsByStop(
            routes: [route],
            line: nil,
            stop: stop,
            patterns: [
                .ByHeadsign(
                    route: route, headsign: "North", line: nil, routePatternId: "test-north",
                    patterns: [patternNorth], upcomingTrips: [], alertsHere: nil
                ),
                .ByHeadsign(
                    route: route, headsign: "South", line: nil, routePatternId: "test-south",
                    patterns: [patternSouth], upcomingTrips: [], alertsHere: nil
                ),
            ],
            directions: [
                Direction(name: "North", destination: "Selected Destination", id: 0),
                Direction(name: "South", destination: "Other Destination", id: 1),
            ]
        )

        return patternsByStop
    }

    func testDirectionFilter() throws {
        let patternsByStop = testData()

        let filter: Binding<StopDetailsFilter?> = .init(wrappedValue: .init(
            routeId: patternsByStop.routeIdentifier,
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

    func testFormatsNorthSouth() throws {
        let patternsByStop = testData()

        let filter: Binding<StopDetailsFilter?> = .init(wrappedValue: .init(
            routeId: patternsByStop.routeIdentifier,
            directionId: 0
        ))

        let sut = DirectionPicker(patternsByStop: patternsByStop, filter: filter)
        XCTAssertNotNil(try sut.inspect().find(text: "Northbound to"))
        XCTAssertNotNil(try? sut.inspect().find(text: "Southbound to"))
    }
}
