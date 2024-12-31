package com.mbta.tid.mbta_app.android.component

import android.content.Intent
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.location.LocationDataManager

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationAuthButton(locationDataManager: LocationDataManager, modifier: Modifier) {
    val locationPermissions = locationDataManager.rememberPermissions()
    val locationServicesButtonPressed = rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    if (anyGranted(locationPermissions.permissions)) {
        // Don't show button if they've granted any location permissions already
    } else {
        Button(
            modifier = modifier,
            onClick = {
                Log.i("KB", "Should show rationale ${locationPermissions.shouldShowRationale}")
                locationPermissions.launchMultiplePermissionRequest()
                locationPermissions.shouldShowRationale

                if (locationServicesButtonPressed.value) {
                    // Have already pressed & denied permissions once already
                    if (locationPermissions.shouldShowRationale) {
                        // can launch regular permissions dialogue again
                        locationPermissions.launchMultiplePermissionRequest()
                    } else {
                        // user has denied permissions again. Send to settings
                        // TODO: Pop dialogue instead
                        context.startActivity(
                            Intent(
                                ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    }
                } else {
                    // first time requesting
                    locationPermissions.launchMultiplePermissionRequest()
                }
                locationServicesButtonPressed.value = true
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
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
fun anyGranted(permissions: List<PermissionState>): Boolean {
    return permissions.any { it.status.isGranted }
}
