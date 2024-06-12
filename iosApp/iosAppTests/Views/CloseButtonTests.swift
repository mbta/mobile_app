//
//  CloseButtonTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 6/10/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation

@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class CloseButtonTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testCloseButtonTap() throws {
        let exp = XCTestExpectation(description: "Close button pressed")
        let sut = CloseButton {
            exp.fulfill()
        }
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "Close"))
        try sut.inspect().button().tap()
        wait(for: [exp], timeout: 1)
    }
}
