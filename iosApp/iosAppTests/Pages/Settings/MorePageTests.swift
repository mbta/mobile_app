//
//  MorePageTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-05-03.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import Shared
import ViewInspector
import XCTest

final class MorePageTests: XCTestCase {
    @MainActor func testLoadsState() async throws {
        let viewModel = SettingsViewModel()

        let sut = MorePage(viewModel: viewModel)
        let exp = sut.inspection.inspect(after: 1) { view in
            XCTAssertTrue(try view.find(text: "Debug Mode").parent().parent().find(ViewType.Toggle.self).isOn())
            XCTAssertTrue(try view.find(text: "Map Display").parent().parent().find(ViewType.Toggle.self).isOn())
        }

        ViewHosting.host(view: sut.withFixedSettings([.devDebugMode: true,
                                                      .searchRouteResults: false,
                                                      .hideMaps: false]))

        await fulfillment(of: [exp], timeout: 2)
    }

    @MainActor func testSavesState() async throws {
        let savedExp = expectation(description: "saved state")

        let settingsRepository = MockSettingsRepository(
            onSaveSettings: {
                let devDebugModeSetting = $0[.devDebugMode] ?? false
                XCTAssertTrue(devDebugModeSetting.boolValue)
                savedExp.fulfill()
            }
        )
        let viewModel = SettingsViewModel()

        let sut = MorePage(viewModel: viewModel)
        let tapExp = sut.inspection.inspect(after: 1) { view in
            try view.find(text: "Debug Mode").parent().parent().find(ViewType.Toggle.self).tap()
        }

        ViewHosting.host(view: sut.environmentObject(SettingsCache(settingsRepo: settingsRepository)))

        await fulfillment(of: [tapExp, savedExp], timeout: 5)
    }

    @MainActor func testLinksExist() async throws {
        let viewModel = SettingsViewModel()

        let sut = MorePage(viewModel: viewModel)
        let exp = sut.inspection.inspect(after: 2) { view in
            try XCTAssertNotNil(view.find(text: "Send App Feedback"))
            try XCTAssertNotNil(view.find(text: "Trip Planner"))
            try XCTAssertNotNil(view.find(text: "Fare Information"))
            try XCTAssertNotNil(view.find(text: "Commuter Rail and Ferry Tickets"))
            try XCTAssertNotNil(view.find(text: "Terms of Use"))
            try XCTAssertNotNil(view.find(text: "Privacy Policy"))
            try XCTAssertNotNil(view.find(text: "View Source on GitHub"))
            try XCTAssertNotNil(view.find(text: "617-222-3200"))
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))

        await fulfillment(of: [exp], timeout: 5)
    }
}
