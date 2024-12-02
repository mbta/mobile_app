//
//  PredictionRowViewTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-09-10.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class PredictionRowViewTests: XCTestCase {
    func testSecondaryAlert() throws {
        let sut = PredictionRowView(
            predictions: RealtimePatterns.FormatNone(secondaryAlert: .init(
                iconName: "alert-large-bus-issue"
            )),
            pillDecoration: .none,
            destination: { EmptyView() }
        )

        XCTAssertEqual(try sut.inspect().find(ViewType.Image.self).actualImage().name(), "alert-large-bus-issue")
        XCTAssertEqual(try sut.inspect().find(ViewType.Image.self).accessibilityLabel().string(), "Alert")
    }
}
