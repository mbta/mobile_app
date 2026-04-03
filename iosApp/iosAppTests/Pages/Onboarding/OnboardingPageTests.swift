//
//  OnboardingPageTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-10-28.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import Shared
import ViewInspector
import XCTest

final class OnboardingPageTests: XCTestCase {
    @MainActor func testFlow() async throws {
        let onboardingRepository = MockOnboardingRepository()
        let finishExp = expectation(description: "calls onFinish")
        let stepChannel = PassthroughSubject<Void, Never>()

        let sut = OnboardingPage(
            screens: OnboardingScreen.allCases,
            onFinish: { finishExp.fulfill() },
            onAdvance: { stepChannel.send() },
            onboardingRepository: onboardingRepository,
            // Actual button location dialogue handling is unit tested in OnboardingScreenView.testLocationFlow
            skipLocationDialogue: true
        )

        ViewHosting.host(view: sut)

        let locationExp = sut.inspection.inspect(onReceive: stepChannel, after: 0.1) { view in
            try view.find(button: "Continue").tap()
        }
        let stationAccessibilityExp = sut.inspection.inspect(onReceive: stepChannel.dropFirst(), after: 0.1) { view in
            try view.find(button: "Continue").tap()
        }
        // TODO: Switch?

        let hideMapsExp = sut.inspection.inspect(onReceive: stepChannel.dropFirst(2), after: 0.1) { view in
            try view.find(ViewType.Toggle.self).tap()
        }

        let hideMapsContExp = sut.inspection.inspect(onReceive: stepChannel.dropFirst(2), after: 0.1) { view in
            try view.find(button: "Continue").tap()
        }
        let feedbackExp = sut.inspection.inspect(onReceive: stepChannel.dropFirst(3), after: 0.1) { view in
            try view.find(button: "Get started").tap()
        }
        stepChannel.send()
        await fulfillment(
            of: [locationExp, stationAccessibilityExp, hideMapsExp, hideMapsContExp, feedbackExp, finishExp],
            timeout: 1
        )
        XCTAssertEqual(onboardingRepository.finished, [.location, .stationAccessibility, .hideMaps, .feedback])
    }

    private class MockOnboardingRepository: IOnboardingRepository {
        var finished: [OnboardingScreen] = []

        func __getPendingOnboarding() async throws -> [OnboardingScreen] {
            OnboardingScreen.allCases.filter { !finished.contains($0) }
        }

        func __markOnboardingCompleted(screen: OnboardingScreen) async throws {
            finished += [screen]
        }

        func __notificationsBetaFeedbackDialogSetState(shouldShow _: Bool) async throws {}

        func __notificationsBetaFeedbackDialogShouldShow() async throws -> KotlinBoolean { false }

        func __notificationsBetaPromptDismissed() async throws {}

        func __notificationsBetaPromptShouldShow() async throws -> KotlinBoolean { false }

        func __notificationsBetaResetAndForce() async throws {}

        func __notificationsBetaTargetingOverride() async throws -> KotlinBoolean? { nil }

        func __notificationsFavoritesHintShouldShow() async throws -> KotlinBoolean { false }

        func __notificationsFavoriteHintDismissed() async throws {}
    }
}
