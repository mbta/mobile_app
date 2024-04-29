//
//  DebouncerTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 3/21/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp

import shared
import XCTest

final class DebouncerTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testMethodIsDebounced() {
        let testDebouncer = Debouncer(delay: 0.5)

        let notRunExpectation = XCTestExpectation(description: "Should not run")
        notRunExpectation.isInverted = true
        let asapExpectation = XCTestExpectation(description: "Should run, but not immediately")
        asapExpectation.isInverted = true
        let runExpectation = XCTestExpectation(description: "Should run")

        testDebouncer.debounce {
            notRunExpectation.fulfill()
        }
        testDebouncer.debounce {
            asapExpectation.fulfill()
            runExpectation.fulfill()
        }

        wait(for: [asapExpectation], timeout: 0.25)
        wait(for: [runExpectation], timeout: 0.75)
    }
}
