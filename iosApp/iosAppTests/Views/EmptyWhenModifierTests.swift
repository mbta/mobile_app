//
//  EmptyWhenModifierTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 3/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import SwiftUI
import ViewInspector
import XCTest

final class EmptyWhenModifierTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testEmptyOnFalse() throws {
        let sut = Text("hello").emptyWhen(true)
        let modifier = try sut.inspect().text().modifier(EmptyWhenModifier.self)

        XCTAssertNotNil(try modifier.emptyView())
        XCTAssertThrowsError(try modifier.viewModifierContent())
    }

    @MainActor func testNotEmptyOnTrue() throws {
        let sut = Text("hello").emptyWhen(false)
        let modifier = try sut.inspect().text().modifier(EmptyWhenModifier.self)

        XCTAssertNotNil(try modifier.viewModifierContent())
        XCTAssertThrowsError(try modifier.emptyView())
    }
}
