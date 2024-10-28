//
//  SettingsPageTests.swift
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

final class SettingsPageTests: XCTestCase {
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

    @MainActor func testLoadsState() throws {
        let loadedPublisher = PassthroughSubject<Void, Never>()

        let settingsRepository = FakeSettingsRepository(
            mapDebug: true,
            searchRouteResults: false,
            onGet: {
                loadedPublisher.send(())
            }
        )
        let sut = MorePage(
            viewModel: SettingsViewModel(settingsRepository: settingsRepository)
        )
        let exp = sut.inspection.inspect(onReceive: loadedPublisher, after: 1) { view in
            XCTAssertTrue(try view.find(ViewType.Toggle.self).isOn())
        }

        ViewHosting.host(view: sut)

        wait(for: [exp], timeout: 2)
    }

    @MainActor func testSavesState() throws {
        let loadedPublisher = PassthroughSubject<Void, Never>()
        let savedExp = expectation(description: "saved state")

        let settingsRepository = FakeSettingsRepository(
            mapDebug: false,
            searchRouteResults: false,
            onGet: {
                loadedPublisher.send(())
            },
            onSet: {
                let mapSetting = $0.first(where: { $0.key == .map })
                XCTAssertTrue(mapSetting?.isOn == true)
                savedExp.fulfill()
            }
        )
        let sut = MorePage(
            viewModel: SettingsViewModel(settingsRepository: settingsRepository)
        )

        ViewHosting.host(view: sut)

        let tapExp = sut.inspection.inspect(onReceive: loadedPublisher, after: 1) { view in
            try view.find(ViewType.Toggle.self).tap()
        }

        wait(for: [tapExp, savedExp], timeout: 2)
    }
}
