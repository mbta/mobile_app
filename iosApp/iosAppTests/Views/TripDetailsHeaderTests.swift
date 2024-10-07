//
//  TripDetailsHeaderTests.swift
//  iosAppTests
//
//  Created by Brandon Rodriguez on 7/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class TripDetailsHeaderTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testTripDataDisplayed() throws {
        let objects = ObjectCollectionBuilder()
        let trip = objects.trip { trip in
            trip.id = "target"
            trip.headsign = "Alewife"
        }
        let route = objects.route { route in
            route.type = .heavyRail
            route.longName = "Red Line"
        }

        let sut = TripDetailsHeader(route: route, line: nil, trip: trip, onBack: {})
        XCTAssertNotNil(try sut.inspect().find(text: "to Alewife"))
        XCTAssertNotNil(try sut.inspect().find(text: "RL"))
    }
}
