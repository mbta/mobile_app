package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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

    public suspend fun notificationsFavoritesHintShouldShow(): Boolean

    public suspend fun notificationsFavoriteHintDismissed()
}

internal class OnboardingRepository : IOnboardingRepository, KoinComponent {
    private val accessibilityStatus: IAccessibilityStatusRepository by inject()
    private val settings: ISettingsRepository by inject()
    private val dataStore: DataStore<Preferences> by inject()

    private val onboardingCompletedKey = stringSetPreferencesKey("onboardingScreensCompleted")
    private val notificationsFavoritesHint =
        booleanPreferencesKey("notificationsFavoritesHintShouldShow")

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
            OnboardingScreen.entries.filter { it.applies(accessibilityStatus) } -
                onboardingCompleted
        return onboardingPending
    }

    override suspend fun markOnboardingCompleted(screen: OnboardingScreen) {
        dataStore.edit {
            it[onboardingCompletedKey] = it[onboardingCompletedKey].orEmpty() + screen.name
        }
    }

    override suspend fun notificationsFavoriteHintDismissed(): Unit =
        setOnboardingStateBool(notificationsFavoritesHint, false)

    override suspend fun notificationsFavoritesHintShouldShow(): Boolean =
        getOnboardingStateBool(notificationsFavoritesHint) ?: true

    private suspend fun getOnboardingStateBool(key: Preferences.Key<Boolean>): Boolean? =
        dataStore.data.map { dataStore -> dataStore[key] }.first()

    private suspend fun setOnboardingStateBool(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }
}

public class MockOnboardingRepository
@DefaultArgumentInterop.Enabled
constructor(
    public var onMarkComplete: (OnboardingScreen) -> Unit = {},
    public var onNotificationsFavoriteHintDismissed: () -> Unit = {},
) : IOnboardingRepository, KoinComponent {
    public var pendingOnboarding: List<OnboardingScreen> = emptyList()
    public var notificationsFavoritesHintShouldShow: Boolean = false

    override suspend fun getPendingOnboarding(): List<OnboardingScreen> = pendingOnboarding

    override suspend fun markOnboardingCompleted(screen: OnboardingScreen): Unit =
        onMarkComplete(screen)

    override suspend fun notificationsFavoritesHintShouldShow(): Boolean =
        notificationsFavoritesHintShouldShow

    override suspend fun notificationsFavoriteHintDismissed(): Unit =
        onNotificationsFavoriteHintDismissed()
}
