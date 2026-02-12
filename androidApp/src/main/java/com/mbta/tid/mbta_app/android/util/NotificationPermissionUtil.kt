package com.mbta.tid.mbta_app.android.util

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import io.sentry.kotlin.multiplatform.Context

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
fun notificationPermissionState(onPermissionResult: (Boolean) -> Unit = {}): PermissionState {
    // Explicit permission for notifications was not required until API 33 (Tiramisu)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS, onPermissionResult)
    } else {
        val context = LocalContext.current
        val notificationManager = remember {
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        var notificationsEnabled by rememberSaveable {
            mutableStateOf(notificationManager.areNotificationsEnabled())
        }

        LifecycleResumeEffect(Unit) {
            notificationsEnabled = notificationManager.areNotificationsEnabled()

            onPauseOrDispose {}
        }

        LaunchedEffect(notificationsEnabled) { onPermissionResult(notificationsEnabled) }

        ConstantPermissionState(
            android.Manifest.permission.POST_NOTIFICATIONS,
            if (notificationsEnabled) PermissionStatus.Granted else PermissionStatus.Denied(true),
        )
    }
}
