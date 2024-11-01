package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.OnboardingScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IOnboardingRepository {
    suspend fun getPendingOnboarding(): List<OnboardingScreen>

    suspend fun markOnboardingCompleted(screen: OnboardingScreen)
}

class OnboardingRepository : IOnboardingRepository, KoinComponent {
    private val accessibilityStatus: IAccessibilityStatusRepository by inject()
    private val dataStore: DataStore<Preferences> by inject()

    private val onboardingCompletedKey = stringSetPreferencesKey("onboardingScreensCompleted")

    override suspend fun getPendingOnboarding(): List<OnboardingScreen> {
        val onboardingCompleted =
            dataStore.data
                .map { it[onboardingCompletedKey] }
                .first()
                .orEmpty()
                .map(OnboardingScreen::valueOf)
                .toSet()
        val onboardingPending =
            OnboardingScreen.entries.filter { it.applies(accessibilityStatus) } -
                onboardingCompleted
        return onboardingPending
    }

    override suspend fun markOnboardingCompleted(screen: OnboardingScreen) {
        dataStore.edit {
            it[onboardingCompletedKey] = it[onboardingCompletedKey].orEmpty() + screen.name
        }
    }
}

class MockOnboardingRepository
@DefaultArgumentInterop.Enabled
constructor(
    private val pendingOnboarding: List<OnboardingScreen> = emptyList(),
    private val onMarkComplete: (OnboardingScreen) -> Unit = {}
) : IOnboardingRepository, KoinComponent {
    override suspend fun getPendingOnboarding(): List<OnboardingScreen> {
        return pendingOnboarding
    }

    override suspend fun markOnboardingCompleted(screen: OnboardingScreen) {
        onMarkComplete(screen)
    }
}
