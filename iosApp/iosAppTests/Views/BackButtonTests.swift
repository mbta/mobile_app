//
//  BackButtonTests.swift
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

final class BackButtonTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testBackButtonTap() throws {
        let exp = XCTestExpectation(description: "Back button pressed")
        let sut = BackButton {
            exp.fulfill()
        }
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "Back"))
        try sut.inspect().button().tap()
        wait(for: [exp], timeout: 1)
    }
}
