//
//  TripStopRowTests.swift
//  iosAppTests
//
//  Created by esimon on 12/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
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
        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.addingTimeInterval(5).toKotlinInstant()
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.addingTimeInterval(6).toKotlinInstant()
        }
        let route = objects.route()

        let sut = TripStopRow(
            stop: .init(
                stop: stop, stopSequence: 0, alert: nil,
                schedule: schedule, prediction: prediction, predictionStop: nil,
                vehicle: nil, routes: [route]
            ),
            now: now.toKotlinInstant(),
            onTapLink: { _, _, _ in },
            routeAccents: TripRouteAccents(route: route)
        )

        XCTAssertNotNil(try sut.inspect().find(text: stop.name))
    }

    func testPrediction() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.addingTimeInterval(5).toKotlinInstant()
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.addingTimeInterval(6).toKotlinInstant()
        }
        let route = objects.route()

        let sut = TripStopRow(
            stop: .init(
                stop: stop, stopSequence: 0, alert: nil,
                schedule: schedule, prediction: prediction, predictionStop: nil,
                vehicle: nil, routes: [route]
            ),
            now: now.toKotlinInstant(),
            onTapLink: { _, _, _ in },
            routeAccents: TripRouteAccents(route: route)
        )

        XCTAssertNotNil(try sut.inspect().find(UpcomingTripView.self))
    }

    func testTrackNumber() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.id = "place-sstat" }
        let platformStop = objects.stop { stop in stop.platformCode = "7" }
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
                stop: stop, stopSequence: 0, alert: nil,
                schedule: schedule, prediction: prediction, predictionStop: platformStop,
                vehicle: nil, routes: [route]
            ),
            now: now.toKotlinInstant(),
            onTapLink: { _, _, _ in },
            routeAccents: .init(route: route)
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Track 7"))
    }

    func testTargetPin() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.addingTimeInterval(5).toKotlinInstant()
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.addingTimeInterval(6).toKotlinInstant()
        }
        let route = objects.route()

        let targeted = TripStopRow(
            stop: .init(
                stop: stop, stopSequence: 0, alert: nil,
                schedule: schedule, prediction: prediction, predictionStop: nil,
                vehicle: nil, routes: [route]
            ),
            now: now.toKotlinInstant(),
            onTapLink: { _, _, _ in },
            routeAccents: TripRouteAccents(route: route),
            targeted: true
        )

        XCTAssertNotNil(try targeted.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "stop-pin-indicator"
        }))

        let notTargeted = TripStopRow(
            stop: .init(
                stop: stop, stopSequence: 0, alert: nil,
                schedule: schedule, prediction: prediction, predictionStop: nil,
                vehicle: nil, routes: [route]
            ),
            now: now.toKotlinInstant(),
            onTapLink: { _, _, _ in },
            routeAccents: TripRouteAccents(route: route)
        )

        XCTAssertThrowsError(try notTargeted.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "stop-pin-indicator"
        }))
    }

    func testAccessibility() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in stop.name = "stop" }
        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.addingTimeInterval(5).toKotlinInstant()
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.addingTimeInterval(6).toKotlinInstant()
        }
        let route = objects.route()

        let stopEntry = TripDetailsStopList.Entry(
            stop: stop, stopSequence: 0, alert: nil,
            schedule: schedule, prediction: prediction, predictionStop: nil,
            vehicle: nil, routes: [route]
        )

        let basicRow = TripStopRow(
            stop: stopEntry,
            now: now.toKotlinInstant(),
            onTapLink: { _, _, _ in },
            routeAccents: TripRouteAccents(route: route)
        )
        XCTAssertNotNil(try basicRow.inspect().find(viewWithAccessibilityLabel: "stop"))

        let selectedRow = TripStopRow(
            stop: stopEntry,
            now: now.toKotlinInstant(),
            onTapLink: { _, _, _ in },
            routeAccents: TripRouteAccents(route: route),
            targeted: true
        )
        XCTAssertNotNil(try selectedRow.inspect().find(viewWithAccessibilityLabel: "stop, selected stop"))

        let firstRow = TripStopRow(
            stop: stopEntry,
            now: now.toKotlinInstant(),
            onTapLink: { _, _, _ in },
            routeAccents: TripRouteAccents(route: route),
            firstStop: true
        )
        XCTAssertNotNil(try firstRow.inspect().find(viewWithAccessibilityLabel: "stop, first stop"))

        let selectedFirstRow = TripStopRow(
            stop: stopEntry,
            now: now.toKotlinInstant(),
            onTapLink: { _, _, _ in },
            routeAccents: TripRouteAccents(route: route),
            targeted: true,
            firstStop: true
        )
        XCTAssertNotNil(try selectedFirstRow.inspect().find(
            viewWithAccessibilityLabel: "stop, selected stop, first stop"
        ))
    }
}
