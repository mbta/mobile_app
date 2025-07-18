//
//  RoutePickerRootRowTests.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/17/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class RoutePickerRootRowTests: XCTestCase {
    func testBasic() {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { route in
            route.longName = "Red Line"
            route.type = RouteType.heavyRail
        }

        let sut = RoutePickerRootRow(route: RouteCardData.LineOrRoute.route(route), onTap: {})
        XCTAssertNotNil(try sut.inspect().find(text: "Red Line"))
    }

    func testLine() {
        let objects = TestData.clone()
        let line = objects.getLine(id: "line-Green")
        let allRoutes = (objects.routes.allValues as? [Route]) ?? []
        let routes = Set(allRoutes.filter { $0.lineId == line.id })

        let sut = RoutePickerRootRow(route: RouteCardData.LineOrRoute.line(line, routes), onTap: {})
        XCTAssertNotNil(try sut.inspect().find(text: "Green Line"))
    }

    func testTap() throws {
        let objects = ObjectCollectionBuilder()
        let route =
            objects.route { route in
                route.longName = "Blue Line"
                route.shortName = "Blue"
                route.type = RouteType.heavyRail
            }

        var tapped = false
        let sut = RoutePickerRootRow(
            route: RouteCardData.LineOrRoute.route(route),
            onTap: { tapped = true }
        )
        try sut.inspect().find(button: "Blue Line").tap()
        XCTAssertTrue(tapped)
    }

    func testPath() {
        let sut = RoutePickerRootRow(
            path: RoutePickerPath.Silver(),
            onTap: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Bus"))
        XCTAssertNotNil(try sut.inspect().find(text: "Silver Line"))
    }

    func testLabel() {
        let sut = RoutePickerRootRow(
            routeType: RouteType.ferry,
            routeColor: Color.cyan,
            textColor: Color.pink,
            onTap: {},
            label: { AnyView(Text("Label text")) }
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Label text"))
    }
}
