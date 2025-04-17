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
        let loadedPublisher = PassthroughSubject<Void, Never>()

        let settingsRepository = MockSettingsRepository(
            settings: [.devDebugMode: true,
                       .searchRouteResults: false,
                       .hideMaps: false],
            onGetSettings: { loadedPublisher.send(()) }
        )
        let viewModel = SettingsViewModel(settingsRepository: settingsRepository)

        let sut = MorePage(viewModel: viewModel)
        let exp = sut.inspection.inspect(onReceive: loadedPublisher, after: 1) { view in
            XCTAssertTrue(try view.find(text: "Debug Mode").parent().parent().find(ViewType.Toggle.self).isOn())
        }

        let mapDisplayTrueByDefault = sut.inspection.inspect(onReceive: loadedPublisher, after: 1) { view in
            XCTAssertTrue(try view.find(text: "Map Display").parent().parent().find(ViewType.Toggle.self).isOn())
        }

        ViewHosting.host(view: sut)
        await viewModel.getSections()

        await fulfillment(of: [exp, mapDisplayTrueByDefault], timeout: 2)
    }

    @MainActor func testSavesState() async throws {
        let loadedPublisher = PassthroughSubject<Void, Never>()
        let savedExp = expectation(description: "saved state")

        let settingsRepository = MockSettingsRepository(
            settings: [.devDebugMode: false,
                       .searchRouteResults: false],
            onGetSettings: { loadedPublisher.send(()) },
            onSaveSettings: {
                let devDebugModeSetting = $0[.devDebugMode] ?? false
                XCTAssertTrue(devDebugModeSetting.boolValue)
                savedExp.fulfill()
            }
        )
        let viewModel = SettingsViewModel(settingsRepository: settingsRepository)

        let sut = MorePage(viewModel: viewModel)
        let tapExp = sut.inspection.inspect(onReceive: loadedPublisher, after: 1) { view in
            try view.find(text: "Debug Mode").parent().parent().find(ViewType.Toggle.self).tap()
        }

        ViewHosting.host(view: sut)
        await viewModel.getSections()

        await fulfillment(of: [tapExp, savedExp], timeout: 5)
    }

    @MainActor func testLinksExist() async throws {
        let loadedPublisher = PassthroughSubject<Void, Never>()

        let settingsRepository = MockSettingsRepository(
            settings: [.devDebugMode: false,
                       .searchRouteResults: false],
            onGetSettings: { loadedPublisher.send(()) }
        )
        let viewModel = SettingsViewModel(settingsRepository: settingsRepository)

        let sut = MorePage(viewModel: viewModel)
        let exp = sut.inspection.inspect(onReceive: loadedPublisher, after: 2) { view in
            try XCTAssertNotNil(view.find(text: "Send app feedback"))
            try XCTAssertNotNil(view.find(text: "Trip Planner"))
            try XCTAssertNotNil(view.find(text: "Fare Information"))
            try XCTAssertNotNil(view.find(text: "Commuter Rail and Ferry tickets"))
            try XCTAssertNotNil(view.find(text: "Terms of Use"))
            try XCTAssertNotNil(view.find(text: "Privacy Policy"))
            try XCTAssertNotNil(view.find(text: "View source on GitHub"))
            try XCTAssertNotNil(view.find(text: "617-222-3200"))
        }

        ViewHosting.host(view: sut)
        await viewModel.getSections()

        await fulfillment(of: [exp], timeout: 5)
    }
}
