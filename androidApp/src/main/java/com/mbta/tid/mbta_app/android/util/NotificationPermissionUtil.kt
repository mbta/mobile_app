package com.mbta.tid.mbta_app.android.util

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
internal class ConstantPermissionState(
    override val permission: String,
    override val status: PermissionStatus,
) : PermissionState {
    override fun launchPermissionRequest() {}
}

@SuppressLint("InlinedApi")
@Composable
@OptIn(ExperimentalPermissionsApi::class)
fun notificationPermissionState(): PermissionState {
    // Explicit permission for notifications was not required until API 33 (Tiramisu)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else {
        ConstantPermissionState(
            android.Manifest.permission.POST_NOTIFICATIONS,
            PermissionStatus.Granted,
        )
    }
}
