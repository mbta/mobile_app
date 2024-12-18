package com.mbta.tid.mbta_app.android.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentSender
import android.location.Location
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.mbta.tid.mbta_app.android.util.LocalActivity
import com.mbta.tid.mbta_app.android.util.LocalLocationClient
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

open class LocationDataManager {
    private val _currentLocation = MutableStateFlow<Location?>(null)

    var hasPermission by mutableStateOf(false)

    /**
     * Attach the event handlers for this [LocationDataManager] in the context of the current
     * composition. Must be called once and only once, ideally by [rememberLocationDataManager].
     */
    // with reference to
    // https://github.com/android/platform-samples/blob/20c7a4e5016fcfefbea6c598f95c51477b073a1f/samples/location/src/main/java/com/example/platform/location/locationupdates/LocationUpdatesScreen.kt
    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun running(lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current): LocationDataManager {
        val permissions = rememberPermissions()
        val locationRequest =
            remember(permissions) {
                val finePermission =
                    permissions.permissions.find {
                        it.permission == Manifest.permission.ACCESS_FINE_LOCATION
                    }
                LocationRequest.Builder(5.seconds.inWholeMilliseconds)
                    // ignore updates less than 0.1km
                    .setMinUpdateDistanceMeters(100F)
                    .setPriority(
                        if (finePermission?.status?.isGranted == true)
                            Priority.PRIORITY_HIGH_ACCURACY
                        else Priority.PRIORITY_BALANCED_POWER_ACCURACY
                    )
                    .build()
            }

        val settingsRequest =
            remember(locationRequest) {
                LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
            }

        val activity = LocalActivity.current
        val context = LocalContext.current
        hasPermission = permissions.permissions.any { it.status.isGranted }

        val locationClient = LocalLocationClient.current

        var settingsCorrect by remember { mutableStateOf(false) }

        if (hasPermission) {
            LaunchedEffect(Unit) {
                locationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    _currentLocation.value = location
                }
            }

            // https://developer.android.com/develop/sensors-and-location/location/change-location-settings#prompt
            LaunchedEffect(settingsRequest) {
                val settingsClient = LocationServices.getSettingsClient(context)
                val task = settingsClient.checkLocationSettings(settingsRequest)

                task.addOnSuccessListener { settingsCorrect = true }
                task.addOnFailureListener { exception ->
                    if (exception is ResolvableApiException) {
                        try {
                            exception.startResolutionForResult(activity, 1)
                        } catch (sendEx: IntentSender.SendIntentException) {
                            // sample ignores this so we do too
                        }
                    }
                }
            }
        }

        if (hasPermission && settingsCorrect) {
            DisposableEffect(locationRequest, lifecycleOwner) {
                val locationCallback = LocationListener { location ->
                    _currentLocation.value = location
                }
                val lifecycleObserver = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START) {
                        locationClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            Looper.getMainLooper()
                        )
                    } else if (event == Lifecycle.Event.ON_STOP) {
                        locationClient.removeLocationUpdates(locationCallback)
                    }
                }

                lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

                onDispose {
                    locationClient.removeLocationUpdates(locationCallback)
                    lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                }
            }
        }

        return this
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun rememberPermissions(onPermissionsResult: (Map<String, Boolean>) -> Unit = {}) =
        rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            onPermissionsResult
        )

    open val currentLocation = _currentLocation.asStateFlow()
}

@Composable fun rememberLocationDataManager() = remember { LocationDataManager() }.running()
