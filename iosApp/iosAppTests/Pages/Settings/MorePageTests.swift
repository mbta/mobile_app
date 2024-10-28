//
//  MorePageTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-05-03.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import shared
import ViewInspector
import XCTest

final class MorePageTests: XCTestCase {
    class FakeSettingsRepository: ISettingsRepository {
        var settings: Set<Setting>
        let onGet: (() -> Void)?
        let onSet: ((Set<Setting>) -> Void)?

        init(
            mapDebug: Bool,
            searchRouteResults: Bool,
            onGet: (() -> Void)? = nil,
            onSet: ((Set<Setting>) -> Void)? = nil
        ) {
            settings = [
                Setting(key: .map, isOn: mapDebug),
                Setting(key: .searchRouteResults, isOn: searchRouteResults),
            ]
            self.onGet = onGet
            self.onSet = onSet
        }

        func __getSettings() async throws -> Set<Setting> {
            onGet?()
            return settings
        }

        func __setSettings(settings: Set<Setting>) async throws {
            onSet?(settings)
            self.settings = settings
        }
    }

    @MainActor func testLoadsState() async throws {
        let loadedPublisher = PassthroughSubject<Void, Never>()

        let settingsRepository = FakeSettingsRepository(
            mapDebug: true,
            searchRouteResults: false,
            onGet: { loadedPublisher.send(()) }
        )
        let viewModel = SettingsViewModel(settingsRepository: settingsRepository)

        let sut = MorePage(viewModel: viewModel)
        let exp = sut.inspection.inspect(onReceive: loadedPublisher, after: 1) { view in
            XCTAssertTrue(try view.find(text: "Map Debug").parent().parent().find(ViewType.Toggle.self).isOn())
        }

        ViewHosting.host(view: sut)
        await viewModel.getSections()

        await fulfillment(of: [exp], timeout: 2)
    }

    @MainActor func testSavesState() async throws {
        let loadedPublisher = PassthroughSubject<Void, Never>()
        let savedExp = expectation(description: "saved state")

        let settingsRepository = FakeSettingsRepository(
            mapDebug: false,
            searchRouteResults: false,
            onGet: { loadedPublisher.send(()) },
            onSet: {
                let mapSetting = $0.first(where: { $0.key == .map })
                XCTAssertTrue(mapSetting?.isOn == true)
                savedExp.fulfill()
            }
        )
        let viewModel = SettingsViewModel(settingsRepository: settingsRepository)

        let sut = MorePage(viewModel: viewModel)
        let tapExp = sut.inspection.inspect(onReceive: loadedPublisher, after: 1) { view in
            try view.find(text: "Map Debug").parent().parent().find(ViewType.Toggle.self).tap()
        }

        ViewHosting.host(view: sut)
        await viewModel.getSections()

        await fulfillment(of: [tapExp, savedExp], timeout: 5)
    }
}
