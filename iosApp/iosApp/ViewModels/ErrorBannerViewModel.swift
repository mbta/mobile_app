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
    let settingsRepository: ISettingsRepository

    @Published
    private(set) var errorState: ErrorBannerState? = nil

    @Published
    var loadingWhenPredictionsStale: Bool

    @Published
    var showDebugMessages: Bool = false

    // option for testing
    var skipListeningForStateChanges = false

    init(
        errorRepository: IErrorBannerStateRepository = RepositoryDI().errorBanner,
        settingsRepository: ISettingsRepository = RepositoryDI().settings,
        initialLoadingWhenPredictionsStale: Bool = false,
        showDebugMessages: Bool = false,
        skipListeningForStateChanges: Bool = false
    ) {
        self.errorRepository = errorRepository
        self.settingsRepository = settingsRepository
        loadingWhenPredictionsStale = initialLoadingWhenPredictionsStale
        errorState = self.errorRepository.state.value
        self.showDebugMessages = showDebugMessages
        self.skipListeningForStateChanges = skipListeningForStateChanges
    }

    @MainActor
    func activate() async {
        errorRepository.subscribeToNetworkStatusChanges()
        showDebugMessages = await (try? settingsRepository.getSettings()[.devDebugMode]?.boolValue) ?? false

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
