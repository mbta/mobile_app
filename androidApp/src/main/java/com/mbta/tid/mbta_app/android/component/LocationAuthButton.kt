package com.mbta.tid.mbta_app.android.component

import android.content.Intent
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.mbta.tid.mbta_app.android.BuildConfig
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.location.LocationDataManager
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationAuthButton(locationDataManager: LocationDataManager, modifier: Modifier = Modifier) {
    val locationPermissions = locationDataManager.rememberPermissions()

    var showSettingsPrompt by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    var lastRequestStart: EasternTimeInstant? by remember { mutableStateOf(null) }

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val currentTime = EasternTimeInstant.now()

            showSettingsPrompt =
                shouldShowSettingsPrompt(
                    locationPermissions.shouldShowRationale,
                    lastRequestStart,
                    currentTime,
                )
        }

    if (locationDataManager.hasPermission) {
        // Don't show button if they've granted any location permissions already
    } else {
        Button(
            modifier = modifier,
            onClick = {
                lastRequestStart = EasternTimeInstant.now()
                requestPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
            },
        ) {
            Row {
                Text(
                    stringResource(R.string.location_services_off),
                    color = colorResource(R.color.fill3),
                )
                Icon(
                    painterResource(id = R.drawable.baseline_chevron_right_24),
                    contentDescription = null,
                    modifier =
                        Modifier.padding(horizontal = 4.dp)
                            .size(16.dp)
                            .fillMaxSize(0.1F)
                            .background(colorResource(R.color.fill3), shape = CircleShape)
                            .padding(4.dp)
                            .align(Alignment.CenterVertically),
                    colorResource(id = R.color.key),
                )
            }
            if (showSettingsPrompt) {
                AlertDialog(
                    onDismissRequest = { showSettingsPrompt = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", BuildConfig.APPLICATION_ID, null),
                                    )
                                )
                                showSettingsPrompt = false
                            }
                        ) {
                            Text(
                                stringResource(R.string.location_settings_prompt_turn_on),
                                style = Typography.body,
                            )
                            //
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSettingsPrompt = false }) {
                            Text(
                                stringResource(R.string.location_settings_prompt_keep_off),
                                style = Typography.body,
                            )
                        }
                    },
                    title = {
                        Text(
                            stringResource(R.string.location_settings_prompt_title),
                            style = Typography.headlineBold,
                        )
                    },
                    text = { Text(stringResource(R.string.location_settings_prompt_body)) },
                    containerColor = colorResource(R.color.fill3),
                )
            }
        }
    }
}

/**
 * Whether the user should be prompted to turn location permissions on in settings. True in two
 * cases: the user was not shown the permissions prompt because they previously permanently denied
 * permissions the user was shown the permissions prompt and permanently denied permissions in <
 * 0.35 seconds (extreme edge case)
 */
fun shouldShowSettingsPrompt(
    shouldShowRationale: Boolean,
    startPermissionRequest: EasternTimeInstant?,
    endPermissionRequest: EasternTimeInstant,
): Boolean {
    return !shouldShowRationale &&
        startPermissionRequest != null &&
        (endPermissionRequest - startPermissionRequest!! < 0.35.seconds)
}
