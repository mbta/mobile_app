//
//  ErrorBannerTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-09-26.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class ErrorBannerTests: XCTestCase {
    @MainActor
    func testRespondsToState() throws {
        let repo = MockErrorBannerStateRepository(state: nil)
        let errorBannerVM = ErrorBannerViewModel(
            errorRepository: repo,
            sentryRepository: MockSentryRepository(),
            clock: SystemClock,
            onEventBufferOverflow: .suspend
        )

        let sut = ErrorBanner(errorBannerVM)

        ViewHosting.host(view: sut)

        XCTAssertNil(try? sut.inspect().find(ViewType.Text.self))

        let now = EasternTimeInstant.now()
        let minutesAgo = 2
        let predictionsLastUpdated = now.minus(minutes: Int32(minutesAgo))
        let callsAction = expectation(description: "calls action when button pressed")

        let stateSetPublisher = PassthroughSubject<Void, Never>()

        let showedState = sut.inspection.inspect(onReceive: stateSetPublisher, after: 0.5) { view in
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

    @MainActor func testWhenNetworkError() throws {
        let sut = ErrorBanner(MockErrorBannerViewModel(initialState: .init(loadingWhenPredictionsStale: false,
                                                                           errorState: .NetworkError())))

        ViewHosting.host(view: sut)

        let showedError = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.find(text: "Unable to connect"))
        }

        wait(for: [showedError], timeout: 1)
    }

    @MainActor func testLoadingWhenPredictionsStale() throws {
        let sut = ErrorBanner(MockErrorBannerViewModel(initialState: .init(loadingWhenPredictionsStale: true,
                                                                           errorState: .StalePredictions(
                                                                               lastUpdated: Date.distantPast
                                                                                   .toEasternInstant(),
                                                                               action: {}
                                                                           ))))

        ViewHosting.host(view: sut)

        let showedLoading = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.find(ViewType.ProgressView.self))
        }

        wait(for: [showedLoading], timeout: 1)
    }

    @MainActor func testDebugModeNotShownByDefault() throws {
        let sut = ErrorBanner(
            MockErrorBannerViewModel(initialState: .init(
                loadingWhenPredictionsStale: false,
                errorState: .DataError(messages: ["Fake message"], details: [], action: {})
            ))
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        XCTAssertThrowsError(try sut.inspect().find(text: "Fake message"))
    }
}
