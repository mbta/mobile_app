//
//  UpcomingTripViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 5/22/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class UpcomingTripViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testFirstBoardingAccessibilityLabel() throws {
        let sut = UpcomingTripView(prediction: .some(UpcomingTrip.FormatBoarding()), isFirst: true)
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "boarding now"))
    }

    func testBoardingAccessibilityLabel() throws {
        let sut = UpcomingTripView(prediction: .some(UpcomingTrip.FormatBoarding()), isFirst: false)
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and boarding now"))
    }

    func testFirstArrivingAccessibilityLabel() throws {
        let sut = UpcomingTripView(prediction: .some(UpcomingTrip.FormatArriving()), isFirst: true)
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "arriving now"))
    }

    func testArrivingAccessibilityLabel() throws {
        let sut = UpcomingTripView(prediction: .some(UpcomingTrip.FormatArriving()), isFirst: false)
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and arriving now"))
    }

    func testFirstDistantAccessibilityLabel() throws {
        let date = ISO8601DateFormatter().date(from: "2024-05-01T20:00:00Z")!
        let formatters = UpcomingTripAccessibilityFormatters()
        let text: any View = UpcomingTripAccessibilityFormatters().distantFuture(date: date, isFirst: true)
        let foundText: String = try text.inspect().text()
            .string(locale: Locale(identifier: "en"))

        XCTAssertEqual("arriving at 4:00 PM", foundText)
    }

    func testDistantAccessibilityLabel() throws {
        let date = ISO8601DateFormatter().date(from: "2024-05-01T20:00:00Z")!
        let formatters = UpcomingTripAccessibilityFormatters()
        let text: any View = UpcomingTripAccessibilityFormatters().distantFuture(date: date, isFirst: false)
        let foundText: String = try text.inspect().text()
            .string(locale: Locale(identifier: "en"))

        XCTAssertEqual("and at 4:00 PM", foundText)
    }

    func testFirstScheduledAccessibilityLabel() throws {
        let date = ISO8601DateFormatter().date(from: "2024-05-01T20:00:00Z")!
        let formatters = UpcomingTripAccessibilityFormatters()
        let text: any View = UpcomingTripAccessibilityFormatters().scheduled(date: date, isFirst: true)
        let foundText: String = try text.inspect().text()
            .string(locale: Locale(identifier: "en"))

        XCTAssertEqual("arriving at 4:00 PM scheduled", foundText)
    }

    func testScheduledAccessibilityLabel() throws {
        let date = ISO8601DateFormatter().date(from: "2024-05-01T20:00:00Z")!
        let formatters = UpcomingTripAccessibilityFormatters()
        let text: any View = UpcomingTripAccessibilityFormatters().scheduled(date: date, isFirst: false)
        let foundText: String = try text.inspect().text()
            .string(locale: Locale(identifier: "en"))

        XCTAssertEqual("and at 4:00 PM scheduled", foundText)
    }

    func testFirstPredictedAccessibilityLabel() throws {
        let sut = UpcomingTripView(prediction: .some(UpcomingTrip.FormatMinutes(minutes: 5)), isFirst: true)
        let predictionView = try sut.inspect().find(PredictionText.self)
        XCTAssertEqual("arriving in 5 minutes", try predictionView.accessibilityLabel().string(locale: Locale(identifier: "en")))
    }

    func testPredictedAccessibilityLabel() throws {
        let sut = UpcomingTripView(prediction: .some(UpcomingTrip.FormatMinutes(minutes: 5)), isFirst: false)
        let predictionView = try sut.inspect().find(PredictionText.self)
        XCTAssertEqual("and in 5 minutes", try predictionView.accessibilityLabel().string(locale: Locale(identifier: "en")))
    }
}
