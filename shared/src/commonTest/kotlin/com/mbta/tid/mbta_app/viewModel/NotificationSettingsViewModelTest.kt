package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.Preset
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month

class NotificationSettingsViewModelTest {
    @Test
    fun initialStateIsDisabledWithNoPreset() = runTest {
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())

        testViewModelFlow(viewModel).test {
            assertEquals(
                NotificationSettingsViewModel.State(
                    FavoriteSettings.Notifications.disabled,
                    selectedPreset = null,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun loadSavedSettingsReplacesStateAndSelectsPreset() = runTest {
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        val loadedSettings =
            FavoriteSettings.Notifications(
                enabled = true,
                windows =
                    listOf(
                        FavoriteSettings.Notifications.Window(
                            startTime = Preset.Morning.startTime,
                            endTime = Preset.Morning.endTime,
                            daysOfWeek = FavoriteSettings.Notifications.Window.weekdays,
                        )
                    ),
            )

        testViewModelFlow(viewModel).test {
            assertEquals(FavoriteSettings.Notifications.disabled, awaitItem().settings)

            viewModel.loadSavedSettings(loadedSettings)

            val state = awaitItem()
            assertEquals(loadedSettings, state.settings)
            assertEquals(Preset.Morning, state.selectedPreset)
        }
    }

    @Test
    fun setEnabledUpdatesNotificationToggle() = runTest {
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())

        testViewModelFlow(viewModel).test {
            assertEquals(FavoriteSettings.Notifications.disabled, awaitItem().settings)

            viewModel.setEnabled(true)
            assertEquals(true, awaitItem().settings.enabled)

            viewModel.setEnabled(false)
            assertEquals(false, awaitItem().settings.enabled)
        }
    }

    @Test
    fun setEnabledAddsDefaultWeekdayWindowWhenPresetsFeatureDisabled() = runTest {
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())

        testViewModelFlow(viewModel).test {
            awaitItem()

            viewModel.setPresetsEnabledFlag(false)
            viewModel.setEnabled(true)

            val state = awaitItem()
            assertEquals(true, state.settings.enabled)
            assertEquals(
                listOf(
                    FavoriteSettings.Notifications.Window(
                        startTime = LocalTime(8, 0),
                        endTime = LocalTime(9, 0),
                        daysOfWeek = FavoriteSettings.Notifications.Window.weekdays,
                    )
                ),
                state.settings.windows,
            )
            assertEquals(null, state.selectedPreset)
        }
    }

    @Test
    fun setEnabledUsesCurrentPresetWindow() = runTest {
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())

        testViewModelFlow(viewModel).test {
            awaitItem()

            viewModel.setNow(EasternTimeInstant(2026, Month.SEPTEMBER, 3, 12, 30))
            viewModel.setPresetsEnabledFlag(true)
            viewModel.setEnabled(true)

            val state = awaitItem()
            assertEquals(true, state.settings.enabled)
            assertEquals(
                listOf(
                    FavoriteSettings.Notifications.Window(
                        startTime = Preset.Midday.startTime,
                        endTime = Preset.Midday.endTime,
                        daysOfWeek = FavoriteSettings.Notifications.Window.weekdays,
                    )
                ),
                state.settings.windows,
            )
            assertEquals(Preset.Midday, state.selectedPreset)
        }
    }

    @Test
    fun testRestoringCustomWindows() = runTest {
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        val customWindows =
            listOf(
                FavoriteSettings.Notifications.Window(
                    startTime = LocalTime(9, 0),
                    endTime = LocalTime(11, 0),
                    daysOfWeek = setOf(DayOfWeek.MONDAY),
                )
            )

        testViewModelFlow(viewModel).test {
            awaitItem()

            viewModel.setCustomWindows(customWindows)
            assertEquals(customWindows, awaitItem().settings.windows)

            viewModel.setNow(EasternTimeInstant(2026, Month.SEPTEMBER, 2, 8, 0))
            viewModel.setPreset(Preset.Morning)

            val presetState = awaitItem()
            assertEquals(
                listOf(
                    FavoriteSettings.Notifications.Window(
                        startTime = Preset.Morning.startTime,
                        endTime = Preset.Morning.endTime,
                        daysOfWeek = FavoriteSettings.Notifications.Window.weekdays,
                    )
                ),
                presetState.settings.windows,
            )
            assertEquals(Preset.Morning, presetState.selectedPreset)

            viewModel.setPreset(null)
            val customState = awaitItem()
            assertEquals(customWindows, customState.settings.windows)
            assertEquals(null, customState.selectedPreset)
        }
    }

    @Test
    fun addPlaceholderWindowAppendsWeekendWindowWhenFeatureFlagDisabled() = runTest {
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        val customWindows =
            listOf(
                FavoriteSettings.Notifications.Window(
                    startTime = LocalTime(9, 0),
                    endTime = LocalTime(11, 0),
                    daysOfWeek = setOf(DayOfWeek.MONDAY),
                )
            )

        testViewModelFlow(viewModel).test {
            awaitItem()

            viewModel.setCustomWindows(customWindows)
            assertEquals(customWindows, awaitItem().settings.windows)

            viewModel.addPlaceholderWindow()

            val state = awaitItem()
            assertEquals(2, state.settings.windows.size)
            assertEquals(
                customWindows +
                    FavoriteSettings.Notifications.Window(
                        startTime = LocalTime(12, 0),
                        endTime = LocalTime(13, 0),
                        daysOfWeek = FavoriteSettings.Notifications.Window.weekend,
                    ),
                state.settings.windows,
            )
            assertEquals(null, state.selectedPreset)
        }
    }

    @Test
    fun setPresetUsesWeekDaysWhenNowIsWeekday() = runTest {
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())

        testViewModelFlow(viewModel).test {
            viewModel.setNow(EasternTimeInstant(2026, Month.SEPTEMBER, 3, 8, 0))

            awaitItem()

            viewModel.setPreset(Preset.Evening)

            val weekendPresetState = awaitItem()
            assertEquals(
                FavoriteSettings.Notifications.Window.weekdays,
                weekendPresetState.settings.windows.single().daysOfWeek,
            )
            assertEquals(Preset.Evening, weekendPresetState.selectedPreset)
        }
    }

    @Test
    fun setPresetUsesWeekendDaysWhenNowIsWeekend() = runTest {
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())

        testViewModelFlow(viewModel).test {
            viewModel.setNow(EasternTimeInstant(2026, Month.SEPTEMBER, 5, 8, 0))

            awaitItem()

            viewModel.setPreset(Preset.Evening)

            val weekendPresetState = awaitItem()
            assertEquals(
                FavoriteSettings.Notifications.Window.weekend,
                weekendPresetState.settings.windows.single().daysOfWeek,
            )
            assertEquals(Preset.Evening, weekendPresetState.selectedPreset)
        }
    }
}
