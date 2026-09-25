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

    func testLaunchPerformance() {
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

        addNotificationsPermissionPromptHandler()
        addLocationPermissionPromptHandler()

        let app = XCUIApplication()
        app.launchArguments = ["--e2e-mocks", "--skip-map"]
        app.launch()
        app.tap() // trigger handling permission prompts

        let prediction = app.staticTexts["Boston College"].firstMatch
        if prediction.waitForExistence(timeout: 10) {
            defaultAccessibilityAudit(app)
            prediction.tap()
        } else {
            XCTFail("prediction did not display")
        }

        let follow = app.staticTexts["Follow"]
        if follow.waitForExistence(timeout: 10) {
            defaultAccessibilityAudit(app)
            follow.tap()
        } else {
            XCTFail("Follow button did not display")
        }

        let live = app.staticTexts["Live"]
        if live.waitForExistence(timeout: 10) {
            defaultAccessibilityAudit(app)
        } else {
            XCTFail("Trip details did not display")
        }
    }

    func testAccessibilityFavoritesToNotificationSettings() {
        XCUIDevice.shared.location = XCUILocation(location: CLLocation(latitude: 42.356395, longitude: -71.062424))

        addNotificationsPermissionPromptHandler()
        addLocationPermissionPromptHandler()

        let app = XCUIApplication()
        app.launchArguments = ["--e2e-mocks", "--skip-map"]
        app.launch()
        app.tap() // trigger handling permission prompts

        app.buttons["Favorites"].firstMatch.tapAfterWaiting()

        app.buttons["Add favorite stops"].firstMatch.tapAfterWaiting()

        app.buttons["Red Line"].firstMatch.tapAfterWaiting()
        app.buttons.element(matching: .init(format: "label CONTAINS[c] %@", "Davis")).tapAfterWaiting()
        app.switches["Get disruption notifications"].firstMatch.tapAfterWaiting()
        app.buttons["Add another time period"].firstMatch.tapAfterWaiting()

        defaultAccessibilityAudit(app)
    }

    func defaultAccessibilityAudit(_ app: XCUIApplication) {
        try? app.performAccessibilityAudit(for: [.elementDetection, .hitRegion, .sufficientElementDescription, .trait])
    }

    func addNotificationsPermissionPromptHandler() {
        addUIInterruptionMonitor(withDescription: "Push Notifications Alert") { alert -> Bool in
            if alert.buttons["Allow"].exists {
                alert.buttons["Allow"].tap()
                return true
            }
            return false
        }
    }

    func addLocationPermissionPromptHandler() {
        let monitor = addUIInterruptionMonitor(withDescription: "Location Permission Alert") { alert -> Bool in
            let allowLocationButton = alert.buttons["Allow Once"]
            if allowLocationButton.exists {
                allowLocationButton.tap()
                return true
            }
            return false
        }
    }
}

extension XCUIElement {
    /// Waits for the element to exist and taps it. Fails the test if it doesn't appear.
    func tapAfterWaiting(timeout: TimeInterval = 10.0, file: StaticString = #file, line: UInt = #line) {
        if waitForExistence(timeout: timeout) {
            tap()
        } else {
            XCTFail("Element \(description) did not appear within \(timeout) seconds.", file: file, line: line)
        }
    }
}
