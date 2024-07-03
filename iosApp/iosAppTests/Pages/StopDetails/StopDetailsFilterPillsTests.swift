//
//  StopDetailsFilterPillsTests.swift
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

final class StopDetailsFilterPillsTests: XCTestCase {
    func testRoutePillsDisplay() throws {
        let objects = ObjectCollectionBuilder()
        let route1 = objects.route()
        let route2 = objects.route()
        let sut = StopDetailsFilterPills(
            servedRoutes: [.route(route1, nil), .route(route2, nil)],
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
        let sut = StopDetailsFilterPills(
            servedRoutes: [.route(route1, nil), .route(route2, nil)],
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
        let sut = StopDetailsFilterPills(
            servedRoutes: [.route(route1, nil), .route(route2, nil)],
            tapRoutePill: { filter in
                tapExpectation.fulfill()
                guard case let .route(route, _) = filter else {
                    XCTFail("Filter was not by route")
                    return
                }
                XCTAssertEqual(route, route2)
            },
            filter: .constant(nil)
        )

        let pills = try sut.inspect().findAll(RoutePill.self)
        try pills.last?.callOnTapGesture()
        wait(for: [tapExpectation])
    }

    func testLinePillTap() throws {
        let objects = ObjectCollectionBuilder()
        let route1 = objects.route()
        let route2 = objects.route()
        let line = objects.line()
        let tapExpectation = XCTestExpectation()
        let sut = StopDetailsFilterPills(
            servedRoutes: [.route(route1, nil), .route(route2, nil), .line(line)],
            tapRoutePill: { filter in
                tapExpectation.fulfill()
                guard case let .line(line) = filter else {
                    XCTFail("Filter was not by line")
                    return
                }
                XCTAssertEqual(line, line)
            },
            filter: .constant(nil)
        )

        let pills = try sut.inspect().findAll(RoutePill.self)
        try pills.last?.callOnTapGesture()
        wait(for: [tapExpectation])
    }
}
