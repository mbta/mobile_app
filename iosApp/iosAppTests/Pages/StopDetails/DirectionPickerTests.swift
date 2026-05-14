//
//  DirectionPickerTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 2024-04-24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class DirectionPickerTests: XCTestCase {
    let directions = [
        Direction(name: "North", destination: "Selected Destination", id: 0),
        Direction(name: "South", destination: "Other Destination", id: 1),
    ]

    func testDirectionFilter() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }

        let setFilter1Exp: XCTestExpectation = .init(description: "set filter called with direction 1")
        let setFilter0Exp: XCTestExpectation = .init(description: "set filter called with direction 0")

        let filter: StopDetailsFilter? = .init(
            routeId: route.id,
            directionId: 0
        )

        let sut = DirectionPicker(availableDirections: [0, 1],
                                  directions: directions,
                                  route: route,
                                  filter: filter, setFilter: { filter in
                                      if filter?.directionId == 1 {
                                          setFilter1Exp.fulfill()
                                      }
                                      if filter?.directionId == 0 {
                                          setFilter0Exp.fulfill()
                                      }
                                  })
        XCTAssertNotNil(try sut.inspect().find(text: "Selected Destination"))
        XCTAssertNotNil(try? sut.inspect().find(text: "Other Destination"))
        try sut.inspect().find(button: "Other Destination").tap()
        wait(for: [setFilter1Exp], timeout: 1)
        try sut.inspect().find(button: "Selected Destination").tap()
        wait(for: [setFilter0Exp], timeout: 1)
    }

    func testFormatsNorthSouth() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }

        let filter: StopDetailsFilter? = .init(
            routeId: route.id,
            directionId: 0
        )

        let sut = DirectionPicker(availableDirections: [0, 1],
                                  directions: directions,
                                  route: route,
                                  filter: filter, setFilter: { _ in })
        XCTAssertNotNil(try sut.inspect().find(text: "Northbound to"))
        XCTAssertNotNil(try? sut.inspect().find(text: "Southbound to"))
    }
}
