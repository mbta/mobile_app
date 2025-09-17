//
//  TripDetailsPageTests.swift
//  iosApp
//
//  Created by esimon on 9/17/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class TripDetailsPageTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor
    func testDisplaysTripId() throws {
        let filter = TripDetailsPageFilter(
            tripId: "trip id", vehicleId: nil, routeId: "", directionId: 0, stopId: "", stopSequence: nil
        )
        let sut = TripDetailsPage(filter: filter, onClose: {})
        XCTAssertNotNil(try sut.inspect().find(text: "trip id: \(filter.tripId)"))
    }

    @MainActor
    func testClose() throws {
        let closeExp = expectation(description: "Page closed")
        let sut = TripDetailsPage(
            filter: .init(tripId: "", vehicleId: nil, routeId: "", directionId: 0, stopId: "", stopSequence: nil),
            onClose: { closeExp.fulfill() }
        )
        try sut.inspect().find(ActionButton.self).implicitAnyView().button().tap()
        wait(for: [closeExp], timeout: 1)
    }
}
