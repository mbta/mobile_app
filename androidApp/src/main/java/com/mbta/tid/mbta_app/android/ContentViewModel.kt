package com.mbta.tid.mbta_app.android

import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.model.FeaturePromo
import com.mbta.tid.mbta_app.model.OnboardingScreen
import com.mbta.tid.mbta_app.repositories.DefaultTab
import com.mbta.tid.mbta_app.repositories.IOnboardingRepository
import com.mbta.tid.mbta_app.repositories.ITabPreferencesRepository
import com.mbta.tid.mbta_app.usecases.IFeaturePromoUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContentViewModel(
    private val featurePromoUseCase: IFeaturePromoUseCase,
    private val onboardingRepository: IOnboardingRepository,
    private val tabPreferencesRepository: ITabPreferencesRepository,
) : ViewModel() {
    private val _pendingFeaturePromos: MutableStateFlow<List<FeaturePromo>?> =
        MutableStateFlow(null)
    val pendingFeaturePromos: StateFlow<List<FeaturePromo>?> = _pendingFeaturePromos
    private val _pendingOnboarding: MutableStateFlow<List<OnboardingScreen>?> =
        MutableStateFlow(null)

    val pendingOnboarding: StateFlow<List<OnboardingScreen>?> = _pendingOnboarding

    private val _defaultTab: MutableStateFlow<DefaultTab?> = MutableStateFlow(null)
    val defaultTab: StateFlow<DefaultTab?> = _defaultTab

    init {
        loadPendingFeaturePromosAndTabPreferences()
        loadPendingOnboarding()
    }

    fun loadPendingFeaturePromosAndTabPreferences() {
        CoroutineScope(Dispatchers.IO).launch {
            val data = featurePromoUseCase.getFeaturePromos()
            _pendingFeaturePromos.value = data
            loadTabPreferences(data.contains(FeaturePromo.EnhancedFavorites))
        }
    }

    fun loadPendingOnboarding() {
        CoroutineScope(Dispatchers.IO).launch {
            val data = onboardingRepository.getPendingOnboarding()
            _pendingOnboarding.value = data
        }
    }

    fun loadTabPreferences(hasPendingFavoritesPromo: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            if (hasPendingFavoritesPromo) {
                _defaultTab.value = DefaultTab.Favorites
                tabPreferencesRepository.setDefaultTab(DefaultTab.Favorites)
            } else {
                _defaultTab.value = tabPreferencesRepository.getDefaultTab()
            }
        }
    }

    fun clearPendingOnboarding() {
        _pendingOnboarding.value = emptyList()
    }

    fun clearPendingFeaturePromos() {
        _pendingFeaturePromos.value = emptyList()
    }
}
