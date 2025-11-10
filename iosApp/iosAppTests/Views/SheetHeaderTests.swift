//
//  SheetHeaderTests.swift
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

final class SheetHeaderTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testIncludesBackButtonWhenGivenAction() throws {
        let backExp = XCTestExpectation(description: "Back button pressed")
        let closeExp = XCTestExpectation(description: "Close button pressed")
        let sut = SheetHeader(
            title: "Header Text",
            navCallbacks: .init(
                onBack: { backExp.fulfill() },
                onClose: { closeExp.fulfill() },
                backButtonPresentation: .header
            )
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Header Text"))
        try sut.inspect().find(viewWithAccessibilityLabel: "Back").button().tap()
        try sut.inspect().find(viewWithAccessibilityLabel: "Close").button().tap()
        wait(for: [backExp, closeExp], timeout: 1)
    }

    func testNoBackButtonWhenNoAction() throws {
        let sut = SheetHeader(
            title: "Header Text",
            navCallbacks: .init(onBack: nil, onClose: nil, backButtonPresentation: .header)
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Header Text"))
        XCTAssertThrowsError(try sut.inspect().find(ActionButton.self))
    }

    func testIncludesRightActionContents() throws {
        let sut = SheetHeader(
            title: "Header Text",
            navCallbacks: .companion.empty,
            rightActionContents: { Text("Hello") }
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Hello"))
    }
}
