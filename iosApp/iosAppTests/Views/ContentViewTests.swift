//
//  contentViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 12/28/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import XCTest
import ViewInspector
@testable import iosApp

final class ContentViewTests: XCTestCase {

    override func setUp() {
        executionTimeAllowance = 60
    }

    func testExample() throws {
        let sut = ContentView()

        let greeting = try sut.inspect().text().string()
        XCTAssertTrue(greeting.hasPrefix("Hello"), "displays greeting")
    }
}
