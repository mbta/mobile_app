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

public interface IOnboardingRepository {
    public suspend fun getPendingOnboarding(): List<OnboardingScreen>

    public suspend fun markOnboardingCompleted(screen: OnboardingScreen)
}

internal class OnboardingRepository : IOnboardingRepository, KoinComponent {
    private val accessibilityStatus: IAccessibilityStatusRepository by inject()
    private val settings: ISettingsRepository by inject()
    private val dataStore: DataStore<Preferences> by inject()

    private val onboardingCompletedKey = stringSetPreferencesKey("onboardingScreensCompleted")

    override suspend fun getPendingOnboarding(): List<OnboardingScreen> {
        val settings = settings.getSettings()
        val onboardingCompleted =
            dataStore.data
                .map { it[onboardingCompletedKey] }
                .first()
                .orEmpty()
                .mapNotNull {
                    try {
                        OnboardingScreen.valueOf(it)
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
                .toSet()
        val onboardingPending =
            OnboardingScreen.entries.filter { it.applies(accessibilityStatus, settings) } -
                onboardingCompleted
        return onboardingPending
    }

    override suspend fun markOnboardingCompleted(screen: OnboardingScreen) {
        dataStore.edit {
            it[onboardingCompletedKey] = it[onboardingCompletedKey].orEmpty() + screen.name
        }
    }
}

public class MockOnboardingRepository
@DefaultArgumentInterop.Enabled
constructor(
    private val pendingOnboarding: List<OnboardingScreen> = emptyList(),
    private val onMarkComplete: (OnboardingScreen) -> Unit = {},
) : IOnboardingRepository, KoinComponent {
    override suspend fun getPendingOnboarding(): List<OnboardingScreen> {
        return pendingOnboarding
    }

    override suspend fun markOnboardingCompleted(screen: OnboardingScreen) {
        onMarkComplete(screen)
    }
}
