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
    class FakeSettingsRepository: ISettingsRepository {
        var settings: [Settings: Bool]
        let onGet: (() -> Void)?
        let onSet: (([Settings: Bool]) -> Void)?

        init(
            devDebugMode: Bool,
            searchRouteResults: Bool,
            onGet: (() -> Void)? = nil,
            onSet: (([Settings: Bool]) -> Void)? = nil
        ) {
            settings = [
                .devDebugMode: devDebugMode,
                .searchRouteResults: searchRouteResults,
            ]
            self.onGet = onGet
            self.onSet = onSet
        }

        func __getSettings() async throws -> [Settings: KotlinBoolean] {
            onGet?()
            return settings.mapValues { KotlinBoolean(bool: $0) }
        }

        func __setSettings(settings: [Settings: KotlinBoolean]) async throws {
            let settingsUnboxed = settings.mapValues { $0.boolValue }
            onSet?(settingsUnboxed)
            self.settings = settingsUnboxed
        }
    }

    @MainActor func testLoadsState() async throws {
        let loadedPublisher = PassthroughSubject<Void, Never>()

        let settingsRepository = FakeSettingsRepository(
            devDebugMode: true,
            searchRouteResults: false,
            onGet: { loadedPublisher.send(()) }
        )
        let viewModel = SettingsViewModel(settingsRepository: settingsRepository)

        let sut = MorePage(viewModel: viewModel)
        let exp = sut.inspection.inspect(onReceive: loadedPublisher, after: 1) { view in
            XCTAssertTrue(try view.find(text: "Debug Mode").parent().parent().find(ViewType.Toggle.self).isOn())
        }

        ViewHosting.host(view: sut)
        await viewModel.getSections()

        await fulfillment(of: [exp], timeout: 2)
    }

    @MainActor func testSavesState() async throws {
        let loadedPublisher = PassthroughSubject<Void, Never>()
        let savedExp = expectation(description: "saved state")

        let settingsRepository = FakeSettingsRepository(
            devDebugMode: false,
            searchRouteResults: false,
            onGet: { loadedPublisher.send(()) },
            onSet: {
                let devDebugModeSetting = $0[.devDebugMode] ?? false
                XCTAssertTrue(devDebugModeSetting)
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

        let settingsRepository = FakeSettingsRepository(
            devDebugMode: false,
            searchRouteResults: false,
            onGet: { loadedPublisher.send(()) }
        )
        let viewModel = SettingsViewModel(settingsRepository: settingsRepository)

        let sut = MorePage(viewModel: viewModel)
        let exp = sut.inspection.inspect(onReceive: loadedPublisher, after: 1) { view in
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
