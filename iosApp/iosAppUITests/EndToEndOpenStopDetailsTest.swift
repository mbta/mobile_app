//
//  EndToEndOpenStopDetailsTest.swift
//  iosAppUITests
//
//  Created by Horn, Melody on 2024-08-08.
//  Copyright © 2024 MBTA. All rights reserved.
//

import XCTest

final class EndToEndOpenStopDetailsTest: XCTestCase {
    let app = XCUIApplication()

    override func setUpWithError() throws {
        app.launchArguments = ["--e2e-mocks"]
        app.resetAuthorizationStatus(for: .location)
        XCUIDevice.shared.location = XCUILocation(location: .init(latitude: 42.356395, longitude: -71.062424))

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false

        // In UI tests it’s important to set the initial state - such as interface orientation - required for your tests
        // before they run. The setUp method is a good place to do this.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    func testOpenStopDetails() throws {
        addUIInterruptionMonitor(
            withDescription: "Location alert",
            handler: { (_: XCUIElement) -> Bool in self.acceptLocationPermissionAlert(timeout: 10)
                return true
            }
        )
        // UI tests must launch the application that they test.
        app.launch()

//        acceptLocationPermissionAlert(timeout: 10)

        // Use XCTAssert and related functions to verify your tests produce the correct results.
        app.staticTexts["Alewife"].tap()

        XCTAssertFalse(app.staticTexts["Nearby Transit"].exists)
        XCTAssertTrue(app.staticTexts["Northbound to"].exists)
        XCTAssertTrue(app.staticTexts["Southbound to"].exists)
        XCTAssertTrue(app.staticTexts["Ashmont/Braintree"].exists)
    }
}
