//
//  StopDetailsRoutesViewTests.swift
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

final class StopDetailsRoutesViewTests: XCTestCase {
    private func testData() -> (departures: StopDetailsDepartures, routeId: String) {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let wrongRoute = objects.route()

        let departures = StopDetailsDepartures(routes: [
            PatternsByStop(route: route, stop: stop, patterns: [], elevatorAlerts: []),
            PatternsByStop(route: wrongRoute, stop: stop, patterns: [], elevatorAlerts: []),
        ])

        return (departures: departures, routeId: route.id)
    }

    func testShowsListWithoutFilter() throws {
        let (departures: departures, routeId: _) = testData()

        let sut = StopDetailsRoutesView(departures: departures,
                                        global: .none,
                                        now: Date.now.toKotlinInstant(),
                                        filter: nil,
                                        setFilter: { _ in },
                                        pushNavEntry: { _ in },
                                        pinRoute: { _ in },
                                        pinnedRoutes: [])

        XCTAssertNotNil(try sut.inspect().find(StopDeparturesSummaryList.self))
        XCTAssertNil(try? sut.inspect().find(StopDetailsFilteredRouteView.self))
    }

    func testShowsFilteredWithFilter() throws {
        let (departures: departures, routeId: routeId) = testData()

        let filter = StopDetailsFilter(routeId: routeId, directionId: 0)
        let sut = StopDetailsRoutesView(
            departures: departures,
            global: .none,
            now: Date.now.toKotlinInstant(),
            filter: filter,
            setFilter: { _ in },
            pushNavEntry: { _ in },
            pinRoute: { _ in },
            pinnedRoutes: []
        )

        let actualView = try sut.inspect().find(StopDetailsFilteredRouteView.self).actualView()
        XCTAssertEqual(actualView.filter, filter)
        XCTAssertEqual(actualView.patternsByStop, departures.routes[0])
        XCTAssertNil(try? sut.inspect().list())
    }
}
