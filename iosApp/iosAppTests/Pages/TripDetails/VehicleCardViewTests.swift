//
//  VehicleCardViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 5/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import shared
import ViewInspector
import XCTest

final class VehicleCardViewTests: XCTestCase {
    func testVehicleTripMismatchShowsNothing() {
        let objects = ObjectCollectionBuilder()
        let trip = objects.trip { trip in
            trip.id = "target"
            trip.headsign = "Alewife"
        }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = "other"
            vehicle.currentStatus = .stoppedAt
        }

        let route = objects.route { route in
            route.type = .heavyRail
            route.longName = "Red Line"
        }
        let stop = objects.stop { _ in }

        let sut = VehicleCardView(vehicle: vehicle, route: route, line: nil, stop: stop, trip: trip)
        XCTAssertNotNil(try sut.inspect().find(text: "This vehicle is completing another trip."))
    }

    func testVehicleApproaching() {
        let objects = ObjectCollectionBuilder()
        let trip = objects.trip { trip in
            trip.id = "target"
        }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = "target"
            vehicle.currentStatus = .incomingAt
        }

        let route = objects.route { _ in }
        let stop = objects.stop { stop in
            stop.name = "Davis"
        }

        let sut = VehicleCardView(vehicle: vehicle, route: route, line: nil, stop: stop, trip: trip)
        XCTAssertNotNil(try sut.inspect().find(text: "Approaching"))
        XCTAssertNotNil(try sut.inspect().find(text: "Davis"))
    }

    func testVehicleInTransit() {
        let objects = ObjectCollectionBuilder()
        let trip = objects.trip { trip in
            trip.id = "target"
        }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = "target"
            vehicle.currentStatus = .inTransitTo
        }

        let route = objects.route { _ in }
        let stop = objects.stop { _ in }

        let sut = VehicleCardView(vehicle: vehicle, route: route, line: nil, stop: stop, trip: trip)
        XCTAssertNotNil(try sut.inspect().find(text: "Next stop"))
    }

    func testVehicleStopped() {
        let objects = ObjectCollectionBuilder()
        let trip = objects.trip { trip in
            trip.id = "target"
        }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = "target"
            vehicle.currentStatus = .stoppedAt
        }

        let route = objects.route { _ in }
        let stop = objects.stop { _ in }

        let sut = VehicleCardView(vehicle: vehicle, route: route, line: nil, stop: stop, trip: trip)
        XCTAssertNotNil(try sut.inspect().find(text: "Now at"))
    }

    func testLoading() {
        let objects = ObjectCollectionBuilder()
        let trip = objects.trip { trip in
            trip.id = "target"
            trip.headsign = "Alewife"
        }

        let sut = VehicleCardView(vehicle: nil, route: nil, line: nil, stop: nil, trip: trip)
        XCTAssertNotNil(try sut.inspect().find(text: "Loading..."))
    }
}
