//
//  NonNilModifierTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 3/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import SwiftUI
import ViewInspector
import XCTest

final class NonNilModifierTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testNilNotReplaced() throws {
        let value: String? = nil
        let sut = Text("nil content").replaceWhen(value) { _ in
            Text("not nil content")
        }
        let modifier = try sut.inspect().implicitAnyView().text().modifier(NonNilReplaceModifier<String, Text>.self)
        XCTAssertNotNil(try modifier.implicitAnyView().viewModifierContent())
        XCTAssertThrowsError(try modifier.find(text: "not nil content"))
    }

    @MainActor func testNonNilReplaced() throws {
        let value: String? = "display non nil"
        let sut = Text("nil content").replaceWhen(value) { _ in
            Text("not nil content")
        }
        let modifier = try sut.inspect().implicitAnyView().text().modifier(NonNilReplaceModifier<String, Text>.self)
        XCTAssertNotNil(try modifier.find(text: "not nil content"))
        XCTAssertThrowsError(try modifier.implicitAnyView().viewModifierContent())
    }

    @MainActor func testNilDoesNotDisplayAbove() throws {
        let value: String? = nil
        let sut = Text("nil content").putAboveWhen(value) { _ in
            Text("not nil content")
        }
        let modifier = try sut.inspect().implicitAnyView().text().modifier(NonNilAboveModifier<String, Text>.self)
        XCTAssertNotNil(try modifier.implicitAnyView().vStack().first!.isAbsent)
        XCTAssertNotNil(try modifier.implicitAnyView().vStack().last!.viewModifierContent())
        XCTAssertThrowsError(try modifier.find(text: "not nil content"))
    }

    @MainActor func testNonNilDisplaysAbove() throws {
        let value: String? = "display non nil"
        let sut = Text("nil content").putAboveWhen(value) { _ in
            Text("not nil content")
        }
        let modifier = try sut.inspect().implicitAnyView().text().modifier(NonNilAboveModifier<String, Text>.self)
        XCTAssertNotNil(try modifier.implicitAnyView().vStack().first!.text())
        XCTAssertNotNil(try modifier.implicitAnyView().vStack().last!.viewModifierContent())
        XCTAssertNotNil(try modifier.find(text: "not nil content"))
    }

    @MainActor func testNilDoesNotDisplayBelow() throws {
        let value: String? = nil
        let sut = Text("nil content").putBelowWhen(value) { _ in
            Text("not nil content")
        }
        let modifier = try sut.inspect().implicitAnyView().text().modifier(NonNilBelowModifier<String, Text>.self)
        XCTAssertNotNil(try modifier.implicitAnyView().vStack().first!.viewModifierContent())
        XCTAssertNotNil(try modifier.implicitAnyView().vStack().last!.isAbsent)
        XCTAssertThrowsError(try modifier.find(text: "not nil content"))
    }

    @MainActor func testNonNilDisplaysBelow() throws {
        let value: String? = "display non nil"
        let sut = Text("nil content").putBelowWhen(value) { _ in
            Text("not nil content")
        }
        let modifier = try sut.inspect().implicitAnyView().text().modifier(NonNilBelowModifier<String, Text>.self)
        XCTAssertNotNil(try modifier.implicitAnyView().vStack().first!.viewModifierContent())
        XCTAssertNotNil(try modifier.implicitAnyView().vStack().last!.text())
        XCTAssertNotNil(try modifier.find(text: "not nil content"))
    }
}
