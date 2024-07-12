//
//  ContentViewModelTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 7/20/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import XCTest

final class ContentViewModelTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testLoadConfigSetsConfig() async {
        let expectedResult = ApiResultOk<ConfigResponse>(data: .init(mapboxPublicToken: "FAKE_TOKEN"))
        let contentVM = ContentViewModel(configUseCase: ConfigUseCase(
            appCheckRepo: MockAppCheckRepository(),
            configRepo: MockConfigRepository(response: expectedResult)
        ))
        await contentVM.loadConfig()
        XCTAssertEqual(contentVM.configResponse, expectedResult)
    }

    func testLoadSettingsSetsSettings() async {
        let expectedResult: Set<Setting> = [.init(key: .dynamicMapKey, isOn: true), .init(key: .search, isOn: true)]
        let contentVM = ContentViewModel(settingsRepo: MockSettingsRepository(settings: expectedResult))
        await contentVM.loadSettings()
        XCTAssertTrue(contentVM.dynamicMapKeyEnabled)
        XCTAssertTrue(contentVM.searchEnabled)
    }
}
