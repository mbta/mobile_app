//
//  TripVehicleCardTests.swift
//  iosAppTests
//
//  Created by esimon on 12/11/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class TripVehicleCardTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testDisplaysStopName() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .inTransitTo
            vehicle.tripId = ""
        }
        let sut = TripVehicleCard(
            vehicle: vehicle,
            stop: stop,
            tripId: "",
            targetId: "",
            terminalEntry: nil,
            routeAccents: .init(),
            now: now
        )

        try XCTAssertNotNil(sut.inspect().find(text: stop.name))
    }

    func testDisplaysStatusDescription() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let inTransitVehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .inTransitTo
            vehicle.tripId = ""
        }
        let inTransitSut = TripVehicleCard(
            vehicle: inTransitVehicle,
            stop: stop,
            tripId: "",
            targetId: "",
            terminalEntry: nil,
            routeAccents: .init(),
            now: now
        )
        try XCTAssertNotNil(inTransitSut.inspect().find(text: "Next stop"))

        let incomingVehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .incomingAt
            vehicle.tripId = ""
        }
        let incomingSut = TripVehicleCard(
            vehicle: incomingVehicle,
            stop: stop,
            tripId: "",
            targetId: "",
            terminalEntry: nil,
            routeAccents: .init(),
            now: now
        )
        try XCTAssertNotNil(incomingSut.inspect().find(text: "Approaching"))

        let stoppedVehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .stoppedAt
            vehicle.tripId = ""
        }
        let stoppedSut = TripVehicleCard(
            vehicle: stoppedVehicle,
            stop: stop,
            tripId: "",
            targetId: "",
            terminalEntry: nil,
            routeAccents: .init(),
            now: now
        )
        try XCTAssertNotNil(stoppedSut.inspect().find(text: "Now at"))
    }

    func testDifferentTrip() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .inTransitTo
            vehicle.tripId = "different"
        }
        let sut = TripVehicleCard(
            vehicle: vehicle,
            stop: stop,
            tripId: "selected",
            targetId: "",
            terminalEntry: nil,
            routeAccents: .init(),
            now: now
        )
        try XCTAssertNotNil(sut.inspect().find(text: "This vehicle is completing another trip"))
        try XCTAssertThrowsError(sut.inspect().find(text: stop.name))
    }

    func testAtTarget() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .stoppedAt
            vehicle.tripId = ""
            vehicle.stopId = stop.id
        }
        let targeted = TripVehicleCard(
            vehicle: vehicle,
            stop: stop,
            tripId: "",
            targetId: stop.id,
            terminalEntry: nil,
            routeAccents: .init(),
            now: now
        )

        XCTAssertNotNil(try targeted.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "stop-pin-indicator"
        }))

        let notTargeted = TripVehicleCard(
            vehicle: vehicle,
            stop: stop,
            tripId: "",
            targetId: "",
            terminalEntry: nil,
            routeAccents: .init(),
            now: now
        )

        XCTAssertThrowsError(try notTargeted.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "stop-pin-indicator"
        }))
    }

    func testAtTerminal() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .stoppedAt
            vehicle.tripId = ""
            vehicle.stopId = stop.id
            vehicle.currentStopSequence = 0
        }
        let prediction = objects.prediction { prediction in
            prediction.departureTime = now.addingTimeInterval(5 * 60).toKotlinInstant()
        }

        let sut = TripVehicleCard(
            vehicle: vehicle,
            stop: stop,
            tripId: "",
            targetId: stop.id,
            terminalEntry: .init(
                stop: stop,
                stopSequence: 0,
                alert: nil,
                schedule: nil,
                prediction: prediction,
                vehicle: vehicle,
                routes: []
            ),
            routeAccents: .init(),
            now: now
        )

        try XCTAssertNotNil(sut.inspect().find(text: "Waiting to depart"))
        try XCTAssertNotNil(sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "stop-pin-indicator"
        }))
        try XCTAssertNotNil(sut.inspect().find(UpcomingTripView.self))
    }
}
