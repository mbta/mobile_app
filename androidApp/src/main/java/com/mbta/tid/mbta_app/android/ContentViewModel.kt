package com.mbta.tid.mbta_app.android

import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.model.OnboardingScreen
import com.mbta.tid.mbta_app.repositories.IOnboardingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContentViewModel(private val onboardingRepository: IOnboardingRepository) : ViewModel() {
    private val _pendingOnboarding: MutableStateFlow<List<OnboardingScreen>?> =
        MutableStateFlow(null)
    val pendingOnboarding: StateFlow<List<OnboardingScreen>?> = _pendingOnboarding

    init {
        loadPendingOnboarding()
    }

    fun loadPendingOnboarding() {
        CoroutineScope(Dispatchers.IO).launch {
            val data = onboardingRepository.getPendingOnboarding()
            _pendingOnboarding.value = data
        }
    }

    fun clearPendingOnboarding() {
        _pendingOnboarding.value = emptyList()
    }
}
