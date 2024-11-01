//
//  LoadingCardTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 6/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import SwiftUI
import ViewInspector
import XCTest

final class LoadingCardTests: XCTestCase {
    func testDefault() throws {
        let sut = LoadingCard()
        XCTAssertNotNil(try sut.inspect().find(ViewType.ProgressView.self))
        XCTAssertNotNil(try sut.inspect().find(text: "Loading..."))
    }

    func testCustomMessage() throws {
        let sut = LoadingCard { Text("custom") }
        XCTAssertNotNil(try sut.inspect().find(ViewType.ProgressView.self))
        XCTAssertNotNil(try sut.inspect().find(text: "custom"))
    }
}
