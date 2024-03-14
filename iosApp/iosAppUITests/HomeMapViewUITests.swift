//
//  HomeMapViewUITests.swift
//  iosAppUITests
//
//  Created by Simon, Emma on 3/13/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import XCTest

final class HomeMapViewUITests: XCTestCase {
    var app: XCUIApplication? = nil

    override func setUp() {
        executionTimeAllowance = 60
    }

    override func setUpWithError() throws {
        app = XCUIApplication()
        app!.resetAuthorizationStatus(for: .location)
        app!.launchArguments = ["-testing"]
        XCUIDevice.shared.location = XCUILocation(location: .init(latitude: 42.356395, longitude: -71.062424))
        continueAfterFailure = false
        app!.launch()
    }

    override func tearDownWithError() throws {
        app!.terminate()
    }

    func testRecentersToUserLocation() throws {
        acceptLocationPermissionAlert(timeout: 5)

        let map = app!.otherElements.matching(identifier: "transitMap").element
        map.isAccessibilityElement = true
        app!.forceTap()
        XCTAssert(map.waitForExistence(timeout: 10))

        let recenterButton = app!.images.matching(identifier: "mapRecenterButton").element
        XCTAssertFalse(recenterButton.exists)

        map.swipeDown()
        XCTAssert(recenterButton.waitForExistence(timeout: 3))

        recenterButton.tap()
        XCTAssertFalse(recenterButton.exists)
    }

    func testNoRecenterWithNoLocation() throws {
        denyLocationPermissionAlert(timeout: 5)

        let map = app!.otherElements.matching(identifier: "transitMap").element
        XCTAssert(map.waitForExistence(timeout: 5))

        let recenterButton = app!.images.matching(identifier: "mapRecenterButton").element
        XCTAssertFalse(recenterButton.exists)

        map.swipeDown()
        XCTAssertFalse(recenterButton.exists)
    }
}

extension XCUIElement {
    func labelContains(text: String) -> Bool {
        let predicate = NSPredicate(format: "label CONTAINS %@", text)
        return staticTexts.matching(predicate).firstMatch.exists
    }

    func forceTap() {
        if isHittable {
            tap()
        } else {
            coordinate(withNormalizedOffset: CGVectorMake(0.0, 0.0)).tap()
        }
    }
}

extension XCTestCase {
    private func tapLocationAlertButton(label: String, timeout: TimeInterval) {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")

        let alert = springboard.alerts.firstMatch
        XCTAssert(alert.waitForExistence(timeout: timeout))
        XCTAssert(alert.labelContains(text: "use your location?"))

        let button = alert.buttons[label]
        XCTAssert(button.waitForExistence(timeout: timeout))
        button.tap()
    }

    func acceptLocationPermissionAlert(timeout: TimeInterval) {
        tapLocationAlertButton(label: "Allow Once", timeout: timeout)
    }

    func denyLocationPermissionAlert(timeout: TimeInterval) {
        // Note that this uses a fancy ’ (U+2019) apostrophe rather than a ' (U+0027)
        tapLocationAlertButton(label: "Don’t Allow", timeout: timeout)
    }
}
