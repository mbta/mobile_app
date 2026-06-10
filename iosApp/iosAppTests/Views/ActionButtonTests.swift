//
//  ActionButtonTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 6/10/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class ActionButtonTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testActionButtonTap() throws {
        let exp = XCTestExpectation(description: "Action button pressed")
        let sut = ActionButton(kind: .close) {
            exp.fulfill()
        }
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "Close"))
        try sut.inspect().find(ViewType.Button.self).tap()
        wait(for: [exp], timeout: 1)
    }
}
