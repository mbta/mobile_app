//
//  SheetHeaderTests.swift
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

final class SheetHeaderTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testIncludesCloseButtonWhenGivenAction() throws {
        let exp = XCTestExpectation(description: "Back button pressed")
        let sut = SheetHeader(onClose: { exp.fulfill() }, title: "Header Text")

        XCTAssertNotNil(try sut.inspect().find(text: "Header Text"))
        try sut.inspect().find(CloseButton.self).button().tap()
        wait(for: [exp], timeout: 1)
    }

    func testNoCloseButtonWhenNoAction() throws {
        let exp = XCTestExpectation(description: "Back button pressed")
        let sut = SheetHeader(title: "Header Text")
        XCTAssertNotNil(try sut.inspect().find(text: "Header Text"))
        XCTAssertThrowsError(try sut.inspect().find(CloseButton.self))
    }
}
