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
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.name = "Stop Name" }
        let route = objects.route { _ in }
        let trip = objects.trip { _ in }
        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .inTransitTo
            vehicle.tripId = trip.id
        }
        let sut = TripHeaderCard(
            spec: .vehicle(vehicle, stop, nil, false),
            trip: trip,
            targetId: "",
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: nil,
        )

        try XCTAssertNotNil(sut.inspect().find(text: stop.name))
    }

    func testDisplaysStatusDescription() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { _ in }
        let trip = objects.trip { _ in }

        let inTransitVehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .inTransitTo
            vehicle.tripId = trip.id
        }
        let inTransitSut = TripHeaderCard(
            spec: .vehicle(inTransitVehicle, stop, nil, false),
            trip: trip,
            targetId: "",
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: nil,
        )
        try XCTAssertNotNil(inTransitSut.inspect().find(text: "Next stop"))

        let incomingVehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .incomingAt
            vehicle.tripId = trip.id
        }
        let incomingSut = TripHeaderCard(
            spec: .vehicle(incomingVehicle, stop, nil, false),
            trip: trip,
            targetId: "",
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: nil,
        )
        try XCTAssertNotNil(incomingSut.inspect().find(text: "Approaching"))

        let stoppedVehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .stoppedAt
            vehicle.tripId = trip.id
        }
        let stoppedSut = TripHeaderCard(
            spec: .vehicle(stoppedVehicle, stop, nil, false),
            trip: trip,
            targetId: "",
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: nil,
        )
        try XCTAssertNotNil(stoppedSut.inspect().find(text: "Now at"))
    }

    func testDisplaysBusCrowding() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { route in
            route.type = .bus
        }
        let trip = objects.trip { _ in }

        let notCrowdedVehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .inTransitTo
            vehicle.tripId = trip.id
            vehicle.occupancyStatus = .manySeatsAvailable
        }
        let notCrowdedSut = TripHeaderCard(
            spec: .vehicle(notCrowdedVehicle, stop, nil, false),
            trip: trip,
            targetId: "",
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: nil,
        )
        try XCTAssertNotNil(notCrowdedSut.inspect().find(text: "Not crowded"))

        let someCrowdingVehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .inTransitTo
            vehicle.tripId = trip.id
            vehicle.occupancyStatus = .fewSeatsAvailable
        }
        let someCrowdingSut = TripHeaderCard(
            spec: .vehicle(someCrowdingVehicle, stop, nil, false),
            trip: trip,
            targetId: "",
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: nil,
        )
        try XCTAssertNotNil(someCrowdingSut.inspect().find(text: "Some crowding"))

        let crowdedVehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .inTransitTo
            vehicle.tripId = trip.id
            vehicle.occupancyStatus = .standingRoomOnly
        }
        let crowdedSut = TripHeaderCard(
            spec: .vehicle(crowdedVehicle, stop, nil, false),
            trip: trip,
            targetId: "",
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: nil,
        )
        try XCTAssertNotNil(crowdedSut.inspect().find(text: "Crowded"))
    }

    func testDifferentTrip() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.name = "Stop Name" }
        let route = objects.route { _ in }
        let trip = objects.trip { _ in }

        let sut = TripHeaderCard(
            spec: .finishingAnotherTrip,
            trip: trip,
            targetId: "",
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: nil,
        )
        try XCTAssertNotNil(sut.inspect().find(text: "Finishing another trip"))
        try XCTAssertThrowsError(sut.inspect().find(text: stop.name))
    }

    func testNoVehicle() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.name = "Stop Name" }
        let route = objects.route { _ in }
        let trip = objects.trip { _ in }

        let sut = TripHeaderCard(
            spec: .noVehicle,
            trip: trip,
            targetId: "",
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: nil,
        )
        try XCTAssertNotNil(sut.inspect().find(text: "Location not available yet"))
        try XCTAssertThrowsError(sut.inspect().find(text: stop.name))
    }

    func testAtTarget() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { _ in }
        let trip = objects.trip { _ in }

        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .stoppedAt
            vehicle.tripId = trip.id
            vehicle.stopId = stop.id
        }
        let targeted = TripHeaderCard(
            spec: .vehicle(vehicle, stop, nil, false),
            trip: trip,
            targetId: stop.id,
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: nil,
        )

        XCTAssertNotNil(try targeted.inspect().find(imageName: "stop-pin-indicator"))

        let notTargeted = TripHeaderCard(
            spec: .vehicle(vehicle, stop, nil, false),
            trip: trip,
            targetId: "",
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: nil,
        )

        XCTAssertThrowsError(try notTargeted.inspect().find(imageName: "stop-pin-indicator"))
    }

    func testAtTerminal() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { _ in }
        let trip = objects.trip { _ in }

        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .stoppedAt
            vehicle.tripId = trip.id
            vehicle.stopId = stop.id
            vehicle.currentStopSequence = 0
        }
        let prediction = objects.prediction { prediction in
            prediction.departureTime = now.plus(minutes: 5)
        }

        let sut = TripHeaderCard(
            spec: .vehicle(vehicle, stop, .init(
                stop: stop,
                stopSequence: 0,
                disruption: nil,
                schedule: nil,
                prediction: prediction,
                vehicle: vehicle,
                routes: [],
                elevatorAlerts: []
            ), true),
            trip: trip,
            targetId: stop.id,
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: nil,
        )

        try XCTAssertNotNil(sut.inspect().find(text: "Waiting to depart"))
        try XCTAssertNotNil(sut.inspect().find(imageName: "stop-pin-indicator"))
        try XCTAssertNotNil(sut.inspect().find(UpcomingTripView.self))
    }

    func testTrackNumber() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.id = "place-rugg" }
        let platformStop = objects.stop { platformStop in
            platformStop.platformCode = "4"
            platformStop.vehicleType = .commuterRail
            platformStop.parentStationId = stop.id
        }
        let trip = objects.trip { _ in }

        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .stoppedAt
            vehicle.tripId = trip.id
            vehicle.stopId = stop.id
            vehicle.currentStopSequence = 0
        }
        let prediction = objects.prediction { prediction in
            prediction.departureTime = now.plus(minutes: 5)
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
                routes: [],
                elevatorAlerts: []
            ), true),
            trip: trip,
            targetId: stop.id,
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: nil,
        )

        try XCTAssertNotNil(sut.inspect().find(text: "Track 4"))
    }

    func testScheduled() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.name = "Stop Name" }
        let route = objects.route { $0.type = .bus }
        let trip = objects.trip { _ in }

        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.plus(minutes: 5)
        }

        let sut = TripHeaderCard(
            spec: .scheduled(stop, .init(
                stop: stop,
                stopSequence: 0,
                disruption: nil,
                schedule: schedule,
                prediction: nil,
                vehicle: nil,
                routes: [],
                elevatorAlerts: []
            )),
            trip: trip,
            targetId: stop.id,
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: nil,
        )

        try XCTAssertNotNil(sut.inspect().find(text: "Scheduled to depart"))
        try XCTAssertNotNil(sut.inspect().find(text: stop.name))
        try XCTAssertNotNil(sut.inspect().find(UpcomingTripView.self))
        try XCTAssertThrowsError(sut.inspect().find(imageName: "fa-circle-info"))
    }

    func testScheduledTap() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { _ in }
        let trip = objects.trip { _ in }

        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.plus(minutes: 5)
        }

        let tapExpectation = expectation(description: "card tapped")

        let sut = TripHeaderCard(
            spec: .scheduled(stop, .init(
                stop: stop,
                stopSequence: 0,
                disruption: nil,
                schedule: schedule,
                prediction: nil,
                vehicle: nil,
                routes: [],
                elevatorAlerts: []
            )),
            trip: trip,
            targetId: stop.id,
            route: route,
            routeAccents: .init(route: route),
            onTap: { tapExpectation.fulfill() },
            now: now,
            onFollowTrip: nil,
        )

        try sut.inspect().find(ViewType.ZStack.self).callOnTapGesture()
        wait(for: [tapExpectation], timeout: 1)
        try XCTAssertNotNil(sut.inspect().find(imageName: "fa-circle-info"))
    }

    func testAccessibility() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.name = "stop" }
        let route = objects.route { $0.type = .bus }
        let trip = objects.trip { _ in }

        let withTap = TripHeaderCard(
            spec: .finishingAnotherTrip,
            trip: trip,
            targetId: stop.id,
            route: route,
            routeAccents: .init(route: route),
            onTap: {},
            now: now,
            onFollowTrip: nil,
        )
        XCTAssertEqual(
            "displays more information",
            try withTap.inspect().find(TripHeaderCard.self).implicitAnyView().zStack().accessibilityHint().string()
        )

        let withoutTap = TripHeaderCard(
            spec: .finishingAnotherTrip,
            trip: trip,
            targetId: stop.id,
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: nil,
        )
        XCTAssertEqual(
            "",
            try withoutTap.inspect().find(TripHeaderCard.self).implicitAnyView().zStack().accessibilityHint().string()
        )

        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .incomingAt
            vehicle.tripId = trip.id
        }

        let withVehicleAtStop = TripHeaderCard(
            spec: .vehicle(vehicle, stop, nil, false),
            trip: trip, targetId: stop.id,
            route: route,
            routeAccents: .init(route: route),
            onTap: {},
            now: now,
            onFollowTrip: nil,
        )
        XCTAssertNotNil(try withVehicleAtStop.inspect().find(
            viewWithAccessibilityLabel: "Selected bus Approaching stop, selected stop"
        ))

        let otherStop = objects.stop { stop in
            stop.name = "other stop"
        }
        let withVehicleAtOtherStop = TripHeaderCard(
            spec: .vehicle(vehicle, otherStop, nil, false),
            trip: trip, targetId: stop.id,
            route: route,
            routeAccents: .init(route: route),
            onTap: {},
            now: now,
            onFollowTrip: nil,
        )
        XCTAssertNotNil(try withVehicleAtOtherStop.inspect().find(
            viewWithAccessibilityLabel: "Selected bus Approaching other stop"
        ))

        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.plus(minutes: 5)
        }
        let withScheduleAtStop = TripHeaderCard(
            spec: .scheduled(stop, .init(
                stop: stop,
                stopSequence: 0,
                disruption: nil,
                schedule: schedule,
                prediction: nil,
                vehicle: nil,
                routes: [],
                elevatorAlerts: []
            )),
            trip: trip,
            targetId: stop.id,
            route: route,
            routeAccents: .init(route: route),
            onTap: {},
            now: now,
            onFollowTrip: nil,
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
                vehicle: nil,
                routes: [],
                elevatorAlerts: []
            )),
            trip: trip,
            targetId: stop.id,
            route: route,
            routeAccents: .init(route: route),
            onTap: {},
            now: now,
            onFollowTrip: nil,
        )
        XCTAssertNotNil(try withScheduleAtOtherStop.inspect().find(
            viewWithAccessibilityLabel: "Selected bus scheduled to depart other stop"
        ))

        let boardingVehicle = objects.vehicle { boardingVehicle in
            boardingVehicle.currentStatus = .stoppedAt
            boardingVehicle.tripId = trip.id
        }
        let coreCRStop = objects.stop { stop in
            stop.name = "North Station"
            stop.id = "place-north"
        }
        let crRoute = objects.route { $0.type = .commuterRail }
        let platformStop = objects.stop { platformStop in
            platformStop.platformCode = "4"
            platformStop.vehicleType = .commuterRail
            platformStop.parentStationId = coreCRStop.id
        }
        let withTrackNumber = TripHeaderCard(
            spec: .vehicle(boardingVehicle, coreCRStop, .init(
                stop: stop, stopSequence: 0, disruption: nil, schedule: nil,
                prediction: objects.prediction { prediction in
                    prediction.departureTime = now.plus(minutes: 5)
                    prediction.stopId = platformStop.id
                }, predictionStop: platformStop, vehicle: boardingVehicle, routes: [], elevatorAlerts: []
            ), false),
            trip: trip, targetId: coreCRStop.id,
            route: crRoute,
            routeAccents: .init(type: .commuterRail),
            onTap: {},
            now: now,
            onFollowTrip: nil,
        )
        XCTAssertNotNil(try withTrackNumber.inspect().find(viewWithAccessibilityLabel: "Boarding on track 4"))
        XCTAssertNotNil(try withTrackNumber.inspect().find(
            viewWithAccessibilityLabel: "Selected train Now at North Station, selected stop"
        ))
    }

    func testFollowButton() throws {
        let followExp = expectation(description: "follow button tapped")
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { _ in }
        let trip = objects.trip { _ in }
        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .inTransitTo
            vehicle.tripId = trip.id
        }
        let sut = TripHeaderCard(
            spec: .vehicle(vehicle, stop, nil, false),
            trip: trip,
            targetId: "",
            route: route,
            routeAccents: .init(route: route),
            onTap: nil,
            now: now,
            onFollowTrip: { followExp.fulfill() }
        )

        try sut.inspect().find(button: "Follow").tap()
        wait(for: [followExp])
    }
}
