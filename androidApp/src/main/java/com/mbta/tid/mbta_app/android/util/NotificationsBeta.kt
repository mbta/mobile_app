package com.mbta.tid.mbta_app.android.util

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.Routes
import com.mbta.tid.mbta_app.model.morePage.MoreSection
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.viewModel.INotificationsBetaViewModel
import com.mbta.tid.mbta_app.viewModel.IToastViewModel
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import org.koin.compose.koinInject

@Composable
fun NotificationsBeta(
    navigateTopRoute: (Routes) -> Unit,
    onDismissDialog: () -> Unit,
    instanceIdCache: IInstanceIdCache = koinInject(),
    viewModel: INotificationsBetaViewModel = koinInject(),
    toastViewModel: IToastViewModel = koinInject(),
) {
    val instanceId by instanceIdCache.instanceId.collectAsState()
    val state by viewModel.models.collectAsState()

    val notificationsEnabled = SettingsCache.get(Settings.Notifications)

    val toastText = stringResource(R.string.notifications_beta_toast)

    LaunchedEffect(instanceId) { viewModel.setInstanceId(instanceId) }

    LaunchedEffect(notificationsEnabled) { viewModel.setNotificationsEnabled(notificationsEnabled) }

    LaunchedEffect(state.showBetaDialog) { if (!state.showBetaDialog) onDismissDialog() }

    LaunchedEffect(state.showBetaToast) {
        if (state.showBetaToast) {
            toastViewModel.showToast(
                ToastViewModel.Toast(
                    toastText,
                    action =
                        ToastViewModel.ToastAction.BodyWithClose(
                            {
                                navigateTopRoute(Routes.More(MoreSection.Category.PublicBetas))
                                viewModel.dismissBetaToast()
                            },
                            { viewModel.dismissBetaToast() },
                        ),
                )
            )
        }
    }

    val context = LocalContext.current

    if (state.showBetaDialog) {
        fun openFeedbackLink() {
            val webpage: Uri = Uri.parse("https://www.mbta.com/go-contact")
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Log.i("notificationsBeta", "Failed to navigate to beta feedback link")
            }

            viewModel.dismissBetaDialog()
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissBetaDialog() },
            confirmButton = {
                TextButton(
                    onClick = ::openFeedbackLink,
                    modifier = Modifier.sizeIn(minWidth = 128.dp, minHeight = 48.dp),
                    colors = ButtonDefaults.key(),
                ) {
                    Text(
                        stringResource(R.string.notifications_beta_dialog_yes),
                        style = Typography.body,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissBetaDialog() },
                    modifier = Modifier.sizeIn(minWidth = 128.dp, minHeight = 48.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            contentColor = colorResource(R.color.text),
                            containerColor = colorResource(R.color.fill1).copy(alpha = 0.16f),
                        ),
                ) {
                    Text(
                        stringResource(R.string.notifications_beta_dialog_no),
                        style = Typography.body,
                    )
                }
            },
            text = {
                Column(
                    Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        stringResource(R.string.notifications_beta_dialog_header),
                        style = Typography.headlineSemibold,
                    )
                    Text(
                        stringResource(R.string.notifications_beta_dialog_body),
                        style = Typography.body,
                    )
                }
            },
        )
    }
}
