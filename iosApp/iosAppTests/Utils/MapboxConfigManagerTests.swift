//
//  MapboxConfigManagerTests.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 9/8/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import XCTest

final class MapboxConfigManagerTests: XCTestCase {
    func testWhenLoadConfigSuccessTokenSet() async {
        var configuredToken: String?
        let configRepo = MockConfigRepository(
            response: ApiResultOk(data: ConfigResponse(mapboxPublicToken: "fake_token"))
        )
        let usecase = ConfigUseCase(
            configRepo: configRepo,
            sentryRepo: MockSentryRepository()
        )
        let configManager = MapboxConfigManager(
            configUsecase: usecase,
            configureMapboxToken: { configuredToken = $0 }
        )
        await configManager.loadConfig()
        XCTAssertEqual("fake_token", configuredToken)
    }

    func testWhenLoadConfigErrorTokenNotSet() async {
        var configureTokenCalled = false
        let configRepo = MockConfigRepository(
            response: ApiResultError(code: 500, message: "oops")
        )
        let usecase = ConfigUseCase(
            configRepo: configRepo,
            sentryRepo: MockSentryRepository()
        )
        let configManager = MapboxConfigManager(
            configUsecase: usecase,
            configureMapboxToken: { _ in configureTokenCalled = true }
        )
        await configManager.loadConfig()
        XCTAssertFalse(configureTokenCalled)
    }

    func testInterceptorSetOnInit() {
        var httpInterceptorSet = false
        let configRepo = MockConfigRepository(
            response: ApiResultOk(data: ConfigResponse(mapboxPublicToken: "fake_token"))
        )
        let usecase = ConfigUseCase(
            configRepo: configRepo,
            sentryRepo: MockSentryRepository()
        )
        let configManager = MapboxConfigManager(
            configUsecase: usecase,
            setHttpInterceptor: { _ in httpInterceptorSet = true },
            configureMapboxToken: { _ in }
        )
        XCTAssertTrue(httpInterceptorSet)
    }
}
