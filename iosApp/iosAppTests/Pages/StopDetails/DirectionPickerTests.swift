//
//  DirectionPickerTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 2024-04-24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class DirectionPickerTests: XCTestCase {
    private func getTestData() -> DepartureDataBundle {
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

        let leaf0 = RouteCardData.Leaf(
            directionId: 0,
            routePatterns: [patternNorth],
            stopIds: [stop.id],
            upcomingTrips: [],
            alertsHere: [],
            allDataLoaded: true,
            hasSchedulesToday: true,
            alertsDownstream: []
        )
        let leaf1 = RouteCardData.Leaf(
            directionId: 1,
            routePatterns: [patternSouth],
            stopIds: [stop.id],
            upcomingTrips: [],
            alertsHere: [],
            allDataLoaded: true,
            hasSchedulesToday: true,
            alertsDownstream: []
        )
        let stopData = RouteCardData.RouteStopData(stop: stop, directions: [
            Direction(name: "North", destination: "Selected Destination", id: 0),
            Direction(name: "South", destination: "Other Destination", id: 1),
        ], data: [leaf0, leaf1])
        let routeData = RouteCardData(
            lineOrRoute: RouteCardDataLineOrRouteRoute(route: route),
            stopData: [stopData],
            context: .stopDetailsFiltered,
            at: Date.now.toKotlinInstant()
        )

        return .init(routeData: routeData, stopData: stopData, leaf: leaf0)
    }

    func testDirectionFilter() throws {
        let data = getTestData()

        let setFilter1Exp: XCTestExpectation = .init(description: "set filter called with direction 1")
        let setFilter0Exp: XCTestExpectation = .init(description: "set filter called with direction 0")

        let filter: StopDetailsFilter? = .init(
            routeId: data.routeData.id,
            directionId: 0
        )

        let sut = DirectionPicker(data: data, filter: filter, setFilter: { filter in
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
        let data = getTestData()

        let filter: StopDetailsFilter? = .init(
            routeId: data.routeData.id,
            directionId: 0
        )

        let sut = DirectionPicker(data: data, filter: filter, setFilter: { _ in })
        XCTAssertNotNil(try sut.inspect().find(text: "Northbound to"))
        XCTAssertNotNil(try? sut.inspect().find(text: "Southbound to"))
    }
}
