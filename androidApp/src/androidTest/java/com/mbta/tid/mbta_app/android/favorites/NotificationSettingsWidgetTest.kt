package com.mbta.tid.mbta_app.android.favorites

import android.Manifest
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
import com.mbta.tid.mbta_app.android.testUtils.hasTextMatching
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.android.util.ConstantPermissionState
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.NotificationSettingsViewModel
import kotlinx.datetime.LocalDateTime
import org.junit.Rule
import org.junit.Test
import org.koin.compose.koinInject
import org.koin.test.KoinTest

@OptIn(ExperimentalPermissionsApi::class, ExperimentalTestApi::class)
class NotificationSettingsWidgetTest : KoinTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val permissionGranted =
        ConstantPermissionState(
            android.Manifest.permission.POST_NOTIFICATIONS,
            PermissionStatus.Granted,
        )

    @Test
    fun testAddTimePeriod() {
        loadKoinMocks {
            settings =
                MockSettingsRepository(
                    settings = mapOf(Settings.NotificationPresetWindows to false)
                )
        }

        composeTestRule.setContent {
            NotificationSettingsWidget(
                viewModel = koinInject(),
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            (hasTextMatching(Regex("8:00\\sAM", RegexOption.IGNORE_CASE)))
        )
        composeTestRule
            .onNode(hasTextMatching(Regex("9:00\\sAM", RegexOption.IGNORE_CASE)))
            .assertExists()
        composeTestRule.onNodeWithText("Sunday").assertIsOff()
        composeTestRule.onNodeWithText("Monday").assertIsOn()
        composeTestRule.onNodeWithText("Tuesday").assertIsOn()
        composeTestRule.onNodeWithText("Wednesday").assertIsOn()
        composeTestRule.onNodeWithText("Thursday").assertIsOn()
        composeTestRule.onNodeWithText("Friday").assertIsOn()
        composeTestRule.onNodeWithText("Saturday").assertIsOff()
        composeTestRule.onNodeWithContentDescription("Delete").assertDoesNotExist()
        composeTestRule.onNodeWithText("Add another time period").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            (hasTextMatching(Regex("12:00\\sPM", RegexOption.IGNORE_CASE)))
        )
        composeTestRule
            .onNode(hasTextMatching(Regex("1:00\\sPM", RegexOption.IGNORE_CASE)))
            .assertExists()
        composeTestRule.onAllNodesWithText("Sunday").onLast().assertIsOn()
        composeTestRule.onAllNodesWithText("Monday").onLast().assertIsOff()
        composeTestRule.onAllNodesWithText("Tuesday").onLast().assertIsOff()
        composeTestRule.onAllNodesWithText("Wednesday").onLast().assertIsOff()
        composeTestRule.onAllNodesWithText("Thursday").onLast().assertIsOff()
        composeTestRule.onAllNodesWithText("Friday").onLast().assertIsOff()
        composeTestRule.onAllNodesWithText("Saturday").onLast().assertIsOn()
        composeTestRule.onAllNodesWithContentDescription("Delete").assertCountEquals(2)
    }

    @Test
    fun testChangeTime() {
        loadKoinMocks {
            settings =
                MockSettingsRepository(
                    settings = mapOf(Settings.NotificationPresetWindows to false)
                )
        }

        composeTestRule.setContent {
            val viewModel = NotificationSettingsViewModel(MockSentryRepository())

            NotificationSettingsWidget(
                viewModel,
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasText("Get disruption notifications").and(isEnabled())
        )
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasTextMatching(Regex("8:00\\sAM", RegexOption.IGNORE_CASE))
        )
        composeTestRule
            .onNode(hasTextMatching(Regex("8:00\\sAM", RegexOption.IGNORE_CASE)))
            .performClick()
        composeTestRule.onNodeWithText("Select start time").assertExists()
        composeTestRule.onNodeWithContentDescription("7 o'clock").performClick()
        // selecting hours in this way doesn’t automatically switch to minutes for some reason
        composeTestRule.onNodeWithContentDescription("Select minutes").performClick()
        composeTestRule.onNodeWithContentDescription("45 minutes").performClick()
        composeTestRule.onNodeWithText("Okay").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasTextMatching(Regex("7:45\\sAM", RegexOption.IGNORE_CASE))
        )
        composeTestRule
            .onNode(hasTextMatching(Regex("9:00\\sAM", RegexOption.IGNORE_CASE)))
            .performClick()
        composeTestRule.onNodeWithText("Select end time").assertExists()
        composeTestRule.onNodeWithContentDescription("Time picker type toggle").performClick()
        composeTestRule.onNodeWithContentDescription("for hour").performTextReplacement("9")
        composeTestRule.onNodeWithContentDescription("for minutes").performTextReplacement("10")
        composeTestRule.onNodeWithText("Okay").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasTextMatching(Regex("9:10\\sAM", RegexOption.IGNORE_CASE))
        )
    }

    @Test
    fun testChangeDays() {
        val now = EasternTimeInstant(LocalDateTime(2026, 8, 27, 12, 30, 0))
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())

        composeTestRule.setContent {
            NotificationSettingsWidget(
                viewModel = viewModel,
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
                now = now,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasText("Get disruption notifications").and(isEnabled())
        )
        composeTestRule.onNodeWithText("Sunday").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Sunday").and(isOn()))
        composeTestRule.onNodeWithText("Wednesday").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Wednesday").and(isOff()))
    }

    @Test
    fun testValidatesStartTime() {
        loadKoinMocks {
            settings =
                MockSettingsRepository(
                    settings = mapOf(Settings.NotificationPresetWindows to false)
                )
        }

        composeTestRule.setContent {
            NotificationSettingsWidget(
                koinInject(),
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasText("Get disruption notifications").and(isEnabled())
        )
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasTextMatching(Regex("8:00\\sAM", RegexOption.IGNORE_CASE))
        )
        composeTestRule
            .onNode(hasTextMatching(Regex("8:00\\sAM", RegexOption.IGNORE_CASE)))
            .performClick()
        composeTestRule.onNodeWithContentDescription("10 o'clock").performClick()
        composeTestRule.onNodeWithContentDescription("Select minutes").performClick()
        composeTestRule.onNodeWithContentDescription("45 minutes").performClick()
        composeTestRule.onNodeWithText("Okay").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasTextMatching(Regex("10:45\\sAM", RegexOption.IGNORE_CASE))
        )
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasTextMatching(Regex("11:45\\sAM", RegexOption.IGNORE_CASE))
        )
    }

    @Test
    fun testValidatesEndTime() {
        loadKoinMocks {
            settings =
                MockSettingsRepository(
                    settings = mapOf(Settings.NotificationPresetWindows to false)
                )
        }

        composeTestRule.setContent {
            NotificationSettingsWidget(
                koinInject(),
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasText("Get disruption notifications").and(isEnabled())
        )
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasTextMatching(Regex("8:00\\sAM", RegexOption.IGNORE_CASE))
        )
        composeTestRule
            .onNode(hasTextMatching(Regex("9:00\\sAM", RegexOption.IGNORE_CASE)))
            .performClick()
        composeTestRule.onNodeWithContentDescription("7 o'clock").performClick()
        composeTestRule.onNodeWithContentDescription("Select minutes").performClick()
        composeTestRule.onNodeWithContentDescription("40 minutes").performClick()
        composeTestRule.onNodeWithText("Okay").assertIsNotEnabled()
    }

    @Test
    fun testPermissionDenied() {
        lateinit var hasRequestedPermission: MutableState<Boolean>
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())

        composeTestRule.setContent {
            hasRequestedPermission = remember { mutableStateOf(false) }
            var hasRequestedPermission by hasRequestedPermission
            NotificationSettingsWidget(
                viewModel,
                notificationPermissionState =
                    ConstantPermissionState(
                        Manifest.permission.POST_NOTIFICATIONS,
                        PermissionStatus.Denied(false),
                    ),
                hasRequestedPermission = hasRequestedPermission,
            )
        }

        composeTestRule.onNodeWithText("Allow Notifications in Settings").assertIsNotDisplayed()
        hasRequestedPermission.value = true

        composeTestRule.onNodeWithText("Allow Notifications in Settings").assertCanBeDisplayed()
    }

    @Test
    fun testPresetButtonsAreNotVisibleWhenFeatureFlagDisabled() {
        loadKoinMocks {
            settings =
                MockSettingsRepository(
                    settings = mapOf(Settings.NotificationPresetWindows to false)
                )
        }

        val viewModel = NotificationSettingsViewModel(MockSentryRepository())

        composeTestRule.setContent {
            NotificationSettingsWidget(
                viewModel,
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()

        composeTestRule.onNodeWithText("Morning").assertDoesNotExist()
        composeTestRule.onNodeWithText("Midday").assertDoesNotExist()
        composeTestRule.onNodeWithText("Evening").assertDoesNotExist()
        composeTestRule.onNodeWithText("All day").assertDoesNotExist()
        composeTestRule.onNodeWithText("Custom").assertDoesNotExist()
    }

    @Test
    fun testPresetButtonsAreVisibleWhenFeatureFlagEnabled() {
        loadKoinMocks {
            settings =
                MockSettingsRepository(settings = mapOf(Settings.NotificationPresetWindows to true))
        }

        val viewModel = NotificationSettingsViewModel(MockSentryRepository())

        composeTestRule.setContent {
            NotificationSettingsWidget(
                viewModel,
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Morning"))
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Midday"))
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Evening"))
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("All day"))
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Custom"))
    }

    @Test
    fun testEditingPresetTimeSelectsCustom() {
        loadKoinMocks {
            settings =
                MockSettingsRepository(settings = mapOf(Settings.NotificationPresetWindows to true))
        }
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())

        composeTestRule.setContent {
            NotificationSettingsWidget(
                viewModel,
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Morning"))
        composeTestRule.onNodeWithText("Morning").performClick()
        composeTestRule.onNodeWithText("Morning").assertIsSelected()

        composeTestRule
            .onNode(hasTextMatching(Regex("6:00\\sAM", RegexOption.IGNORE_CASE)))
            .performClick()
        composeTestRule.onNodeWithContentDescription("7 o'clock").performClick()
        composeTestRule.onNodeWithContentDescription("Select minutes").performClick()
        composeTestRule.onNodeWithContentDescription("15 minutes").performClick()
        composeTestRule.onNodeWithText("Okay").performClick()

        composeTestRule.onNodeWithText("Custom").assertIsSelected()
    }

    @Test
    fun testAddingWindowSelectsCustom() {
        loadKoinMocks {
            settings =
                MockSettingsRepository(settings = mapOf(Settings.NotificationPresetWindows to true))
        }
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())

        composeTestRule.setContent {
            NotificationSettingsWidget(
                viewModel,
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Morning"))
        composeTestRule.onNodeWithText("Midday").performClick()
        composeTestRule.onNodeWithText("Midday").assertIsSelected()

        composeTestRule.onNodeWithText("Add another time period").performClick()

        composeTestRule.onNodeWithText("Custom").assertIsSelected()
    }

    @Test
    fun testSelectsPresetMatchingCurrentTime() {
        loadKoinMocks {
            settings =
                MockSettingsRepository(settings = mapOf(Settings.NotificationPresetWindows to true))
        }

        val viewModel = NotificationSettingsViewModel(MockSentryRepository())

        composeTestRule.setContent {
            NotificationSettingsWidget(
                viewModel,
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
                now = EasternTimeInstant(LocalDateTime(2026, 8, 27, 12, 30, 0)),
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Morning"))
        composeTestRule.onNodeWithText("Midday").assertIsSelected()
    }

    @Test
    fun testCustomPresetRestoredAfterSelectingAnotherPreset() {
        loadKoinMocks {
            settings =
                MockSettingsRepository(settings = mapOf(Settings.NotificationPresetWindows to true))
        }
        composeTestRule.setContent {
            var settings by remember { mutableStateOf(FavoriteSettings.Notifications.disabled) }
            NotificationSettingsWidget(
                settings,
                setSettings = { settings = it },
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
                now = EasternTimeInstant(LocalDateTime(2026, 8, 27, 4, 30, 0)),
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Morning"))

        composeTestRule.onNodeWithText("Custom").performClick()
        composeTestRule.onNodeWithText("Sunday").performClick()
        composeTestRule.onNodeWithText("Custom").assertIsSelected()
        composeTestRule.onNodeWithText("Sunday").assertIsOn()

        composeTestRule.onNodeWithText("Morning").performClick()
        composeTestRule.onNodeWithText("Morning").assertIsSelected()
        composeTestRule.onNodeWithText("Sunday").assertIsOff()

        composeTestRule.onNodeWithText("Custom").performClick()
        composeTestRule.onNodeWithText("Custom").assertIsSelected()
        composeTestRule.onNodeWithText("Sunday").assertIsOn()
    }
}
