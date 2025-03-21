//
//  UpcomingTripViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 5/22/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class UpcomingTripViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!
    }

    func testFirstBoardingAccessibilityLabel() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Boarding()),
            routeType: .heavyRail,
            isFirst: true
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "train boarding now"))
    }

    func testBoardingAccessibilityLabel() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Boarding()),
            routeType: .heavyRail,
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and boarding now"))
    }

    func testFirstArrivingAccessibilityLabel() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Arriving()),
            routeType: .heavyRail,
            isFirst: true
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "train arriving now"))
    }

    func testArrivingAccessibilityLabel() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Arriving()),
            routeType: .heavyRail,
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and arriving now"))
    }

    func testFirstDistantAccessibilityLabel() throws {
        let date = ISO8601DateFormatter().date(from: "2024-05-01T20:00:00Z")!
        let text: any View = UpcomingTripAccessibilityFormatters().distantFutureFirst(date: date, vehicleText: "trains")
        let foundText: String = try text.inspect().text()
            .string(locale: Locale(identifier: "en"))

        XCTAssertEqual("trains arriving at 4:00 PM", foundText)
    }

    func testDistantAccessibilityLabel() throws {
        let date = ISO8601DateFormatter().date(from: "2024-05-01T20:00:00Z")!
        let text: any View = UpcomingTripAccessibilityFormatters().distantFutureOther(date: date)
        let foundText: String = try text.inspect().text()
            .string(locale: Locale(identifier: "en"))

        XCTAssertEqual("and at 4:00 PM", foundText)
    }

    func testTimeWithStatus() throws {
        let date = ISO8601DateFormatter().date(from: "2024-05-01T20:00:00Z")!
        let sut = UpcomingTripView(
            prediction: .some(.TimeWithStatus(
                predictionTime: date.toKotlinInstant(),
                status: "All aboard",
                headline: true
            )),
            routeType: .commuterRail
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "train arriving at 4:00\u{202F}PM"))
        XCTAssertNotNil(try sut.inspect().find(text: "All aboard"))
    }

    func testFirstScheduledAccessibilityLabel() throws {
        let date = ISO8601DateFormatter().date(from: "2024-05-01T20:00:00Z")!
        let text: any View = UpcomingTripAccessibilityFormatters().scheduleTimeFirst(date: date, vehicleText: "buses")
        let foundText: String = try text.inspect().text()
            .string(locale: Locale(identifier: "en"))

        XCTAssertEqual("buses arriving at 4:00 PM scheduled", foundText)
    }

    func testScheduledAccessibilityLabel() throws {
        let date = ISO8601DateFormatter().date(from: "2024-05-01T20:00:00Z")!
        let text: any View = UpcomingTripAccessibilityFormatters().scheduleTimeOther(date: date)
        let foundText: String = try text.inspect().text()
            .string(locale: Locale(identifier: "en"))

        XCTAssertEqual("and at 4:00 PM scheduled", foundText)
    }

    func testFirstPredictedAccessibilityLabel() throws {
        let sut = UpcomingTripView(prediction: .some(.Minutes(minutes: 5)),
                                   routeType: .heavyRail,
                                   isFirst: true,
                                   isOnly: false)
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "trains arriving in 5 min"))
    }

    func testPredictedAccessibilityLabel() throws {
        let sut = UpcomingTripView(prediction: .some(.Minutes(minutes: 5)), isFirst: false)
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and in 5 min"))
    }

    func testPredictedHourAccessibilityLabel() throws {
        let sut = UpcomingTripView(prediction: .some(.Minutes(minutes: 67)), isFirst: false)
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and in 1 hr 7 min"))
    }

    func testCancelledAccessibilityLabel() throws {
        let date = ISO8601DateFormatter().date(from: "2024-05-01T20:00:00Z")!.toKotlinInstant()
        let sut = UpcomingTripView(prediction: .some(.Cancelled(scheduledTime: date)),
                                   routeType: .heavyRail,
                                   isFirst: false,
                                   isOnly: false)
        let predictionView = try sut.inspect().find(ViewType.HStack.self)
        XCTAssertEqual(
            "and at 4:00 PM cancelled",
            try predictionView.accessibilityLabel().string(locale: Locale(identifier: "en"))
        )
    }

    func testShuttleAccessibilityLabel() throws {
        let sut = UpcomingTripView(
            prediction: .disruption(
                .init(alert: ObjectCollectionBuilder.Single.shared.alert { $0.effect = .shuttle }),
                iconName: "alert-borderless-shuttle"
            ),
            isFirst: false
        )
        XCTAssertEqual(
            "Shuttle buses replace service",
            try sut.inspect().find(text: "Shuttle Bus")
                .accessibilityLabel().string(locale: Locale(identifier: "en"))
        )
    }

    func testSuspensionAccessibilityLabel() throws {
        let sut = UpcomingTripView(
            prediction: .disruption(
                .init(alert: ObjectCollectionBuilder.Single.shared.alert { $0.effect = .suspension }),
                iconName: "alert-borderless-suspension"
            ),
            isFirst: false
        )
        XCTAssertEqual(
            "Service suspended",
            try sut.inspect().find(text: "Suspension")
                .accessibilityLabel().string(locale: Locale(identifier: "en"))
        )
    }

    func testFirstCommuterRailAccessibilityLabel() throws {
        let date = ISO8601DateFormatter().date(from: "2024-05-01T20:00:00Z")!
        let text: any View = UpcomingTripAccessibilityFormatters().predictionTimeFirst(
            date: date,
            vehicleText: "trains"
        )
        let foundText: String = try text.inspect().text()
            .string(locale: Locale(identifier: "en"))

        XCTAssertEqual(
            "trains arriving at 4:00 PM",
            foundText
        )
    }

    func testCommuterRailAccessibilityLabel() throws {
        let date = ISO8601DateFormatter().date(from: "2024-05-01T20:00:00Z")!
        let text: any View = UpcomingTripAccessibilityFormatters().predictionTimeOther(date: date)
        let foundText: String = try text.inspect().text()
            .string(locale: Locale(identifier: "en"))

        XCTAssertEqual(
            "and at 4:00 PM",
            foundText
        )
    }

    func testDisruptionIconName() throws {
        let alert = ObjectCollectionBuilder.Single.shared.alert { $0.effect = .snowRoute }
        let disruption = UpcomingFormatDisruption(alert: alert, mapStopRoute: .bus)
        let sut = UpcomingTripView(prediction: .disruption(.init(alert: alert), iconName: disruption.iconName))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "alert-large-bus-issue"
        }))
    }
}
