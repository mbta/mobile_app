//
//  ExplainerPageTests.swift
//  iosAppTests
//
//  Created by esimon on 12/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class ExplainerPageTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testFinishingTrip() throws {
        let sut = ExplainerPage(
            explainer: .init(type: ExplainerType.FinishingAnotherTrip(), routeAccents: .init(type: .bus)),
            onClose: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Finishing another trip"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "turnaround-icon-bus"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "turnaround-shape"))
    }

    func testNoPrediction() throws {
        let sut = ExplainerPage(
            explainer: .init(type: ExplainerType.NoPrediction(), routeAccents: .init()),
            onClose: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Prediction not available yet"))
    }

    func testNoVehicle() throws {
        let sut = ExplainerPage(
            explainer: .init(type: ExplainerType.NoVehicle(), routeAccents: .init(type: .lightRail)),
            onClose: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Train location not available yet"))
    }

    func testScheduledTrackChange() throws {
        let sut = ExplainerPage(
            explainer: .init(
                type: ExplainerType.ScheduledTrackChange(data: .init(platformCode: "2", platformName: "Track 2")),
                routeAccents: .init(type: .commuterRail)
            ),
            onClose: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Track Change"))
        XCTAssertNotNil(try sut.inspect()
            .find(text: "The train is traveling on a different track than usual. Please board on Track 2."))
    }

    func testRouteType() throws {
        let sut = ExplainerPage(
            explainer: .init(type: ExplainerType.NoPrediction(), routeAccents: .init(type: .bus)),
            onClose: {}
        )
        XCTAssertNotNil(try sut.inspect().find(imageName: "mode-bus"))
    }

    func testClose() throws {
        let closeExpectation = expectation(description: "close button callback")
        let sut = ExplainerPage(
            explainer: .init(type: ExplainerType.NoPrediction(), routeAccents: .init()),
            onClose: { closeExpectation.fulfill() }
        )
        try sut.inspect().find(ActionButton.self).find(ViewType.Button.self).tap()
        wait(for: [closeExpectation], timeout: 1)
    }
}
