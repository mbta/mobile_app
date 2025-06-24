package com.mbta.tid.mbta_app.android.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import org.junit.Rule
import org.junit.Test

class BarAndToastScaffoldTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testToastDisplay() {
        val toastVM = ToastViewModel()

        composeTestRule.setContent {
            BarAndToastScaffold(toastViewModel = toastVM) { Text("Content") }
        }

        toastVM.showToast(ToastViewModel.Toast(message = "Toast message"))

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Toast message").assertIsDisplayed()
    }

    @Test
    fun testToastAction() {
        val toastVM = ToastViewModel()

        composeTestRule.setContent {
            BarAndToastScaffold(toastViewModel = toastVM) { Text("Content") }
        }

        var actionTapped = false
        toastVM.showToast(
            ToastViewModel.Toast(
                message = "Toast message",
                actionLabel = "Action",
                onAction = { actionTapped = true },
            )
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Action").assertIsDisplayed().performClick()

        assert(actionTapped)
    }

    @Test
    fun testToastClose() {
        val toastVM = ToastViewModel()

        composeTestRule.setContent {
            BarAndToastScaffold(toastViewModel = toastVM) { Text("Content") }
        }

        var closeTapped = false
        toastVM.showToast(
            ToastViewModel.Toast(message = "Toast message", onClose = { closeTapped = true })
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed().performClick()

        assert(closeTapped)
    }
}
