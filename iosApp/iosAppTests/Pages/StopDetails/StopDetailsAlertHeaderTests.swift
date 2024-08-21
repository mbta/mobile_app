//
//  StopDetailsAlertHeaderTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 7/29/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsAlertHeaderTests: XCTestCase {
    func testClosureAlertHeader() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.header = "No service"
            alert.effect = .stationClosure
        }

        let sut = try StopDetailsAlertHeader(alert: alert, routeColor: Color.pink).inspect()

        XCTAssertNotNil(try sut.find(text: "No service"))
        XCTAssertNotNil(try sut.find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "alert-borderless-suspension"
        }))
        XCTAssertNotNil(try sut.find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "fa-circle-info"
        }))
    }

    func testShuttleAlertHeader() throws {
        let objects = ObjectCollectionBuilder()

        let alert = objects.alert { alert in
            alert.header = "Shuttle"
            alert.effect = .shuttle
        }

        let sut = try StopDetailsAlertHeader(alert: alert, routeColor: Color(hex: "ffffff")).inspect()

        XCTAssertNotNil(try sut.find(text: "Shuttle"))
        XCTAssertNotNil(try sut.find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "alert-borderless-shuttle"
        }))
        XCTAssertNotNil(try sut.find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "fa-circle-info"
        }))
    }

    func testOtherAlertHeader() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .bikeIssue
        }

        let sut = try StopDetailsAlertHeader(alert: alert, routeColor: nil).inspect()

        XCTAssertNotNil(try sut.find(text: ""))
        XCTAssertNotNil(try sut.find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "alert-borderless-issue"
        }))
        XCTAssertNotNil(try sut.find(ViewType.Image.self, where: { image in try
                image.actualImage().name() == "fa-circle-info"
        }))
    }
}
