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
        XCUIDevice.shared.location = XCUILocation(location: .init(latitude: 42.356395, longitude: -71.062424))

        continueAfterFailure = false
    }

    func testOpenStopDetails() throws {
        app.activate()
        app.launch()
        let alewifeHeadsign = app.staticTexts["Alewife"]
        XCTAssert(alewifeHeadsign.waitForExistence(timeout: 30))

        alewifeHeadsign.tap()

        let rlPill = app.staticTexts["RL"]
        XCTAssert(rlPill.waitForExistence(timeout: 30))

        XCTAssertFalse(app.staticTexts["Nearby Transit"].exists)
        XCTAssertTrue(app.staticTexts["Northbound to"].exists)
        XCTAssertTrue(app.staticTexts["Southbound to"].exists)
        XCTAssertTrue(app.staticTexts["Ashmont/Braintree"].exists)
    }
}
