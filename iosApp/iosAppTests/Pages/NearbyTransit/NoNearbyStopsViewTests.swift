//
//  NoNearbyStopsViewTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-11-08.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import SwiftUI
import ViewInspector
import XCTest

final class NoNearbyStopsViewTests: XCTestCase {
    func testCopy() throws {
        let sut = NoNearbyStopsView(onOpenSearch: {}, onPanToDefaultCenter: {}).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(
            ViewType.Image.self,
            where: { try $0.actualImage().name() == "mbta-logo" }
        ))
        XCTAssertNotNil(try sut.inspect().find(text: "No nearby stops"))
        XCTAssertNotNil(try sut.inspect().find(text: "You’re outside the MBTA service area."))
    }

    func testSearch() throws {
        let exp = expectation(description: "calls onOpenSearch")
        let sut = NoNearbyStopsView(onOpenSearch: { exp.fulfill() }, onPanToDefaultCenter: {}).withFixedSettings([:])
        try sut.inspect().find(button: "Search by stop").tap()
        wait(for: [exp], timeout: 1)
    }

    func testPanToDefaultCenter() throws {
        let exp = expectation(description: "calls onPanToDefaultCenter")
        let sut = NoNearbyStopsView(onOpenSearch: {}, onPanToDefaultCenter: { exp.fulfill() }).withFixedSettings([:])
        try sut.inspect().find(button: "View transit near Boston").tap()
        wait(for: [exp], timeout: 1)
    }

    func testWithHideMaps() throws {
        let sut = NoNearbyStopsView(onOpenSearch: {}, onPanToDefaultCenter: {}).withFixedSettings([.hideMaps: true])
        XCTAssertThrowsError(try sut.inspect().find(button: "View transit near Boston"))
    }
}
