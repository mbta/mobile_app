package com.mbta.tid.mbta_app.android.favorites

import android.Manifest
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
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
import com.mbta.tid.mbta_app.android.hasTextMatching
import com.mbta.tid.mbta_app.android.util.ConstantPermissionState
import com.mbta.tid.mbta_app.model.FavoriteSettings
import kotlin.test.assertEquals
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPermissionsApi::class)
class NotificationSettingsWidgetTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val permissionGranted =
        ConstantPermissionState(
            android.Manifest.permission.POST_NOTIFICATIONS,
            PermissionStatus.Granted,
        )

    @Test
    fun testAddTimePeriod() {
        lateinit var settings: MutableState<FavoriteSettings.Notifications>
        composeTestRule.setContent {
            settings = remember { mutableStateOf(FavoriteSettings.Notifications.disabled) }
            var settings by settings
            NotificationSettingsWidget(
                settings,
                setSettings = { settings = it },
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        assertEquals(1, settings.value.windows.size)
        composeTestRule.onNode(hasTextMatching(Regex("8:00\\sAM"))).assertExists()
        composeTestRule.onNode(hasTextMatching(Regex("9:00\\sAM"))).assertExists()
        composeTestRule.onNodeWithText("Sunday").assertIsOff()
        composeTestRule.onNodeWithText("Monday").assertIsOn()
        composeTestRule.onNodeWithText("Tuesday").assertIsOn()
        composeTestRule.onNodeWithText("Wednesday").assertIsOn()
        composeTestRule.onNodeWithText("Thursday").assertIsOn()
        composeTestRule.onNodeWithText("Friday").assertIsOn()
        composeTestRule.onNodeWithText("Saturday").assertIsOff()
        composeTestRule.onNodeWithContentDescription("Delete").assertDoesNotExist()
        composeTestRule.onNodeWithText("Add another time period").performClick()
        assertEquals(2, settings.value.windows.size)
        composeTestRule.onNode(hasTextMatching(Regex("12:00\\sPM"))).assertExists()
        composeTestRule.onNode(hasTextMatching(Regex("1:00\\sPM"))).assertExists()
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
        lateinit var settings: MutableState<FavoriteSettings.Notifications>
        composeTestRule.setContent {
            settings = remember { mutableStateOf(FavoriteSettings.Notifications.disabled) }
            var settings by settings
            NotificationSettingsWidget(
                settings,
                setSettings = { settings = it },
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.onNode(hasTextMatching(Regex("8:00\\sAM"))).performClick()
        composeTestRule.onNodeWithContentDescription("for hour").performTextReplacement("7")
        composeTestRule.onNodeWithContentDescription("for minutes").performTextReplacement("45")
        composeTestRule.onNodeWithText("Okay").performClick()
        assertEquals(LocalTime(7, 45), settings.value.windows.single().startTime)
        composeTestRule.onNode(hasTextMatching(Regex("9:00\\sAM"))).performClick()
        composeTestRule.onNodeWithContentDescription("for hour").performTextReplacement("9")
        composeTestRule.onNodeWithContentDescription("for minutes").performTextReplacement("10")
        composeTestRule.onNodeWithText("Okay").performClick()
        assertEquals(LocalTime(9, 10), settings.value.windows.single().endTime)
    }

    @Test
    fun testChangeDays() {
        lateinit var settings: MutableState<FavoriteSettings.Notifications>
        composeTestRule.setContent {
            settings = remember { mutableStateOf(FavoriteSettings.Notifications.disabled) }
            var settings by settings
            NotificationSettingsWidget(
                settings,
                setSettings = { settings = it },
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.onNodeWithText("Sunday").performClick()
        composeTestRule.onNodeWithText("Wednesday").performClick()
        composeTestRule.onNodeWithText("Friday").performClick()
        assertEquals(
            setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            settings.value.windows.single().daysOfWeek,
        )
    }

    @Test
    fun testValidatesTime() {
        lateinit var settings: MutableState<FavoriteSettings.Notifications>
        composeTestRule.setContent {
            settings = remember { mutableStateOf(FavoriteSettings.Notifications.disabled) }
            var settings by settings
            NotificationSettingsWidget(
                settings,
                setSettings = { settings = it },
                notificationPermissionState = permissionGranted,
                hasRequestedPermission = true,
            )
        }

        composeTestRule.onNodeWithText("Get disruption notifications").performClick()
        composeTestRule.onNode(hasTextMatching(Regex("8:00\\sAM"))).performClick()
        composeTestRule.onNodeWithContentDescription("for hour").performTextReplacement("10")
        composeTestRule.onNodeWithContentDescription("for minutes").performTextReplacement("45")
        composeTestRule.onNodeWithText("Okay").performClick()
        assertEquals(LocalTime(10, 45), settings.value.windows.single().startTime)
        assertEquals(LocalTime(11, 45), settings.value.windows.single().endTime)
        composeTestRule.onNode(hasTextMatching(Regex("11:45\\sAM"))).performClick()
        composeTestRule.onNodeWithContentDescription("for hour").performTextReplacement("10")
        composeTestRule.onNodeWithContentDescription("for minutes").performTextReplacement("40")
        composeTestRule.onNodeWithText("Okay").assertIsNotEnabled()
    }

    @Test
    fun testPermissionDenied() {
        lateinit var hasRequestedPermission: MutableState<Boolean>

        composeTestRule.setContent {
            hasRequestedPermission = remember { mutableStateOf(false) }
            var hasRequestedPermission by hasRequestedPermission
            NotificationSettingsWidget(
                FavoriteSettings.Notifications.disabled,
                setSettings = {},
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

        composeTestRule.onNodeWithText("Allow Notifications in Settings").assertIsDisplayed()
    }
}
