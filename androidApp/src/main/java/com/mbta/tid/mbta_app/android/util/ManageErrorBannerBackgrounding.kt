package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.viewModel.IErrorBannerViewModel
import kotlin.time.Duration.Companion.seconds
import org.koin.compose.koinInject

@Composable
fun ManageErrorBannerBackgrounding(vm: IErrorBannerViewModel = koinInject()) {
    val state by vm.models.collectAsState()
    var startTimer by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.returnFromBackground()
        if (state.hideBanner) startTimer = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        startTimer = false
        vm.sendToBackground()
    }

    val timer: State<EasternTimeInstant>? =
        if (startTimer) timer(updateInterval = 1.seconds) else null

    LaunchedEffect(timer?.value) { vm.setNow(timer?.value) }
    LaunchedEffect(state.hideBanner) {
        if (!state.hideBanner && startTimer) startTimer = false
    }
}
