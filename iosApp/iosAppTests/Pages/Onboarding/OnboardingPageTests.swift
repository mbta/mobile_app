//
//  OnboardingPageTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-10-28.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import shared
import ViewInspector
import XCTest

final class OnboardingPageTests: XCTestCase {
    @MainActor func testFlow() throws {
        let finishExp = expectation(description: "calls onFinish")
        let sut = OnboardingPage(screens: [.location, .hideMaps, .feedback], onFinish: { finishExp.fulfill() })
        ViewHosting.host(view: sut)

        let stepChannel = PassthroughSubject<Void, Never>()
        let locationExp = sut.inspection.inspect(onReceive: stepChannel, after: 0.1) { view in
            try view.find(button: "Not now").tap()
            stepChannel.send()
        }
        let hideMapsExp = sut.inspection.inspect(onReceive: stepChannel.dropFirst(), after: 0.1) { view in
            try view.find(button: "Show maps").tap()
            stepChannel.send()
        }
        let feedbackExp = sut.inspection.inspect(onReceive: stepChannel.dropFirst(2), after: 0.1) { view in
            try view.find(button: "Get started").tap()
        }
        stepChannel.send()
        wait(for: [locationExp, hideMapsExp, feedbackExp, finishExp], timeout: 1)
    }
}
