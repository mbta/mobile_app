//
//  ContentViewModel.swift
//  iosApp
//
//  Created by Brady, Kayla on 7/19/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@_spi(Experimental) import MapboxMaps
import shared

class ContentViewModel: ObservableObject {
    @Published var configResponse: ApiResult<ConfigResponse>?
    var hideMaps: Bool {
        get { false }
        set {}
    }

    @Published var onboardingScreensPending: [OnboardingScreen]?

    var configUseCase: ConfigUseCase
    var onboardingRepository: IOnboardingRepository
    var settingsRepository: ISettingsRepository

    init(configUseCase: ConfigUseCase = UsecaseDI().configUsecase,
         configResponse: ApiResult<ConfigResponse>? = nil,
         onboardingRepository: IOnboardingRepository = RepositoryDI().onboarding,
         onboardingScreensPending: [OnboardingScreen]? = nil,
         settingsRepository: ISettingsRepository = RepositoryDI().settings) {
        self.configUseCase = configUseCase
        self.configResponse = configResponse
        self.onboardingRepository = onboardingRepository
        self.onboardingScreensPending = onboardingScreensPending
        self.settingsRepository = settingsRepository
    }

    func configureMapboxToken(token: String) {
        MapboxOptions.accessToken = token
    }

    @MainActor func loadConfig() async {
        do {
            configResponse = try await configUseCase.getConfig()
        } catch {
            configResponse = ApiResultError(code: nil, message: "\(error.localizedDescription)")
        }
    }

    @MainActor func loadHideMaps() async {}

    @MainActor func loadOnboardingScreens() async {
        onboardingScreensPending = await (try? onboardingRepository.getPendingOnboarding()) ?? []
    }
}
