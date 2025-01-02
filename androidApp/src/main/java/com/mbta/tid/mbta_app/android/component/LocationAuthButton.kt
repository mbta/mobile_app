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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationAuthButton(locationDataManager: LocationDataManager, modifier: Modifier) {
    val locationPermissions = locationDataManager.rememberPermissions()

    var showSettingsPrompt by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (!locationPermissions.shouldShowRationale) {
                // Either of the following just happened:
                // * the system permissions dialog was shown and the user denied permissions
                // * the system permissions dialog was not shown because the user has permanently
                // denied permissions

                // the system dialog will not be shown again, so pop the prompt to go to settings
                showSettingsPrompt = true
            }
        }
    if (locationDataManager.hasPermission) {
        // Don't show button if they've granted any location permissions already
    } else {
        Button(
            modifier = modifier,
            onClick = {
                requestPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        ) {
            Row {
                Text(
                    stringResource(R.string.location_services_off),
                    color = colorResource(R.color.fill3)
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
                    colorResource(id = R.color.key)
                )
            }
            if (showSettingsPrompt) {
                AlertDialog(
                    onDismissRequest = { showSettingsPrompt = false },
                    confirmButton = {
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                    )
                                )
                                showSettingsPrompt = false
                            }
                        ) {
                            // TODO localize
                            Text("Turn on in Settings")
                            //
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showSettingsPrompt = false }) {
                            Text("Keep Location Services Off")
                        }
                    },
                    title = { Text("MBTA Go works best with Location Services turned on") },
                    text = {
                        Text(
                            "You’ll see nearby transit options and get better search results when you turn on Location Services for MBTA Go."
                        )
                    }
                )
            }
        }
    }
}
