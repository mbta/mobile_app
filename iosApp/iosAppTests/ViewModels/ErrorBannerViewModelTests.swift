//
//  ErrorBannerViewModelTests.swift
//  iosAppTests
//
//  Created by Kayla Brady on 10/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import shared
import XCTest

final class ErrorBannerViewModelTests: XCTestCase {
    func testActivateSubscribesToNetworkChanges() async {
        let onSubscribeExp = XCTestExpectation(description: "onSubscribe called")
        let repo = MockErrorBannerStateRepository(state: nil, onSubscribeToNetworkChanges: { onSubscribeExp.fulfill() })
        let errorVM = ErrorBannerViewModel(
            errorRepository: repo,
            initialLoadingWhenPredictionsStale: false,
            skipListeningForStateChanges: true
        )
        await errorVM.activate()
        wait(for: [onSubscribeExp], timeout: 1)
    }
}
