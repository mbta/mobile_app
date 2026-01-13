//
//  DepartureTileTests.swift
//  iosAppTests
//
//  Created by esimon on 11/29/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class DepartureTileTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testBasic() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let upcomingTrip = UpcomingTrip(trip: objects.trip { _ in })
        let sut = DepartureTile(
            data: .init(
                route: route,
                headsign: "headsign",
                formatted: UpcomingFormat.Some(
                    trips: [.init(
                        trip: upcomingTrip,
                        routeType: .heavyRail,
                        format: .Minutes(minutes: 5, last: false),
                        lastTrip: false
                    )],
                    secondaryAlert: nil
                ),
                upcoming: upcomingTrip
            ),
            onTap: {}
        )

        XCTAssertNotNil(try sut.inspect().find(text: "headsign"))
        XCTAssertNotNil(try sut.inspect().find(text: "5 min"))
        XCTAssertNotNil(try sut.inspect().find(TripStatus.self))
    }

    func testPillDecorator() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let upcomingTrip = UpcomingTrip(trip: objects.trip { _ in })
        let sut = DepartureTile(
            data: .init(
                route: route,
                headsign: "headsign",
                formatted: UpcomingFormat.Some(
                    trips: [.init(
                        trip: upcomingTrip,
                        routeType: .heavyRail,
                        format: .Minutes(minutes: 5, last: false),
                        lastTrip: false
                    )],
                    secondaryAlert: nil
                ),
                upcoming: upcomingTrip
            ),
            onTap: {},
            pillDecoration: .onPrediction(route: route)
        )

        XCTAssertNotNil(try sut.inspect().find(RoutePill.self))
    }

    func testTap() throws {
        let objects = ObjectCollectionBuilder()

        let tapExpectation = XCTestExpectation(description: "Departure tile tap callback")

        let route = objects.route()
        let upcomingTrip = UpcomingTrip(trip: objects.trip { _ in })
        let sut = DepartureTile(
            data: .init(
                route: route,
                headsign: "headsign",
                formatted: UpcomingFormat.Some(
                    trips: [.init(
                        trip: upcomingTrip,
                        routeType: .heavyRail,
                        format: .Minutes(minutes: 5, last: false),
                        lastTrip: false
                    )],
                    secondaryAlert: nil
                ),
                upcoming: upcomingTrip
            ),
            onTap: { tapExpectation.fulfill() },
            pillDecoration: .onPrediction(route: route)
        )

        XCTAssertNoThrow(try sut.inspect().find(ViewType.Button.self).tap())
        wait(for: [tapExpectation], timeout: 1)
    }

    func testAccessibility() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let upcomingTrip = UpcomingTrip(trip: objects.trip { _ in })

        let tileData = TileData(
            route: route,
            headsign: "headsign",
            formatted: UpcomingFormat.Some(
                trips: [.init(
                    trip: upcomingTrip,
                    routeType: .heavyRail,
                    format: .Minutes(minutes: 5, last: false),
                    lastTrip: false
                )],
                secondaryAlert: nil
            ),
            upcoming: upcomingTrip
        )
        let notSelected = DepartureTile(data: tileData, onTap: {})
        XCTAssertEqual(
            "displays more information about this trip",
            try notSelected.inspect().implicitAnyView().button().accessibilityHint().string()
        )

        let selected = DepartureTile(data: tileData, onTap: {}, isSelected: true)
        XCTAssertEqual("", try selected.inspect().implicitAnyView().button().accessibilityHint().string())
    }
}
