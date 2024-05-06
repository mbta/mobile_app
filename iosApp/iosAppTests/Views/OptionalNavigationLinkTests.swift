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
        let target = SheetNavigationStackEntry.tripDetails(tripId: "1", vehicleId: "a", target: nil)
        let sut = OptionalNavigationLink(value: target) { Text("This is a link") }

        XCTAssertEqual(try sut.inspect().find(navigationLink: "This is a link").value(), target)
        XCTAssertTrue(try sut.inspect().find(text: "This is a link").pathToRoot.contains("navigationLink"))
    }

    func testIsNotLink() throws {
        let sut = OptionalNavigationLink(value: nil) { Text("Ceci n'est pas un lien") }

        XCTAssertFalse(try sut.inspect().find(text: "Ceci n'est pas un lien").pathToRoot.contains("navigationLink"))
    }
}
