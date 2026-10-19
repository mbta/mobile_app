package com.mbta.tid.mbta_app.android.favorites

import android.Manifest
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.mbta.tid.mbta_app.android.testUtils.hasTextMatching
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.android.util.ConstantPermissionState
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.NotificationSettingsViewModel
import kotlin.test.assertEquals
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.datetime.LocalDateTime
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest

@OptIn(ExperimentalPermissionsApi::class, ExperimentalTestApi::class)
class NotificationSettingsWidgetTest : KoinTest {
    @get:Rule val composeTestRule = createComposeRule(effectContext = UnconfinedTestDispatcher())

    private val permissionGranted =
        ConstantPermissionState(
            Manifest.permission.POST_NOTIFICATIONS,
            PermissionStatus.Granted,
        )

    @Test
    fun testAddTimePeriod() {
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        viewModel.loadSavedSettings(FavoriteSettings.Notifications.disabled)

        composeTestRule.setContent {
            NotificationSettingsWidget(
                viewModel = viewModel,
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            (hasTextMatching(Regex("10:00\\sAM", RegexOption.IGNORE_CASE)))
        )
        composeTestRule
            .onNode(hasTextMatching(Regex("4:00\\sPM", RegexOption.IGNORE_CASE)))
            .assertExists()
        composeTestRule.onNodeWithText("Sunday").assertIsOff()
        composeTestRule.onNodeWithText("Monday").assertIsOn()
        composeTestRule.onNodeWithText("Tuesday").assertIsOn()
        composeTestRule.onNodeWithText("Wednesday").assertIsOn()
        composeTestRule.onNodeWithText("Thursday").assertIsOn()
        composeTestRule.onNodeWithText("Friday").assertIsOn()
        composeTestRule.onNodeWithText("Saturday").assertIsOff()
        composeTestRule.onNodeWithContentDescription("Delete time period").assertDoesNotExist()
        composeTestRule.onNodeWithText("Add another time period").performClick()
        composeTestRule
            .onAllNodes(hasTextMatching(Regex("10:00\\sAM", RegexOption.IGNORE_CASE)))
            .assertCountEquals(2)
        composeTestRule
            .onAllNodes(hasTextMatching(Regex("10:00\\sAM", RegexOption.IGNORE_CASE)))
            .assertCountEquals(2)
        composeTestRule.onAllNodesWithText("Sunday").assertCountEquals(2).assertAll(isOff())
        composeTestRule.onAllNodesWithText("Monday").assertCountEquals(2).assertAll(isOn())
        composeTestRule.onAllNodesWithText("Tuesday").assertCountEquals(2).assertAll(isOn())
        composeTestRule.onAllNodesWithText("Wednesday").assertCountEquals(2).assertAll(isOn())
        composeTestRule.onAllNodesWithText("Thursday").assertCountEquals(2).assertAll(isOn())
        composeTestRule.onAllNodesWithText("Friday").assertCountEquals(2).assertAll(isOn())
        composeTestRule.onAllNodesWithText("Saturday").assertCountEquals(2).assertAll(isOff())
        composeTestRule.onAllNodesWithContentDescription("Delete time period").assertCountEquals(2)
    }

    @Test
    fun testChangeTime() {
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        viewModel.loadSavedSettings(FavoriteSettings.Notifications.disabled)

        composeTestRule.setContent {
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
            hasTextMatching(Regex("4:00\\sPM", RegexOption.IGNORE_CASE))
        )
        composeTestRule
            .onNode(hasTextMatching(Regex("10:00\\sAM", RegexOption.IGNORE_CASE)))
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
            .onNode(hasTextMatching(Regex("4:00\\sPM", RegexOption.IGNORE_CASE)))
            .performClick()
        composeTestRule.onNodeWithText("Select end time").assertExists()
        composeTestRule.onNodeWithContentDescription("Time picker type toggle").performClick()
        composeTestRule.onNodeWithContentDescription("for hour").performTextReplacement("9")
        composeTestRule.onNodeWithContentDescription("for minutes").performTextReplacement("15")
        composeTestRule.onNodeWithText("Okay").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasTextMatching(Regex("9:15\\sPM", RegexOption.IGNORE_CASE))
        )
    }

    @Test
    fun testChangeDays() {
        val now = EasternTimeInstant(LocalDateTime(2026, 8, 27, 12, 30, 0))
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        viewModel.loadSavedSettings(FavoriteSettings.Notifications.disabled)

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
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        viewModel.loadSavedSettings(FavoriteSettings.Notifications.disabled)

        composeTestRule.setContent {
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
        composeTestRule.onNodeWithText("Morning").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasTextMatching(Regex("10:00\\sAM", RegexOption.IGNORE_CASE))
        )
        composeTestRule
            .onNode(hasTextMatching(Regex("6:00\\sAM", RegexOption.IGNORE_CASE)))
            .performClick()
        composeTestRule.onNodeWithContentDescription("10 o'clock").performClick()
        composeTestRule.onNodeWithContentDescription("Select minutes").performClick()
        composeTestRule.onNodeWithContentDescription("45 minutes").performClick()
        composeTestRule.onNodeWithText("Okay").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasTextMatching(Regex("10:45\\sAM", RegexOption.IGNORE_CASE))
        )
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasTextMatching(Regex("11:00\\sAM", RegexOption.IGNORE_CASE))
        )
    }

    @Test
    fun testValidatesEndTime() {
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        viewModel.loadSavedSettings(FavoriteSettings.Notifications.disabled)

        composeTestRule.setContent {
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
        composeTestRule.onNodeWithText("Morning").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasTextMatching(Regex("6:00\\sAM", RegexOption.IGNORE_CASE))
        )
        composeTestRule
            .onNode(hasTextMatching(Regex("10:00\\sAM", RegexOption.IGNORE_CASE)))
            .performClick()
        composeTestRule.onNodeWithContentDescription("5 o'clock").performClick()
        composeTestRule.onNodeWithContentDescription("Select minutes").performClick()
        composeTestRule.onNodeWithContentDescription("45 minutes").performClick()
        composeTestRule.onNodeWithText("Okay").performClick()
        composeTestRule
            .onNode(hasTextMatching(Regex("6:15\\sAM", RegexOption.IGNORE_CASE)))
            .performClick()
    }

    @Test
    fun testPermissionDenied() {
        lateinit var hasRequestedPermission: MutableState<Boolean>
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        viewModel.loadSavedSettings(FavoriteSettings.Notifications.disabled)

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

        composeTestRule.onNodeWithText("Allow Notifications in Settings").assertExists()
    }

    @Test
    fun testPresetButtonsAreVisibleWhenFeatureFlagEnabled() {

        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        viewModel.loadSavedSettings(FavoriteSettings.Notifications.disabled)

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
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        viewModel.loadSavedSettings(FavoriteSettings.Notifications.disabled)

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
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        viewModel.loadSavedSettings(FavoriteSettings.Notifications.disabled)

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
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        viewModel.loadSavedSettings(FavoriteSettings.Notifications.disabled)

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
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        viewModel.loadSavedSettings(FavoriteSettings.Notifications.disabled)
        composeTestRule.setContent {
            NotificationSettingsWidget(
                viewModel,
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

    @Test
    fun testStartAndEndOfService() {
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        viewModel.loadSavedSettings(FavoriteSettings.Notifications.disabled)

        composeTestRule.setContent {
            NotificationSettingsWidget(
                viewModel,
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Morning"))
        composeTestRule.onNodeWithText("All day").performClick()
        composeTestRule.onNodeWithText("All day").assertIsSelected()

        val serviceBoundTimes =
            composeTestRule.onAllNodes(hasTextMatching(Regex("3:00\\sAM", RegexOption.IGNORE_CASE)))

        assertEquals(2, serviceBoundTimes.fetchSemanticsNodes().size)
        composeTestRule.onNodeWithContentDescription("start of service").assertExists()
        composeTestRule.onNodeWithContentDescription("end of service").assertExists()
    }

    @Test
    fun testNextDay() {
        val viewModel = NotificationSettingsViewModel(MockSentryRepository())
        viewModel.loadSavedSettings(FavoriteSettings.Notifications.disabled)

        composeTestRule.setContent {
            NotificationSettingsWidget(
                viewModel,
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Evening"))
        composeTestRule.onNodeWithText("Evening").performClick()
        composeTestRule.onNodeWithText("Evening").assertIsSelected()

        composeTestRule
            .onNode(hasTextMatching(Regex("4:00\\sPM", RegexOption.IGNORE_CASE)))
            .performClick()

        composeTestRule.onNodeWithContentDescription("10 o'clock").performClick()
        composeTestRule.onNodeWithText("Okay").performClick()

        composeTestRule
            .onNode(hasTextMatching(Regex("10:15\\sPM", RegexOption.IGNORE_CASE)))
            .performClick()

        composeTestRule.onNodeWithContentDescription("1 o'clock").performClick()
        composeTestRule.onNodeWithText("AM").performClick()
        composeTestRule.onNodeWithText("Okay").performClick()

        composeTestRule.onNodeWithContentDescription("next day").assertExists()
    }
}
