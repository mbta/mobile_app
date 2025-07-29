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
        let contentVM = ContentViewModel(configUseCase: ConfigUseCase(
            configRepo: MockConfigRepository(response: expectedResult),
            sentryRepo: MockSentryRepository()
        ))
        await contentVM.loadConfig()
        XCTAssertEqual(contentVM.configResponse, expectedResult)
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
}
