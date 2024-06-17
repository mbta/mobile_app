//
//  RouteHeaderTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 6/13/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import shared
import ViewInspector
import XCTest

final class RouteHeaderTests: XCTestCase {
    func testSubway() throws {
        let route = ObjectCollectionBuilder.Single().route { route in
            route.type = .heavyRail
            route.longName = "Red"
        }

        let sut = RouteHeader(route: route)
        XCTAssertNotNil(try sut.inspect().find(text: "Red"))
    }

    func testBus() throws {
        let route = ObjectCollectionBuilder.Single().route { route in
            route.type = .bus
            route.shortName = "66"
        }

        let sut = RouteHeader(route: route)
        XCTAssertNotNil(try sut.inspect().find(text: "66"))
    }

    func testCR() throws {
        let route = ObjectCollectionBuilder.Single().route { route in
            route.type = .commuterRail
            route.longName = "A/B"
        }

        let sut = RouteHeader(route: route)
        XCTAssertNotNil(try sut.inspect().find(text: "A / B"))
    }
}
