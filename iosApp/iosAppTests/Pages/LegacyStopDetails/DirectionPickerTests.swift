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
            }
        }
        let patternSouth = objects.routePattern(route: route) { pattern in
            pattern.directionId = 1
            pattern.representativeTrip {
                $0.headsign = "South"
            }
        }

        let patternsByStop = PatternsByStop(
            routes: [route],
            line: nil,
            stop: stop,
            patterns: [
                .ByHeadsign(
                    route: route, headsign: "North", line: nil,
                    patterns: [patternNorth], upcomingTrips: []
                ),
                .ByHeadsign(
                    route: route, headsign: "South", line: nil,
                    patterns: [patternSouth], upcomingTrips: []
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

        let setFilter1Exp: XCTestExpectation = .init(description: "set filter called with direction 1")
        let setFilter0Exp: XCTestExpectation = .init(description: "set filter called with direction 0")

        let filter: StopDetailsFilter? = .init(
            routeId: patternsByStop.routeIdentifier,
            directionId: 0
        )

        let sut = DirectionPicker(patternsByStop: patternsByStop, filter: filter, setFilter: { filter in
            if filter?.directionId == 1 {
                setFilter1Exp.fulfill()
            }
            if filter?.directionId == 0 {
                setFilter0Exp.fulfill()
            }
        })
        XCTAssertNotNil(try sut.inspect().find(text: "Selected Destination"))
        XCTAssertNotNil(try? sut.inspect().find(text: "Other Destination"))
        try sut.inspect().find(button: "Other Destination").tap()
        wait(for: [setFilter1Exp], timeout: 1)
        try sut.inspect().find(button: "Selected Destination").tap()
        wait(for: [setFilter0Exp], timeout: 1)
    }

    func testFormatsNorthSouth() throws {
        let patternsByStop = testData()

        let filter: StopDetailsFilter? = .init(
            routeId: patternsByStop.routeIdentifier,
            directionId: 0
        )

        let sut = DirectionPicker(patternsByStop: patternsByStop, filter: filter, setFilter: { _ in })
        XCTAssertNotNil(try sut.inspect().find(text: "Northbound to"))
        XCTAssertNotNil(try? sut.inspect().find(text: "Southbound to"))
    }
}
