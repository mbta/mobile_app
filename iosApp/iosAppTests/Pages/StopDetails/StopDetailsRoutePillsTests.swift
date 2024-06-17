//
//  StopDetailsRoutePillsTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 4/17/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsRoutePillsTests: XCTestCase {
    func testRoutePillsDisplay() throws {
        let objects = ObjectCollectionBuilder()
        let route1 = objects.route()
        let route2 = objects.route()
        let sut = StopDetailsRoutePills(
            servedRoutes: [(route: route1, line: nil), (route: route2, line: nil)],
            tapRoutePill: { _ in },
            filter: .constant(nil)
        )

        let pills = try sut.inspect().findAll(RoutePill.self)
        XCTAssertEqual(pills.count, 2)
        XCTAssertEqual(try pills.first?.actualView().route, route1)
    }

    func testRoutePillsWithFilter() throws {
        let objects = ObjectCollectionBuilder()
        let route1 = objects.route()
        let route2 = objects.route()
        let sut = StopDetailsRoutePills(
            servedRoutes: [(route: route1, line: nil), (route: route2, line: nil)],
            tapRoutePill: { _ in },
            filter: .constant(.init(routeId: route1.id, directionId: 0))
        )

        let pills = try sut.inspect().findAll(RoutePill.self)
        XCTAssertTrue(try pills.first!.actualView().isActive)
        XCTAssertFalse(try pills.last!.actualView().isActive)
    }

    func testRoutePillTap() throws {
        let objects = ObjectCollectionBuilder()
        let route1 = objects.route()
        let route2 = objects.route()
        let tapExpectation = XCTestExpectation()
        let sut = StopDetailsRoutePills(
            servedRoutes: [(route: route1, line: nil), (route: route2, line: nil)],
            tapRoutePill: { route in
                tapExpectation.fulfill()
                XCTAssertEqual(route, route2)
            },
            filter: .constant(nil)
        )

        let pills = try sut.inspect().findAll(RoutePill.self)
        try pills.last?.callOnTapGesture()
        wait(for: [tapExpectation])
    }
}
