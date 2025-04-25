//
//  ErrorBannerViewModel.swift
//  iosApp
//
//  Created by esimon on 10/16/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import Shared

class ErrorBannerViewModel: ObservableObject {
    let errorRepository: IErrorBannerStateRepository

    @Published
    private(set) var errorState: ErrorBannerState? = nil

    @Published
    var loadingWhenPredictionsStale: Bool

    // option for testing
    var skipListeningForStateChanges = false

    init(
        errorRepository: IErrorBannerStateRepository = RepositoryDI().errorBanner,
        initialLoadingWhenPredictionsStale: Bool = false,
        skipListeningForStateChanges: Bool = false
    ) {
        self.errorRepository = errorRepository
        loadingWhenPredictionsStale = initialLoadingWhenPredictionsStale
        errorState = self.errorRepository.state.value
        self.skipListeningForStateChanges = skipListeningForStateChanges
    }

    @MainActor
    func activate() async {
        errorRepository.subscribeToNetworkStatusChanges()

        if !skipListeningForStateChanges {
            for await errorState in errorRepository.state {
                self.errorState = errorState
            }
        }
    }

    func clearState() {
        errorRepository.clearState()
    }
}
