//
//  RouteModeLabelTests.swift
//  iosApp
//
//  Created by esimon on 8/13/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class RouteModeLabelTests: XCTestCase {
    func testNameAndType() throws {
        let name = "1"
        let type = RouteType.bus

        XCTAssertEqual("1 bus", routeModeLabel(name: name, type: type))
    }

    func testPlural() throws {
        let name = "Red Line"
        let type = RouteType.heavyRail

        XCTAssertEqual("Red Line trains", routeModeLabel(name: name, type: type, isOnly: false))
    }

    func testNameOnly() throws {
        let name = "Needham Line"

        XCTAssertEqual("Needham Line", routeModeLabel(name: name, type: nil))
    }

    func testTypeOnly() throws {
        let type = RouteType.ferry

        XCTAssertEqual("ferry", routeModeLabel(name: nil, type: type))
    }

    func testEmpty() throws {
        XCTAssertEqual("", routeModeLabel(name: nil, type: nil))
    }

    func testRoute() throws {
        let route = ObjectCollectionBuilder.Single.shared.route { route in
            route.longName = "Orange Line"
            route.type = .heavyRail
        }
        XCTAssertEqual("Orange Line train", routeModeLabel(route: route))
    }

    func testLineAndRoute() throws {
        let line = ObjectCollectionBuilder.Single.shared.line { line in
            line.longName = "Green Line"
        }
        let route = ObjectCollectionBuilder.Single.shared.route { route in
            route.longName = "Green Line C"
            route.type = .lightRail
        }
        XCTAssertEqual("Green Line train", routeModeLabel(line: line, route: route))
    }

    func testLineOrRoute() throws {
        let line = ObjectCollectionBuilder.Single.shared.line { line in
            line.longName = "Silver Line"
        }
        let route = ObjectCollectionBuilder.Single.shared.route { route in
            route.longName = "SL3"
            route.type = .bus
        }
        let lineOrRoute = LineOrRoute.line(line, [route])
        XCTAssertEqual("Silver Line bus", routeModeLabel(lineOrRoute: lineOrRoute))
    }
}
