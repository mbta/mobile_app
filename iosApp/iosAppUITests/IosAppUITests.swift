//
//  IosAppUITests.swift
//  iosAppUITests
//
//  Created by Brady, Kayla on 12/28/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import CoreLocation
import XCTest

final class IosAppUITests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 180
        // Continues even if one audit fails to show all issues
        continueAfterFailure = true
    }

    override func tearDown() {
        continueAfterFailure = false
    }

    func testLaunchPerformance() throws {
        if #available(macOS 10.15, iOS 13.0, tvOS 13.0, watchOS 7.0, *) {
            let opts = XCTMeasureOptions()
            opts.iterationCount = 1
            // This measures how long it takes to launch your application.
            measure(metrics: [XCTApplicationLaunchMetric()], options: opts) {
                XCUIApplication().launch()
            }
        }
    }

    func testMapShown() {
        let app = XCUIApplication()
        app.activate()
        app.launchArguments = ["--skip-map"]
        app.launch()
        XCTAssertNotNil(app.otherElements["About this map"].label)
        print(app.debugDescription)
    }

    func testAccessibilityNearbyToTripDetails() {
        XCUIDevice.shared.location = XCUILocation(location: CLLocation(latitude: 42.356395, longitude: -71.062424))

        let app = XCUIApplication()
        app.launchArguments = ["--e2e-mocks"]
        app.launch()

        defaultAccessibilityAudit(app)

        let prediction = app.staticTexts["Boston College"].firstMatch
        if prediction.waitForExistence(timeout: 10) {
            prediction.tap()
        } else {
            XCTFail("prediction did not display")
        }

        defaultAccessibilityAudit(app)

        let follow = app.staticTexts["Follow"]
        if follow.waitForExistence(timeout: 10) {
            follow.tap()
        } else {
            XCTFail("Follow button did not display")
        }

        defaultAccessibilityAudit(app)
    }

    func defaultAccessibilityAudit(_ app: XCUIApplication) {
        try? app.performAccessibilityAudit(for: [.elementDetection, .hitRegion, .sufficientElementDescription, .trait])
    }
}
