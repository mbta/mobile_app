//
//  iosAppUITests.swift
//  iosAppUITests
//
//  Created by Brady, Kayla on 12/28/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import XCTest

final class IosAppUITests: XCTestCase {
    func testExample() throws {
        let app = XCUIApplication()
        app.launch()
        let greeting = app.staticTexts.firstMatch
        XCTAssertTrue(greeting.label.hasPrefix("Hello"), "When the app opens, greeting should be displayed")
    }

    func testTimeoutConfiguration() throws {
        let app = XCUIApplication()
        app.launch()
        let missingButton = app.buttons.firstMatch
        XCTAssertTrue(missingButton.waitForExistence(timeout: 120), "When a timeout is > 60 seconds, the default max setting of 60 seconds is still enforced")
    }

    func testLaunchPerformance() throws {
        if #available(macOS 10.15, iOS 13.0, tvOS 13.0, watchOS 7.0, *) {
            // This measures how long it takes to launch your application.
            measure(metrics: [XCTApplicationLaunchMetric()]) {
                XCUIApplication().launch()
            }
        }
    }
}
