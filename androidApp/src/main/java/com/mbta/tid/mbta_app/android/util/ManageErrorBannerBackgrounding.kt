package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import org.koin.compose.koinInject

@Composable
fun ManageErrorBannerBackgrounding(vm: IErrorBannerViewModel = koinInject()) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.returnFromBackground() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { vm.sendToBackground() }
}
