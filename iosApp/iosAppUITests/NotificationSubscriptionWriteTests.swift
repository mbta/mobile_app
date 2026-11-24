//
//  NotificationSubscriptionWriteTests.swift
//  iosAppUITests
//
//  Created by Brady, Kayla on 12/28/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import CoreLocation
import XCTest

final class NotificationSubscriptionWriteTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 180
        // Continues even if one audit fails to show all issues
        continueAfterFailure = true
    }

    override func tearDown() {
        continueAfterFailure = false
    }

    func testShowsFavoritesStoredOnDevice() {
        XCUIDevice.shared.location = XCUILocation(location: CLLocation(latitude: 42.356395, longitude: -71.062424))

        addNotificationsPermissionPromptHandler()
        addLocationPermissionPromptHandler()

        let app = XCUIApplication()
        app.launchArguments = ["--e2e-mocks", "--skip-map", "--mock-favorites"]
        app.activate()
        app.launch()

        app.buttons["Favorites"].firstMatch.tapAfter()
        XCTAssertTrue(app.staticTexts["Davis"].waitForExistence(timeout: 10))
    }
}
