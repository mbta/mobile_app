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
                schedule: schedule, prediction: prediction,
                vehicle: nil, routes: [route]
            ),
            now: now.toKotlinInstant(),
            onTapLink: { _, _, _ in },
            route: route
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
                schedule: schedule, prediction: prediction,
                vehicle: nil, routes: [route]
            ),
            now: now.toKotlinInstant(),
            onTapLink: { _, _, _ in },
            route: route
        )

        XCTAssertNotNil(try sut.inspect().find(UpcomingTripView.self))
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
                schedule: schedule, prediction: prediction,
                vehicle: nil, routes: [route]
            ),
            now: now.toKotlinInstant(),
            onTapLink: { _, _, _ in },
            route: route,
            targeted: true
        )

        XCTAssertNotNil(try targeted.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "stop-pin-indicator"
        }))

        let notTargeted = TripStopRow(
            stop: .init(
                stop: stop, stopSequence: 0, alert: nil,
                schedule: schedule, prediction: prediction,
                vehicle: nil, routes: [route]
            ),
            now: now.toKotlinInstant(),
            onTapLink: { _, _, _ in },
            route: route
        )

        XCTAssertThrowsError(try notTargeted.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "stop-pin-indicator"
        }))
    }
}
