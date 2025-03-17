//
//  PredictionTextTests.swift
//  iosAppTests
//
//  Created by Jack Curtis on 8/13/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class PredictionTextTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!
    }

    func testPredictionTextLessThanOneHour() {
        let sut = PredictionText(
            minutes: 24
        )

        XCTAssertEqual(
            "in 24 min",
            try sut.inspect().find(text: "24 min")
                .accessibilityLabel().string(locale: Locale(identifier: "en"))
        )
    }

    func testPredictionTextGreaterThanOneHour() {
        let sut = PredictionText(
            minutes: 124
        )
        XCTAssertEqual(
            "in 2 hr 4 min",
            try sut.inspect().find(text: "2 hr 4 min")
                .accessibilityLabel().string(locale: Locale(identifier: "en"))
        )
    }

    func testPredictionTextOneHour() {
        let sut = PredictionText(
            minutes: 60
        )
        XCTAssertEqual(
            "in 1 hr",
            try sut.inspect().find(text: "1 hr")
                .accessibilityLabel().string(locale: Locale(identifier: "en"))
        )
    }
}
