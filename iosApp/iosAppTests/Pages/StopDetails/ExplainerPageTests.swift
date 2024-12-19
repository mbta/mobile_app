//
//  ExplainerPageTests.swift
//  iosAppTests
//
//  Created by esimon on 12/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class ExplainerPageTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testNoPrediction() throws {
        let sut = ExplainerPage(explainer: .init(type: .noPrediction, routeAccents: .init()), onClose: {})
        XCTAssertNotNil(try sut.inspect().find(text: "Prediction not available yet"))
    }

    func testRouteType() throws {
        let sut = ExplainerPage(explainer: .init(type: .noPrediction, routeAccents: .init(type: .bus)), onClose: {})
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "mode-bus"
        }))
    }

    func testClose() throws {
        let closeExpectation = expectation(description: "close button callback")
        let sut = ExplainerPage(
            explainer: .init(type: .noPrediction, routeAccents: .init()),
            onClose: { closeExpectation.fulfill() }
        )
        try sut.inspect().find(ActionButton.self).button().tap()
        wait(for: [closeExpectation], timeout: 1)
    }
}
