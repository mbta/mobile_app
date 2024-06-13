//
//  RouteSectionTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 6/13/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class RouteSectionTests: XCTestCase {
    func testRouteContents() throws {
        let route = ObjectCollectionBuilder.Single().route { route in
            route.longName = "Red"
        }

        let sut = RouteSection(route: route, pinned: false, onPin: { _ in }) {
            Text("Route details")
        }

        XCTAssertNotNil(try sut.inspect().find(text: "Red"))
        XCTAssertNotNil(try sut.inspect().find(text: "Route details"))
    }

    func testPinRoute() throws {
        let route = ObjectCollectionBuilder.Single().route { route in
            route.longName = "Red"
        }

        let pinRouteExp = XCTestExpectation(description: "pinRoute called")

        func onPin(_: String) {
            pinRouteExp.fulfill()
        }

        let sut = RouteSection(route: route, pinned: false, onPin: onPin) {
            Text("Route details")
        }

        let button =
            try sut.inspect().find(viewWithAccessibilityIdentifier: "pinButton").button()

        try button.tap()
        wait(for: [pinRouteExp], timeout: 1)
    }
}
