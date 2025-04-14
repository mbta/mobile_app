//
//  AlertCardTests.swift
//  iosAppTests
//
//  Created by esimon on 1/17/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
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
            alertSummary: nil,
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
            alertSummary: nil,
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
        try sut.inspect().implicitAnyView().button().tap()
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
            alertSummary: nil,
            spec: .downstream,
            color: Color.pink,
            textColor: Color.orange,
            onViewDetails: {
                exp.fulfill()
            }
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Service change ahead"))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "alert-borderless-issue"
        }))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "fa-circle-info"
        }))
        try sut.inspect().implicitAnyView().button().tap()
        wait(for: [exp], timeout: 1)
    }

    func testElevatorAlertCard() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .elevatorClosure
            alert.header = "Elevator header"
        }

        let exp = XCTestExpectation(description: "Card pressed")
        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .elevator,
            color: Color.pink,
            textColor: Color.orange,
            onViewDetails: {
                exp.fulfill()
            }
        )
        XCTAssertNotNil(try sut.inspect().find(text: alert.header!))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "accessibility-icon-alert"
        }))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "fa-circle-info"
        }))
        try sut.inspect().implicitAnyView().button().tap()
        wait(for: [exp], timeout: 1)
    }

    func testDelayAlertCard() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .delay
            alert.header = "header"
            alert.cause = .heavyRidership
        }

        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .delay,
            color: Color.pink,
            textColor: Color.orange,
            onViewDetails: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Delays due to heavy ridership"))

        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "fa-circle-info"
        }))
    }

    func testDelayAlertCardUnknownCause() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .delay
            alert.header = "header"
            alert.cause = .unknownCause
        }

        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .delay,
            color: Color.pink,
            textColor: Color.orange,
            onViewDetails: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Delays"))

        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "fa-circle-info"
        }))
    }
}
