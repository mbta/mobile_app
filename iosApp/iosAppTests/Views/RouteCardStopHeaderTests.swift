//
//  RouteCardStopHeaderTests.swift
//  iosApp
//
//  Created by esimon on 4/18/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class RouteCardStopHeaderTests: XCTestCase {
    func testStopName() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { stop in
            stop.name = "Stop Name"
        }

        let sut = RouteCardStopHeader(
            data: .init(
                lineOrRoute: .route(route),
                stop: stop,
                directions: [],
                data: []
            )
        ).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(text: stop.name))
    }

    func testNotAccessible() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { stop in
            stop.name = "Boylston"
            stop.wheelchairBoarding = .inaccessible
            stop.vehicleType = .lightRail
        }

        let sut = RouteCardStopHeader(
            data: .init(
                lineOrRoute: .route(route),
                stop: stop,
                directions: [],
                data: []
            )
        ).withFixedSettings([.stationAccessibility: true])
        XCTAssertNotNil(try sut.inspect().find(text: "Not accessible"))
    }

    func testElevatorAlert() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { stop in
            stop.name = "Park Street"
            stop.wheelchairBoarding = .accessible
            stop.vehicleType = .lightRail
        }
        let alert = objects.alert { alert in
            alert.effect = .elevatorClosure
            alert.informedEntity(
                activities: [.board, .exit, .ride],
                directionId: nil, facility: nil,
                route: nil, routeType: nil,
                stop: stop.id, trip: nil
            )
        }

        let sut = RouteCardStopHeader(
            data: .init(
                lineOrRoute: .route(route),
                stop: stop,
                directions: [.init(name: "", destination: "", id: 0)],
                data: [.init(
                    lineOrRoute: .route(route), stop: stop,
                    directionId: 0, routePatterns: [], stopIds: [], upcomingTrips: [],
                    alertsHere: [alert], allDataLoaded: true, hasSchedulesToday: true, subwayServiceStartTime: nil,
                    alertsDownstream: [],
                    context: .stopDetailsFiltered
                )]
            )
        ).withFixedSettings([.stationAccessibility: true])
        XCTAssertNotNil(try sut.inspect().find(text: "1 elevator closed"))
    }
}
