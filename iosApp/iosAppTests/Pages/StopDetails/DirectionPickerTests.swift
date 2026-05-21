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

        let setDirection0Exp: XCTestExpectation = .init(description: "updateDirectionId called with 1")
        let setDirection1Exp: XCTestExpectation = .init(description: "updateDirectionId called with 0")

        let filter: StopDetailsFilter? = .init(
            routeId: route.id,
            directionId: 0
        )

        let sut = DirectionPicker(availableDirections: [0, 1],
                                  directions: directions,
                                  route: route,
                                  selectedDirectionId: 0,
                                  updateDirectionId: { newDirectionId in
                                      if newDirectionId == 1 {
                                          setDirection1Exp.fulfill()
                                      }
                                      if newDirectionId == 0 {
                                          setDirection0Exp.fulfill()
                                      }
                                  })
        XCTAssertNotNil(try sut.inspect().find(text: "Selected Destination"))
        XCTAssertNotNil(try? sut.inspect().find(text: "Other Destination"))
        try sut.inspect().find(button: "Other Destination").tap()
        wait(for: [setDirection1Exp], timeout: 1)
        try sut.inspect().find(button: "Selected Destination").tap()
        wait(for: [setDirection0Exp], timeout: 1)
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
                                  selectedDirectionId: 0,
                                  updateDirectionId: { _ in })
        XCTAssertNotNil(try sut.inspect().find(text: "Northbound to"))
        XCTAssertNotNil(try? sut.inspect().find(text: "Southbound to"))
    }
}
