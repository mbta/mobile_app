package com.mbta.tid.mbta_app.android.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.viewModel.MockToastViewModel
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import kotlinx.coroutines.flow.update
import org.junit.Rule
import org.junit.Test

class BarAndToastScaffoldTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testToastDisplay() {
        val toastVM = MockToastViewModel()
        var toastShown = false

        toastVM.onShowToast = { toast ->
            toastVM.models.update { ToastViewModel.State.Visible(toast) }
            toastShown = true
        }

        composeTestRule.setContent {
            BarAndToastScaffold(toastViewModel = toastVM) { Text("Content") }
        }

        toastVM.showToast(ToastViewModel.Toast(message = "Toast message"))

        composeTestRule.waitForIdle()
        composeTestRule.waitUntil { toastShown }
        composeTestRule.onNodeWithText("Toast message", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testToastAction() {
        val toastVM = MockToastViewModel()
        var toastShown = false

        toastVM.onShowToast = { toast ->
            toastVM.models.update { ToastViewModel.State.Visible(toast) }
            toastShown = true
        }

        composeTestRule.setContent {
            BarAndToastScaffold(toastViewModel = toastVM) { Text("Content") }
        }

        var actionTapped = false
        toastVM.showToast(
            ToastViewModel.Toast(
                message = "Toast message",
                action =
                    ToastViewModel.ToastAction.Custom(
                        actionLabel = "Action",
                        onAction = { actionTapped = true },
                    ),
            )
        )

        composeTestRule.waitForIdle()
        composeTestRule.waitUntil { toastShown }
        composeTestRule
            .onNodeWithText("Action", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        assert(actionTapped)
    }

    @Test
    fun testToastClose() {
        var closeTapped = false
        val toastVM =
            MockToastViewModel(
                ToastViewModel.State.Visible(
                    ToastViewModel.Toast(
                        message = "Toast message",
                        action = ToastViewModel.ToastAction.Close(onClose = { closeTapped = true }),
                    )
                )
            )
        toastVM.onHideToast()
        composeTestRule.setContent {
            BarAndToastScaffold(toastViewModel = toastVM) { Text("Content") }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Close", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        assert(closeTapped)
    }
}
