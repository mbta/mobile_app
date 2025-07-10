//
//  TripStopRowTests.swift
//  iosAppTests
//
//  Created by esimon on 12/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class TripStopRowTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testStopName() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let trip = objects.trip { _ in }
        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.addingTimeInterval(5).toKotlinInstant()
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.addingTimeInterval(6).toKotlinInstant()
        }
        let route = objects.route()

        let sut = TripStopRow(
            stop: .init(
                stop: stop, stopSequence: 0, disruption: nil,
                schedule: schedule, prediction: prediction,
                vehicle: nil, routes: [route], elevatorAlerts: []
            ),
            trip: trip,
            now: now.toKotlinInstant(),
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(route: route),
            alertSummaries: [:]
        )

        XCTAssertNotNil(try sut.inspect().find(text: stop.name))
    }

    func testPrediction() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let trip = objects.trip { _ in }
        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.addingTimeInterval(5).toKotlinInstant()
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.addingTimeInterval(6).toKotlinInstant()
        }
        let route = objects.route()

        let sut = TripStopRow(
            stop: .init(
                stop: stop, stopSequence: 0, disruption: nil,
                schedule: schedule, prediction: prediction,
                vehicle: nil, routes: [route], elevatorAlerts: []
            ),
            trip: trip,
            now: now.toKotlinInstant(),
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(route: route),
            alertSummaries: [:]
        )

        XCTAssertNotNil(try sut.inspect().find(UpcomingTripView.self))
    }

    func testTrackNumber() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.id = "place-sstat" }
        let platformStop = objects.stop { platformStop in
            platformStop.platformCode = "7"
            platformStop.vehicleType = .commuterRail
            platformStop.parentStationId = stop.id
        }
        let trip = objects.trip { _ in }
        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.addingTimeInterval(5).toKotlinInstant()
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.addingTimeInterval(6).toKotlinInstant()
            prediction.stopId = platformStop.id
        }
        let route = objects.route { route in route.type = .commuterRail }

        let sut = TripStopRow(
            stop: .init(
                stop: stop, stopSequence: 0, disruption: nil,
                schedule: schedule, prediction: prediction, predictionStop: platformStop,
                vehicle: nil, routes: [route], elevatorAlerts: []
            ),
            trip: trip,
            now: now.toKotlinInstant(),
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: .init(route: route),
            alertSummaries: [:]
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Track 7"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "Boarding on track 7"))
    }

    func testTargetPin() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let trip = objects.trip { _ in }
        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.addingTimeInterval(5).toKotlinInstant()
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.addingTimeInterval(6).toKotlinInstant()
        }
        let route = objects.route()

        let targeted = TripStopRow(
            stop: .init(
                stop: stop, stopSequence: 0, disruption: nil,
                schedule: schedule, prediction: prediction,
                vehicle: nil, routes: [route], elevatorAlerts: []
            ),
            trip: trip,
            now: now.toKotlinInstant(),
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(route: route),
            alertSummaries: [:],
            targeted: true
        )

        XCTAssertNotNil(try targeted.inspect().find(imageName: "stop-pin-indicator"))

        let notTargeted = TripStopRow(
            stop: .init(
                stop: stop, stopSequence: 0, disruption: nil,
                schedule: schedule, prediction: prediction,
                vehicle: nil, routes: [route], elevatorAlerts: []
            ),
            trip: trip,
            now: now.toKotlinInstant(),
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(route: route),
            alertSummaries: [:]
        )

        XCTAssertThrowsError(try notTargeted.inspect().find(imageName: "stop-pin-indicator"))
    }

    func testAccessibility() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.name = "stop" }
        let trip = objects.trip { _ in }
        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.addingTimeInterval(5).toKotlinInstant()
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.addingTimeInterval(6).toKotlinInstant()
        }
        let route = objects.route()

        let stopEntry = TripDetailsStopList.Entry(
            stop: stop, stopSequence: 0, disruption: nil,
            schedule: schedule, prediction: prediction,
            vehicle: nil, routes: [route], elevatorAlerts: []
        )

        let basicRow = TripStopRow(
            stop: stopEntry,
            trip: trip,
            now: now.toKotlinInstant(),
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(route: route),
            alertSummaries: [:]
        )
        XCTAssertNotNil(try basicRow.inspect().find(viewWithAccessibilityLabel: "stop"))

        let selectedRow = TripStopRow(
            stop: stopEntry,
            trip: trip,
            now: now.toKotlinInstant(),
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(route: route),
            alertSummaries: [:],
            targeted: true
        )
        XCTAssertNotNil(try selectedRow.inspect().find(viewWithAccessibilityLabel: "stop, selected stop"))

        let firstRow = TripStopRow(
            stop: stopEntry,
            trip: trip,
            now: now.toKotlinInstant(),
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(route: route),
            alertSummaries: [:],
            firstStop: true
        )
        XCTAssertNotNil(try firstRow.inspect().find(viewWithAccessibilityLabel: "stop, first stop"))

        let selectedFirstRow = TripStopRow(
            stop: stopEntry,
            trip: trip,
            now: now.toKotlinInstant(),
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(route: route),
            alertSummaries: [:],
            targeted: true,
            firstStop: true
        )
        XCTAssertNotNil(try selectedFirstRow.inspect().find(
            viewWithAccessibilityLabel: "stop, selected stop, first stop"
        ))
    }

    func testWheelchairNotAccessible() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in
            stop.name = "stop"
            stop.wheelchairBoarding = .inaccessible
        }
        let trip = objects.trip { _ in }
        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.addingTimeInterval(5).toKotlinInstant()
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.addingTimeInterval(6).toKotlinInstant()
        }
        let route = objects.route()

        let stopEntry = TripDetailsStopList.Entry(
            stop: stop, stopSequence: 0, disruption: nil,
            schedule: schedule, prediction: prediction,
            vehicle: nil, routes: [route], elevatorAlerts: []
        )

        let row = TripStopRow(
            stop: stopEntry,
            trip: trip,
            now: now.toKotlinInstant(),
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(route: route),
            alertSummaries: [:],
            showStationAccessibility: true
        )
        XCTAssertNotNil(try row.inspect().find(viewWithTag: "wheelchair_not_accessible"))
        XCTAssertNotNil(try row.inspect().find(viewWithAccessibilityLabel: "This stop is not accessible"))
    }

    func testElevatorAccessibilityAlert() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in
            stop.name = "stop"
            stop.wheelchairBoarding = .accessible
        }
        let trip = objects.trip { _ in }
        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.addingTimeInterval(5).toKotlinInstant()
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.addingTimeInterval(6).toKotlinInstant()
        }
        let route = objects.route()

        let stopEntry = TripDetailsStopList.Entry(
            stop: stop, stopSequence: 0, disruption: nil,
            schedule: schedule, prediction: prediction,
            vehicle: nil, routes: [route], elevatorAlerts: [objects.alert {
                $0.activePeriod(
                    start: Date.now.addingTimeInterval(-20 * 60).toKotlinInstant(),
                    end: Date.now.addingTimeInterval(20 * 60).toKotlinInstant()
                )
            }]
        )

        let row = TripStopRow(
            stop: stopEntry,
            trip: trip,
            now: now.toKotlinInstant(),
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(route: route),
            alertSummaries: [:],
            showStationAccessibility: true
        )
        XCTAssertNotNil(try row.inspect().find(viewWithTag: "elevator_alert"))
        XCTAssertNotNil(try row.inspect().find(viewWithAccessibilityLabel: "This stop has 1 elevator closed"))
    }

    func testAlertCard() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let trip = objects.trip { _ in }
        let route = objects.route { _ in }
        let alert = objects.alert { $0.effect = .shuttle }
        let summary = AlertSummary(
            effect: alert.effect,
            location: AlertSummary
                .LocationSuccessiveStops(startStopName: "Roxbury Crossing", endStopName: "Green Street"),
            timeframe: AlertSummary.TimeframeTomorrow.shared
        )

        let entry = TripDetailsStopList.Entry(
            stop: stop,
            stopSequence: 0,
            disruption: .init(alert: alert, mapStopRoute: .orange),
            schedule: nil,
            prediction: nil,
            vehicle: nil,
            routes: []
        )

        let sut = TripStopRow(
            stop: entry,
            trip: trip,
            now: now.toKotlinInstant(),
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: .init(route: route),
            alertSummaries: [alert.id: summary],
            showDownstreamAlert: true
        )

        XCTAssertNotNil(try sut.inspect()
            .find(text: "Shuttle buses from Roxbury Crossing to Green Street through tomorrow"))
    }
}
