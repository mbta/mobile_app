//
//  TripHeaderCardTests.swift
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

final class TripHeaderCardTests: XCTestCase {
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
        let sut = TripHeaderCard(
            spec: .vehicle(vehicle, nil),
            stop: stop,
            tripId: "",
            targetId: "",
            routeAccents: .init(),
            onTap: nil,
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
        let inTransitSut = TripHeaderCard(
            spec: .vehicle(inTransitVehicle, nil),
            stop: stop,
            tripId: "",
            targetId: "",
            routeAccents: .init(),
            onTap: nil,
            now: now
        )
        try XCTAssertNotNil(inTransitSut.inspect().find(text: "Next stop"))

        let incomingVehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .incomingAt
            vehicle.tripId = ""
        }
        let incomingSut = TripHeaderCard(
            spec: .vehicle(incomingVehicle, nil),
            stop: stop,
            tripId: "",
            targetId: "",
            routeAccents: .init(),
            onTap: nil,
            now: now
        )
        try XCTAssertNotNil(incomingSut.inspect().find(text: "Approaching"))

        let stoppedVehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .stoppedAt
            vehicle.tripId = ""
        }
        let stoppedSut = TripHeaderCard(
            spec: .vehicle(stoppedVehicle, nil),
            stop: stop,
            tripId: "",
            targetId: "",
            routeAccents: .init(),
            onTap: nil,
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
        let sut = TripHeaderCard(
            spec: .vehicle(vehicle, nil),
            stop: stop,
            tripId: "selected",
            targetId: "",
            routeAccents: .init(),
            onTap: nil,
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
        let targeted = TripHeaderCard(
            spec: .vehicle(vehicle, nil),
            stop: stop,
            tripId: "",
            targetId: stop.id,
            routeAccents: .init(),
            onTap: nil,
            now: now
        )

        XCTAssertNotNil(try targeted.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "stop-pin-indicator"
        }))

        let notTargeted = TripHeaderCard(
            spec: .vehicle(vehicle, nil),
            stop: stop,
            tripId: "",
            targetId: "",
            routeAccents: .init(),
            onTap: nil,
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

        let sut = TripHeaderCard(
            spec: .vehicle(vehicle, .init(
                stop: stop,
                stopSequence: 0,
                alert: nil,
                schedule: nil,
                prediction: prediction,
                vehicle: vehicle,
                routes: []
            )),
            stop: stop,
            tripId: "",
            targetId: stop.id,
            routeAccents: .init(),
            onTap: nil,
            now: now
        )

        try XCTAssertNotNil(sut.inspect().find(text: "Waiting to depart"))
        try XCTAssertNotNil(sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "stop-pin-indicator"
        }))
        try XCTAssertNotNil(sut.inspect().find(UpcomingTripView.self))
    }

    func testScheduled() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.addingTimeInterval(5 * 60).toKotlinInstant()
        }

        let sut = TripHeaderCard(
            spec: .scheduled(.init(
                stop: stop,
                stopSequence: 0,
                alert: nil,
                schedule: schedule,
                prediction: nil,
                vehicle: nil,
                routes: []
            )),
            stop: stop,
            tripId: "",
            targetId: stop.id,
            routeAccents: .init(),
            onTap: nil,
            now: now
        )

        try XCTAssertNotNil(sut.inspect().find(text: "Scheduled to depart"))
        try XCTAssertNotNil(sut.inspect().find(text: stop.name))
        try XCTAssertNotNil(sut.inspect().find(UpcomingTripView.self))
    }

    func testScheduledTap() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.addingTimeInterval(5 * 60).toKotlinInstant()
        }

        let tapExpectation = expectation(description: "card tapped")

        let sut = TripHeaderCard(
            spec: .scheduled(.init(
                stop: stop,
                stopSequence: 0,
                alert: nil,
                schedule: schedule,
                prediction: nil,
                vehicle: nil,
                routes: []
            )),
            stop: stop,
            tripId: "",
            targetId: stop.id,
            routeAccents: .init(),
            onTap: { tapExpectation.fulfill() },
            now: now
        )

        try sut.inspect().find(ViewType.ZStack.self).callOnTapGesture()
        wait(for: [tapExpectation], timeout: 1)
    }
}
