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

    public suspend fun notificationsBetaFeedbackDialogSetState(shouldShow: Boolean)

    public suspend fun notificationsBetaFeedbackDialogShouldShow(): Boolean

    public suspend fun notificationsBetaPromptDismissed()

    public suspend fun notificationsBetaPromptShouldShow(): Boolean

    public suspend fun notificationsBetaResetAndForce()

    public suspend fun notificationsFavoritesHintShouldShow(): Boolean

    public suspend fun notificationsFavoriteHintDismissed()

    public suspend fun notificationsBetaTargetingOverride(): Boolean?
}

internal class OnboardingRepository : IOnboardingRepository, KoinComponent {
    private val accessibilityStatus: IAccessibilityStatusRepository by inject()
    private val settings: ISettingsRepository by inject()
    private val dataStore: DataStore<Preferences> by inject()

    private val onboardingCompletedKey = stringSetPreferencesKey("onboardingScreensCompleted")

    private val notificationsBetaDialog =
        booleanPreferencesKey("notificationsBetaFeedbackDialogShouldShow")
    private val notificationsBetaPrompt = booleanPreferencesKey("notificationsBetaPromptShouldShow")
    private val notificationsBetaTargetingOverride =
        booleanPreferencesKey("notificationsBetaTargetingOverride")
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
            OnboardingScreen.entries.filter { it.applies(accessibilityStatus, settings) } -
                onboardingCompleted
        return onboardingPending
    }

    override suspend fun markOnboardingCompleted(screen: OnboardingScreen) {
        dataStore.edit {
            it[onboardingCompletedKey] = it[onboardingCompletedKey].orEmpty() + screen.name
        }
    }

    override suspend fun notificationsBetaFeedbackDialogSetState(shouldShow: Boolean) {
        setOnboardingStateBool(notificationsBetaDialog, shouldShow)
    }

    override suspend fun notificationsBetaFeedbackDialogShouldShow(): Boolean =
        getOnboardingStateBool(notificationsBetaDialog) ?: true

    override suspend fun notificationsBetaPromptDismissed(): Unit =
        setOnboardingStateBool(notificationsBetaPrompt, false)

    // If the setting is set to false, that means that the prompt should not be shown again
    override suspend fun notificationsBetaPromptShouldShow(): Boolean =
        getOnboardingStateBool(notificationsBetaPrompt) ?: true

    override suspend fun notificationsBetaResetAndForce() {
        dataStore.edit {
            listOf(notificationsBetaDialog, notificationsBetaPrompt, notificationsFavoritesHint)
                .forEach { key -> it.remove(key) }
            it[onboardingCompletedKey] =
                (it[onboardingCompletedKey] ?: emptySet()) - OnboardingScreen.NotificationsBeta.name
            it[notificationsBetaTargetingOverride] = true
        }
    }

    override suspend fun notificationsBetaTargetingOverride(): Boolean? =
        getOnboardingStateBool(notificationsBetaTargetingOverride)

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
    public var onNotificationsBetaFeedbackDialogSet: (Boolean) -> Unit = {},
    public var onNotificationsBetaPromptDismissed: () -> Unit = {},
    public var onNotificationsBetaReset: () -> Unit = {},
    public var onNotificationsFavoriteHintDismissed: () -> Unit = {},
) : IOnboardingRepository, KoinComponent {
    public var pendingOnboarding: List<OnboardingScreen> = emptyList()
    public var notificationsBetaFeedbackDialogShouldShow: Boolean = false
    public var notificationsBetaPromptShouldShow: Boolean = false
    public var notificationsFavoritesHintShouldShow: Boolean = false
    public var notificationsTargetingOverride: Boolean? = null

    override suspend fun getPendingOnboarding(): List<OnboardingScreen> = pendingOnboarding

    override suspend fun markOnboardingCompleted(screen: OnboardingScreen): Unit =
        onMarkComplete(screen)

    override suspend fun notificationsBetaFeedbackDialogSetState(shouldShow: Boolean): Unit =
        onNotificationsBetaFeedbackDialogSet(shouldShow)

    override suspend fun notificationsBetaFeedbackDialogShouldShow(): Boolean =
        notificationsBetaFeedbackDialogShouldShow

    override suspend fun notificationsBetaPromptDismissed(): Unit =
        onNotificationsBetaPromptDismissed()

    override suspend fun notificationsBetaPromptShouldShow(): Boolean =
        notificationsBetaPromptShouldShow

    override suspend fun notificationsBetaResetAndForce(): Unit = onNotificationsBetaReset()

    override suspend fun notificationsBetaTargetingOverride(): Boolean? =
        notificationsTargetingOverride

    override suspend fun notificationsFavoritesHintShouldShow(): Boolean =
        notificationsFavoritesHintShouldShow

    override suspend fun notificationsFavoriteHintDismissed(): Unit =
        onNotificationsFavoriteHintDismissed()
}
