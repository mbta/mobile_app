//
//  ContentViewModelTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 7/20/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import XCTest

final class ContentViewModelTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testLoadConfigSetsConfig() async {
        let expectedResult = ApiResultOk<ConfigResponse>(data: .init(mapboxPublicToken: "FAKE_TOKEN"))
        var configResponse: String?
        let mapboxConfigManager = MapboxConfigManager(
            configUsecase: ConfigUseCase(
                configRepo: MockConfigRepository(response: expectedResult),
                sentryRepo: MockSentryRepository()
            ),
            configureMapboxToken: { token in
                configResponse = token
            }
        )
        let contentVM = ContentViewModel(mapboxConfigManager: mapboxConfigManager)
        await contentVM.loadConfig()
        XCTAssertEqual(configResponse, expectedResult.data.mapboxPublicToken)
    }

    func testLoadFeaturePromoSetsFeaturePromo() async {
        let contentVM = ContentViewModel(
            featurePromoUseCase: FeaturePromoUseCase(
                currentAppVersionRepository: MockCurrentAppVersionRepository(currentAppVersion: nil),
                lastLaunchedAppVersionRepository: MockLastLaunchedAppVersionRepository(lastLaunchedAppVersion: nil)
            )
        )
        await contentVM.loadPendingFeaturePromosAndTabPreferences()
        XCTAssertEqual(contentVM.featurePromosPending, [])
    }

    func testLoadOnboardingSetsOnboarding() async {
        let contentVM = ContentViewModel(
            onboardingRepository: MockOnboardingRepository(pendingOnboarding: [.location, .feedback])
        )
        await contentVM.loadOnboardingScreens()
        XCTAssertEqual(contentVM.onboardingScreensPending, [.location, .feedback])
    }

    func testDefaultTabNearbyIfNotShownPromo() async {
        let contentVM = ContentViewModel()
        await contentVM.loadPendingFeaturePromosAndTabPreferences()
        XCTAssertEqual(contentVM.defaultTab, .nearby)
    }

    func testDefaultTabFavoritesIfShownPromo() async {
        let contentVM = ContentViewModel(featurePromoUseCase: FeaturePromoUseCase(
            currentAppVersionRepository: MockCurrentAppVersionRepository(currentAppVersion:
                .init(major: 2, minor: 0, patch: 0)),
            lastLaunchedAppVersionRepository: MockLastLaunchedAppVersionRepository(lastLaunchedAppVersion:
                .init(major: 1, minor: 0, patch: 0))
        ))
        await contentVM.loadPendingFeaturePromosAndTabPreferences()
        XCTAssertEqual(contentVM.defaultTab, .favorites)
    }
}
