//
//  ContentViewModel.swift
//  iosApp
//
//  Created by Brady, Kayla on 7/19/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@_spi(Experimental) import MapboxMaps
import Shared

class ContentViewModel: ObservableObject {
    @Published var configResponse: ApiResult<ConfigResponse>?
    @Published var featurePromosPending: [FeaturePromo]?
    @Published var onboardingScreensPending: [OnboardingScreen]?
    @Published var defaultTab: DefaultTab?

    var configUseCase: ConfigUseCase
    var featurePromoUseCase: IFeaturePromoUseCase
    var onboardingRepository: IOnboardingRepository
    var tabPreferencesRepository: ITabPreferencesRepository

    init(configUseCase: ConfigUseCase = UsecaseDI().configUsecase,
         configResponse: ApiResult<ConfigResponse>? = nil,
         featurePromoUseCase: IFeaturePromoUseCase = UsecaseDI().featurePromoUsecase,
         featurePromosPending: [FeaturePromo]? = nil,
         onboardingRepository: IOnboardingRepository = RepositoryDI().onboarding,
         onboardingScreensPending: [OnboardingScreen]? = nil,
         tabPreferencesRepository: ITabPreferencesRepository = RepositoryDI().tabPreferences) {
        self.configUseCase = configUseCase
        self.configResponse = configResponse
        self.featurePromoUseCase = featurePromoUseCase
        self.featurePromosPending = featurePromosPending
        self.onboardingRepository = onboardingRepository
        self.onboardingScreensPending = onboardingScreensPending
        self.tabPreferencesRepository = tabPreferencesRepository
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

    @MainActor func loadPendingFeaturePromosAndTabPreferences() async {
        let promos = await (try? featurePromoUseCase.getFeaturePromos()) ?? []
        featurePromosPending = promos
        await loadTabPreferences(promos.contains(where: { $0 == .enhancedFavorites }))
    }

    @MainActor func loadOnboardingScreens() async {
        onboardingScreensPending = await (try? onboardingRepository.getPendingOnboarding()) ?? []
    }

    @MainActor func loadTabPreferences(_ hasPendingFavoritesPromo: Bool) async {
        if hasPendingFavoritesPromo {
            defaultTab = .favorites
            await (try? tabPreferencesRepository.setDefaultTab(defaultTab: .favorites))
        } else {
            defaultTab = await (try? tabPreferencesRepository.getDefaultTab())
        }
    }
}
