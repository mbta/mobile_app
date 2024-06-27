//
//  DirectionLabelTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 6/27/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class DirectionLabelTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testBasicDirection() throws {
        let sut = DirectionLabel(direction: Direction(name: "Inbound", destination: "South Station", id: 1))
        XCTAssertNotNil(try sut.inspect().find(text: "Inbound"))
        XCTAssertNotNil(try sut.inspect().find(text: "South Station"))
    }

    func testDirectionReformatting() throws {
        let sut = DirectionLabel(direction: Direction(name: "East", destination: "Park St & North", id: 1))
        XCTAssertNotNil(try sut.inspect().find(text: "Eastbound to"))
        XCTAssertNotNil(try sut.inspect().find(text: "Park St & North"))
    }
}
