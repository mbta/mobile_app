package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.test.runTest

class ToastViewModelTest {
    @Test
    fun testShowAndHideToast() = runTest {
        val vm = ToastViewModel(onEventBufferOverflow = BufferOverflow.SUSPEND)
        testViewModelFlow(vm).test {
            assertEquals(ToastViewModel.State.Hidden, awaitItem())
            vm.showToast(ToastViewModel.Toast("Message"))
            assertEquals(ToastViewModel.State.Visible(ToastViewModel.Toast("Message")), awaitItem())
            vm.hideToast()
            assertEquals(ToastViewModel.State.Hidden, awaitItem())
        }
    }
}
