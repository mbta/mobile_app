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
        XCTAssertEqual(try sut.find(ViewType.Image.self).actualImage().name(), "alert-borderless-suspension")
    }

    func testShuttleAlertHeader() throws {
        let objects = ObjectCollectionBuilder()

        let alert = objects.alert { alert in
            alert.header = "Shuttle"
            alert.effect = .shuttle
        }

        let sut = try StopDetailsAlertHeader(alert: alert, routeColor: Color(hex: "ffffff")).inspect()

        XCTAssertNotNil(try sut.find(text: "Shuttle"))
        XCTAssertEqual(try sut.find(ViewType.Image.self).actualImage().name(), "alert-borderless-shuttle")
    }

    func testOtherAlertHeader() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .bikeIssue
        }

        let sut = try StopDetailsAlertHeader(alert: alert, routeColor: nil).inspect()

        XCTAssertNotNil(try sut.find(text: ""))
        XCTAssertEqual(try sut.find(ViewType.Image.self).actualImage().name(), "alert-borderless-issue")
    }
}
