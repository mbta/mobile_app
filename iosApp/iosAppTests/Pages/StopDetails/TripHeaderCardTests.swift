//
//  TripHeaderCardTests.swift
//  iosAppTests
//
//  Created by esimon on 12/11/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
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
        let stop = objects.stop { stop in stop.name = "Stop Name" }
        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .inTransitTo
            vehicle.tripId = ""
        }
        let sut = TripHeaderCard(
            spec: .vehicle(vehicle, stop, nil, false),
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
            spec: .vehicle(inTransitVehicle, stop, nil, false),
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
            spec: .vehicle(incomingVehicle, stop, nil, false),
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
            spec: .vehicle(stoppedVehicle, stop, nil, false),
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
        let stop = objects.stop { stop in stop.name = "Stop Name" }

        let sut = TripHeaderCard(
            spec: .finishingAnotherTrip,
            tripId: "selected",
            targetId: "",
            routeAccents: .init(),
            onTap: nil,
            now: now
        )
        try XCTAssertNotNil(sut.inspect().find(text: "Finishing another trip"))
        try XCTAssertThrowsError(sut.inspect().find(text: stop.name))
    }

    func testNoVehicle() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.name = "Stop Name" }

        let sut = TripHeaderCard(
            spec: .noVehicle,
            tripId: "selected",
            targetId: "",
            routeAccents: .init(),
            onTap: nil,
            now: now
        )
        try XCTAssertNotNil(sut.inspect().find(text: "Location not available yet"))
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
            spec: .vehicle(vehicle, stop, nil, false),

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
            spec: .vehicle(vehicle, stop, nil, false),

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
            spec: .vehicle(vehicle, stop, .init(
                stop: stop,
                stopSequence: 0,
                disruption: nil,
                schedule: nil,
                prediction: prediction,
                predictionStop: nil,
                vehicle: vehicle,
                routes: []
            ), true),
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

    func testTrackNumber() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.id = "place-rugg" }
        let platformStop = objects.stop { platformStop in
            platformStop.platformCode = "4"
            platformStop.vehicleType = .commuterRail
            platformStop.parentStationId = stop.id
        }

        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .stoppedAt
            vehicle.tripId = ""
            vehicle.stopId = stop.id
            vehicle.currentStopSequence = 0
        }
        let prediction = objects.prediction { prediction in
            prediction.departureTime = now.addingTimeInterval(5 * 60).toKotlinInstant()
            prediction.stopId = platformStop.id
        }
        let route = objects.route { route in route.type = .commuterRail }

        let sut = TripHeaderCard(
            spec: .vehicle(vehicle, stop, .init(
                stop: stop,
                stopSequence: 0,
                disruption: nil,
                schedule: nil,
                prediction: prediction,
                predictionStop: platformStop,
                vehicle: vehicle,
                routes: []
            ), true),
            tripId: "",
            targetId: stop.id,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now
        )

        try XCTAssertNotNil(sut.inspect().find(text: "Track 4"))
    }

    func testScheduled() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.name = "Stop Name" }

        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.addingTimeInterval(5 * 60).toKotlinInstant()
        }

        let sut = TripHeaderCard(
            spec: .scheduled(stop, .init(
                stop: stop,
                stopSequence: 0,
                disruption: nil,
                schedule: schedule,
                prediction: nil,
                predictionStop: nil,
                vehicle: nil,
                routes: []
            )),
            tripId: "",
            targetId: stop.id,
            routeAccents: .init(),
            onTap: nil,
            now: now
        )

        try XCTAssertNotNil(sut.inspect().find(text: "Scheduled to depart"))
        try XCTAssertNotNil(sut.inspect().find(text: stop.name))
        try XCTAssertNotNil(sut.inspect().find(UpcomingTripView.self))
        try XCTAssertThrowsError(sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "fa-circle-info"
        }))
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
            spec: .scheduled(stop, .init(
                stop: stop,
                stopSequence: 0,
                disruption: nil,
                schedule: schedule,
                prediction: nil,
                predictionStop: nil,
                vehicle: nil,
                routes: []
            )),
            tripId: "",
            targetId: stop.id,
            routeAccents: .init(),
            onTap: { tapExpectation.fulfill() },
            now: now
        )

        try sut.inspect().find(ViewType.ZStack.self).callOnTapGesture()
        wait(for: [tapExpectation], timeout: 1)
        try XCTAssertNotNil(sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "fa-circle-info"
        }))
    }

    func testAccessibility() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.name = "stop" }

        let withTap = TripHeaderCard(
            spec: .finishingAnotherTrip,
            tripId: "",
            targetId: stop.id,
            routeAccents: .init(),
            onTap: {},
            now: now
        )
        XCTAssertEqual(
            "displays more information",
            try withTap.inspect().find(TripHeaderCard.self).implicitAnyView().zStack().accessibilityHint().string()
        )

        let withoutTap = TripHeaderCard(
            spec: .finishingAnotherTrip,
            tripId: "",
            targetId: stop.id,
            routeAccents: .init(),
            onTap: nil,
            now: now
        )
        XCTAssertEqual(
            "",
            try withoutTap.inspect().find(TripHeaderCard.self).implicitAnyView().zStack().accessibilityHint().string()
        )

        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .incomingAt
        }

        let withVehicleAtStop = TripHeaderCard(
            spec: .vehicle(vehicle, stop, nil, false),
            tripId: "", targetId: stop.id,
            routeAccents: .init(type: .bus),
            onTap: {},
            now: now
        )
        XCTAssertNotNil(try withVehicleAtStop.inspect().find(
            viewWithAccessibilityLabel: "Selected bus Approaching stop, selected stop"
        ))

        let otherStop = objects.stop { stop in
            stop.name = "other stop"
        }
        let withVehicleAtOtherStop = TripHeaderCard(
            spec: .vehicle(vehicle, otherStop, nil, false),
            tripId: "", targetId: stop.id,
            routeAccents: .init(type: .bus),
            onTap: {},
            now: now
        )
        XCTAssertNotNil(try withVehicleAtOtherStop.inspect().find(
            viewWithAccessibilityLabel: "Selected bus Approaching other stop"
        ))

        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.addingTimeInterval(5 * 60).toKotlinInstant()
        }
        let withScheduleAtStop = TripHeaderCard(
            spec: .scheduled(stop, .init(
                stop: stop,
                stopSequence: 0,
                disruption: nil,
                schedule: schedule,
                prediction: nil,
                predictionStop: nil,
                vehicle: nil,
                routes: []
            )),
            tripId: "",
            targetId: stop.id,
            routeAccents: .init(),
            onTap: {},
            now: now
        )
        XCTAssertNotNil(try withScheduleAtStop.inspect().find(
            viewWithAccessibilityLabel: "Selected bus scheduled to depart stop, selected stop"
        ))

        let withScheduleAtOtherStop = TripHeaderCard(
            spec: .scheduled(otherStop, .init(
                stop: otherStop,
                stopSequence: 0,
                disruption: nil,
                schedule: schedule,
                prediction: nil,
                predictionStop: nil,
                vehicle: nil,
                routes: []
            )),
            tripId: "",
            targetId: stop.id,
            routeAccents: .init(),
            onTap: {},
            now: now
        )
        XCTAssertNotNil(try withScheduleAtOtherStop.inspect().find(
            viewWithAccessibilityLabel: "Selected bus scheduled to depart other stop"
        ))

        let boardingVehicle = objects.vehicle { boardingVehicle in
            boardingVehicle.currentStatus = .stoppedAt
        }
        let coreCRStop = objects.stop { stop in
            stop.name = "North Station"
            stop.id = "place-north"
        }
        let platformStop = objects.stop { platformStop in
            platformStop.platformCode = "4"
            platformStop.vehicleType = .commuterRail
            platformStop.parentStationId = coreCRStop.id
        }
        let withTrackNumber = TripHeaderCard(
            spec: .vehicle(boardingVehicle, coreCRStop, .init(
                stop: stop, stopSequence: 0, disruption: nil, schedule: nil,
                prediction: objects.prediction { prediction in
                    prediction.departureTime = now.addingTimeInterval(5 * 60).toKotlinInstant()
                    prediction.stopId = platformStop.id
                }, predictionStop: platformStop, vehicle: boardingVehicle, routes: []
            ), false),
            tripId: "", targetId: coreCRStop.id,
            routeAccents: .init(type: .commuterRail),
            onTap: {},
            now: now
        )
        XCTAssertNotNil(try withTrackNumber.inspect().find(viewWithAccessibilityLabel: "Boarding on track 4"))
        XCTAssertNotNil(try withTrackNumber.inspect().find(
            viewWithAccessibilityLabel: "Selected train Now at North Station, selected stop"
        ))
    }
}
