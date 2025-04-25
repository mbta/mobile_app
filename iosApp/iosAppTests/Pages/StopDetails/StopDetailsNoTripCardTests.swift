//
//  StopDetailsNoTripCardTests.swift
//  iosAppTests
//
//  Created by esimon on 1/3/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsNoTripCardTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testPredictionsUnavailable() throws {
        let sut = StopDetailsNoTripCard(
            status: UpcomingFormat.NoTripsFormatPredictionsUnavailable(),
            accentColor: Color.text,
            routeType: .bus
        ).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "live-data-slash"
        }))
        XCTAssertNotNil(try sut.inspect().find(text: "Predictions unavailable"))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Divider.self))
        XCTAssertNotNil(try sut.inspect().find(
            text: "Service is running, but predicted arrival times aren’t available." +
                " Check the map to see where buses are right now."
        ))
    }

    func testServiceEnded() throws {
        let sut = StopDetailsNoTripCard(
            status: UpcomingFormat.NoTripsFormatServiceEndedToday(),
            accentColor: Color.text,
            routeType: .ferry
        ).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "mode-ferry-slash"
        }))
        XCTAssertNotNil(try sut.inspect().find(text: "Service ended"))
        XCTAssertThrowsError(try sut.inspect().find(ViewType.Divider.self))
    }

    func testNoSchedulesToday() throws {
        let sut = StopDetailsNoTripCard(
            status: UpcomingFormat.NoTripsFormatNoSchedulesToday(),
            accentColor: Color.text,
            routeType: .commuterRail
        ).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "mode-cr-slash"
        }))
        XCTAssertNotNil(try sut.inspect().find(text: "No service today"))
        XCTAssertThrowsError(try sut.inspect().find(ViewType.Divider.self))
    }
}
