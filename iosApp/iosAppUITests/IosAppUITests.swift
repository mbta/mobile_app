//
//  IosAppUITests.swift
//  iosAppUITests
//
//  Created by Brady, Kayla on 12/28/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import XCTest

final class IosAppUITests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 180
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
        app.launchArguments = ["--skip-map"]
        app.launch()
        XCTAssertNotNil(app.otherElements["About this map"].label)
        print(app.debugDescription)
    }
}
