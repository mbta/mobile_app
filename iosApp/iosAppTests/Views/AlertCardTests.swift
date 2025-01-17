//
//  AlertCardTests.swift
//  iosAppTests
//
//  Created by esimon on 1/17/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class AlertCardTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testMajorAlertCard() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .stationClosure
            alert.header = "Test header"
        }

        let exp = XCTestExpectation(description: "Detail button pressed")
        let sut = AlertCard(
            alert: alert,
            spec: .major,
            color: Color.pink,
            textColor: Color.orange,
            onViewDetails: {
                exp.fulfill()
            }
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Station Closure"))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "alert-borderless-suspension"
        }))
        XCTAssertThrowsError(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "fa-circle-info"
        }))
        try sut.inspect().find(button: "View details").tap()
        wait(for: [exp], timeout: 1)
    }

    func testSecondaryAlertCard() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .detour
        }

        let exp = XCTestExpectation(description: "Card pressed")
        let sut = AlertCard(
            alert: alert,
            spec: .secondary,
            color: Color.pink,
            textColor: Color.orange,
            onViewDetails: {
                exp.fulfill()
            }
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Detour"))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "alert-borderless-issue"
        }))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "fa-circle-info"
        }))
        try sut.inspect().button().tap()
        wait(for: [exp], timeout: 1)
    }

    func testDownstreamAlertCard() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .serviceChange
        }

        let exp = XCTestExpectation(description: "Card pressed")
        let sut = AlertCard(
            alert: alert,
            spec: .downstream,
            color: Color.pink,
            textColor: Color.orange,
            onViewDetails: {
                exp.fulfill()
            }
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Service Change ahead"))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "alert-borderless-issue"
        }))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "fa-circle-info"
        }))
        try sut.inspect().button().tap()
        wait(for: [exp], timeout: 1)
    }
}
