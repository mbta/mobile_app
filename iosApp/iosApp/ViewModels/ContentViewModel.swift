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
    @Published var hideMaps: Bool
    @Published var featurePromosPending: [FeaturePromo]?
    @Published var onboardingScreensPending: [OnboardingScreen]?

    var configUseCase: ConfigUseCase
    var featurePromoUseCase: FeaturePromoUseCase
    var onboardingRepository: IOnboardingRepository
    var settingsRepository: ISettingsRepository

    init(configUseCase: ConfigUseCase = UsecaseDI().configUsecase,
         configResponse: ApiResult<ConfigResponse>? = nil,
         featurePromoUseCase: FeaturePromoUseCase = UsecaseDI().featurePromoUsecase,
         featurePromosPending: [FeaturePromo]? = nil,
         onboardingRepository: IOnboardingRepository = RepositoryDI().onboarding,
         onboardingScreensPending: [OnboardingScreen]? = nil,
         settingsRepository: ISettingsRepository = RepositoryDI().settings,
         hideMaps: Bool = false) {
        self.configUseCase = configUseCase
        self.configResponse = configResponse
        self.featurePromoUseCase = featurePromoUseCase
        self.featurePromosPending = featurePromosPending
        self.onboardingRepository = onboardingRepository
        self.onboardingScreensPending = onboardingScreensPending
        self.settingsRepository = settingsRepository
        self.hideMaps = hideMaps
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

    @MainActor func loadFeaturePromos() async {
        featurePromosPending = await (try? featurePromoUseCase.getFeaturePromos()) ?? []
    }

    @MainActor func loadHideMaps() async {
        hideMaps = await (try? settingsRepository.getSettings()[.hideMaps]?.boolValue) ?? false
    }

    @MainActor func loadOnboardingScreens() async {
        onboardingScreensPending = await (try? onboardingRepository.getPendingOnboarding()) ?? []
    }
}
