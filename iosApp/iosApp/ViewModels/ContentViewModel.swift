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
    @Published var featurePromosPending: [FeaturePromo]?
    @Published var onboardingScreensPending: [OnboardingScreen]?
    @Published var defaultTab: DefaultTab?

    var mapboxConfigManager: IMapboxConfigManager
    var featurePromoUseCase: IFeaturePromoUseCase
    var onboardingRepository: IOnboardingRepository
    var tabPreferencesRepository: ITabPreferencesRepository

    init(mapboxConfigManager: IMapboxConfigManager = MapboxConfigManager(),
         featurePromoUseCase: IFeaturePromoUseCase = UsecaseDI().featurePromoUsecase,
         featurePromosPending: [FeaturePromo]? = nil,
         onboardingRepository: IOnboardingRepository = RepositoryDI().onboarding,
         onboardingScreensPending: [OnboardingScreen]? = nil,
         tabPreferencesRepository: ITabPreferencesRepository = RepositoryDI().tabPreferences) {
        self.mapboxConfigManager = mapboxConfigManager
        self.featurePromoUseCase = featurePromoUseCase
        self.featurePromosPending = featurePromosPending
        self.onboardingRepository = onboardingRepository
        self.onboardingScreensPending = onboardingScreensPending
        self.tabPreferencesRepository = tabPreferencesRepository
    }

    @MainActor func loadConfig() async {
        await mapboxConfigManager.loadConfig()
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
