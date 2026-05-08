package com.mbta.tid.mbta_app.android.util

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.viewModel.MockNotificationsBetaViewModel
import com.mbta.tid.mbta_app.viewModel.MockToastViewModel
import com.mbta.tid.mbta_app.viewModel.NotificationsBetaViewModel
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class NotificationsBetaTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testToastIsDisplayed() = runBlocking {
        val instanceId = "83bdbf80ce1c431cbf42f622ad32a639"
        val instanceIdCache = MockInstanceIdCache(instanceId)
        val vm =
            MockNotificationsBetaViewModel(
                initialState =
                    NotificationsBetaViewModel.State(showBetaToast = true, showBetaDialog = false)
            )

        var instanceIdSet = false
        vm.onSetInstanceId = { instanceIdSet = it == instanceId }
        val toastVm = MockToastViewModel()
        var shownToast: ToastViewModel.Toast? = null
        toastVm.onShowToast = { shownToast = it }
        composeTestRule.setContent { NotificationsBeta({}, {}, instanceIdCache, vm, toastVm) }
        composeTestRule.awaitIdle()
        assert(instanceIdSet)
        assertEquals(
            "<u>Get early access to Notifications</u> and provide feedback",
            shownToast?.message,
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testDialogIsDisplayed() = runBlocking {
        val instanceIdCache = MockInstanceIdCache("83bdbf80ce1c431cbf42f622ad32a639")
        val vm =
            MockNotificationsBetaViewModel(
                initialState =
                    NotificationsBetaViewModel.State(showBetaToast = false, showBetaDialog = true)
            )

        val toastVm = MockToastViewModel()

        composeTestRule.setContent { NotificationsBeta({}, {}, instanceIdCache, vm, toastVm) }
        composeTestRule.awaitIdle()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasText("We depend on riders like you to learn how we can improve!")
        )
    }
}
