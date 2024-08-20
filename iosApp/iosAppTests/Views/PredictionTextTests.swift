//
//  PredictionTextTests.swift
//  iosAppTests
//
//  Created by Jack Curtis on 8/13/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
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
            try sut.inspect().find(text: "in 24 min")
                .accessibilityLabel().string(locale: Locale(identifier: "en"))
        )
    }

    func testPredictionTextGreaterThanOneHour() {
        let sut = PredictionText(
            minutes: 124
        )
        XCTAssertEqual(
            "in 2 hr 4 min",
            try sut.inspect().find(text: "in 2 hr 4 min")
                .accessibilityLabel().string(locale: Locale(identifier: "en"))
        )
    }
}
