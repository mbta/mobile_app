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
    func testStalePredictions() {
        let now = EasternTimeInstant.now()
        let minutesAgo = 2
        let predictionsLastUpdated = now.minus(minutes: Int32(minutesAgo))
        let callsAction = expectation(description: "calls action when button pressed")

        let sut = ErrorBanner(MockErrorBannerViewModel(initialState: .init(
            loadingWhenPredictionsStale: false,
            bannerHiddenAfterBackground: false,
            errorState: .StalePredictions(
                lastUpdated: predictionsLastUpdated,
                action: { callsAction.fulfill() }
            )
        )))

        ViewHosting.host(view: sut)

        XCTAssertNil(try? sut.inspect().find(ViewType.Text.self))

        let showedError = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertEqual(try view.find(ViewType.Text.self).string(), "Updated \(minutesAgo) minutes ago")
            try view.find(ViewType.Button.self).tap()
        }

        wait(for: [showedError], timeout: 1)
        wait(for: [callsAction], timeout: 2)
    }

    @MainActor func testWhenNetworkError() {
        let sut = ErrorBanner(MockErrorBannerViewModel(initialState: .init(
            loadingWhenPredictionsStale: false,
            bannerHiddenAfterBackground: false,
            errorState: .NetworkError()
        )))

        ViewHosting.host(view: sut)

        let showedError = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.find(text: "Unable to connect"))
        }

        wait(for: [showedError], timeout: 1)
    }

    @MainActor func testWhenDataError() {
        let sut = ErrorBanner(MockErrorBannerViewModel(initialState: .init(
            loadingWhenPredictionsStale: false,
            bannerHiddenAfterBackground: false,
            errorState: .DataError(messages: [], details: [], action: {})
        )))

        ViewHosting.host(view: sut.withFixedSettings([:]))

        let showedError = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.find(text: "Error loading data"))
        }

        wait(for: [showedError], timeout: 1)
    }

    @MainActor func testLoadingWhenPredictionsStale() {
        let sut = ErrorBanner(MockErrorBannerViewModel(initialState: .init(
            loadingWhenPredictionsStale: true,
            bannerHiddenAfterBackground: false,
            errorState: .StalePredictions(
                lastUpdated: Date.distantPast.toEasternInstant(),
                action: {}
            )
        )))

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
                bannerHiddenAfterBackground: false,
                errorState: .DataError(messages: ["Fake message"], details: [], action: {})
            ))
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        XCTAssertThrowsError(try sut.inspect().find(text: "Fake message"))
    }

    @MainActor func testBannerHidden() {
        let sut = ErrorBanner(MockErrorBannerViewModel(initialState: .init(
            loadingWhenPredictionsStale: false,
            bannerHiddenAfterBackground: true,
            errorState: .DataError(messages: [], details: [], action: {})
        )))

        ViewHosting.host(view: sut.withFixedSettings([:]))

        let hidError = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertThrowsError(try view.find(text: "Error loading data"))
        }

        wait(for: [hidError], timeout: 1)
    }
}
