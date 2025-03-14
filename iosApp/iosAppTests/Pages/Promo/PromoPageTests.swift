//
//  PromoPageTests.swift
//  iosAppTests
//
//  Created by esimon on 2/5/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import Shared
import ViewInspector
import XCTest

final class PromoPageTest: XCTestCase {
    @MainActor func testFlow() throws {
        let finishExp = expectation(description: "calls onFinish")
        let stepChannel = PassthroughSubject<Void, Never>()

        let sut = PromoPage(
            screens: [FeaturePromo.combinedStopAndTrip],
            onFinish: { finishExp.fulfill() },
            onAdvance: { stepChannel.send() }
        )

        ViewHosting.host(view: sut)

        try sut.inspect().find(button: "Got it").tap()
        wait(for: [finishExp], timeout: 2)
    }
}
