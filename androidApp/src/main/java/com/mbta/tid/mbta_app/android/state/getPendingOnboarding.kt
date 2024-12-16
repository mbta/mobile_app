package com.mbta.tid.mbta_app.android.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.model.OnboardingScreen
import com.mbta.tid.mbta_app.repositories.IOnboardingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class PendingOnboardingViewModel(private val onboardingRepository: IOnboardingRepository) :
    ViewModel() {
    private val _pendingOnboarding: MutableStateFlow<List<OnboardingScreen>?> =
        MutableStateFlow(null)
    val pendingOnboarding: StateFlow<List<OnboardingScreen>?> = _pendingOnboarding

    init {
        CoroutineScope(Dispatchers.IO).launch {
            pendingOnboarding.collect { getPendingOnboarding() }
        }
    }

    suspend fun getPendingOnboarding() {
        val data = onboardingRepository.getPendingOnboarding()
        _pendingOnboarding.value = data
    }
}

@Composable
fun getPendingOnboarding(
    onboardingRepository: IOnboardingRepository = koinInject()
): List<OnboardingScreen>? {
    val viewModel = remember { PendingOnboardingViewModel(onboardingRepository) }
    return viewModel.pendingOnboarding.collectAsState(initial = null).value
}
