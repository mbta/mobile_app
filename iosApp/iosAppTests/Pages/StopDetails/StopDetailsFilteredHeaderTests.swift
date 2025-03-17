//
//  StopDetailsFilteredHeaderTests.swift
//  iosAppTests
//
//  Created by esimon on 12/2/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsFilteredHeaderTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testDisplaysStopName() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let sut = StopDetailsFilteredHeader(route: route, stop: stop)
        XCTAssertNotNil(try sut.inspect().find(text: "at **\(stop.name)**"))
    }

    func testDisplaysRoutePill() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let sut = StopDetailsFilteredHeader(route: route, stop: stop)
        XCTAssertNotNil(try sut.inspect().find(RoutePill.self))
    }

    func testPinTap() throws {
        let tapExpectation = XCTestExpectation(description: "Pin button tap callback")
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let sut = StopDetailsFilteredHeader(route: route, stop: stop, onPin: { tapExpectation.fulfill() })
        XCTAssertNoThrow(try sut.inspect().find(PinButton.self).find(ViewType.Button.self).tap())
        wait(for: [tapExpectation], timeout: 1)
    }

    func testCloseTap() throws {
        let tapExpectation = XCTestExpectation(description: "Close button tap callback")
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let sut = StopDetailsFilteredHeader(route: route, stop: stop, onClose: { tapExpectation.fulfill() })
        XCTAssertNoThrow(try sut.inspect().find(ActionButton.self).find(ViewType.Button.self).tap())
        wait(for: [tapExpectation], timeout: 1)
    }
}
