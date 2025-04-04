//
//  OptionalNavigationLinkTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-05-06.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import SwiftUI
import ViewInspector
import XCTest

final class OptionalNavigationLinkTests: XCTestCase {
    func testIsLink() throws {
        let target = SheetNavigationStackEntry.stopDetails(stopId: "s", stopFilter: nil, tripFilter: nil)

        let tappedExp = XCTestExpectation(description: "Nav link button tapped with target")

        func action(val: SheetNavigationStackEntry) {
            if val == target {
                tappedExp.fulfill()
            }
        }
        let sut = OptionalNavigationLink(value: target, action: action, showChevron: true) {
            Text("This is a link")
        }

        try sut.inspect().find(button: "This is a link").tap()

        wait(for: [tappedExp], timeout: 1)
    }

    func testIsNotLink() throws {
        let sut = OptionalNavigationLink(value: nil, action: { _ in
        }, showChevron: true) { Text("Ceci n'est pas un lien") }

        XCTAssertNotNil(try sut.inspect().find(text: "Ceci n'est pas un lien"))
        XCTAssertThrowsError(try sut.inspect().find(button: "Ceci n'est pas un lien"))
    }
}
