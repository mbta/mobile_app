//
//  ErrorBannerTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-09-26.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class ErrorBannerTests: XCTestCase {
    func testRespondsToState() throws {
        let repo = MockErrorBannerStateRepository(state: nil)

        let sut = ErrorBanner(loadingWhenPredictionsStale: false, repo: repo)

        ViewHosting.host(view: sut)

        XCTAssertNil(try? sut.inspect().find(ViewType.Text.self))

        let minutesAgo = 2
        let predictionsLastUpdated = Date.now.addingTimeInterval(TimeInterval(-minutesAgo * 60)).toKotlinInstant()
        let callsAction = expectation(description: "calls action when button pressed")

        let stateSetPublisher = PassthroughSubject<Void, Never>()

        let showedState = sut.inspection.inspect(onReceive: stateSetPublisher, after: 0.2) { view in
            XCTAssertEqual(try view.find(ViewType.Text.self).string(), "Updated \(minutesAgo) minutes ago")

            try view.find(ViewType.Button.self).tap()
        }

        repo.mutableFlow.value = .StalePredictions(
            lastUpdated: predictionsLastUpdated,
            action: { callsAction.fulfill() }
        )

        stateSetPublisher.send()

        wait(for: [showedState], timeout: 1)
        wait(for: [callsAction], timeout: 1)
    }

    func testLoadingWhenPredictionsStale() throws {
        let sut = ErrorBanner(
            loadingWhenPredictionsStale: true,
            repo: MockErrorBannerStateRepository(state: .StalePredictions(
                lastUpdated: Date.distantPast.toKotlinInstant(),
                action: {}
            ))
        )

        ViewHosting.host(view: sut)

        let showedLoading = sut.inspection.inspect(after: 0.2) { view in
            XCTAssertNotNil(try view.find(ViewType.ProgressView.self))
        }

        wait(for: [showedLoading], timeout: 1)
    }
}
