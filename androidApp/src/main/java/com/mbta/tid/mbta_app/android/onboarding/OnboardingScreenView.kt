package com.mbta.tid.mbta_app.android.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.location.LocationDataManager
import com.mbta.tid.mbta_app.model.OnboardingScreen
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreenView(
    screen: OnboardingScreen,
    advance: () -> Unit,
    locationDataManager: LocationDataManager,
    skipLocationDialogue: Boolean = false,
    settingsRepository: ISettingsRepository = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    var sharingLocation by rememberSaveable { mutableStateOf(false) }
    val permissions =
        locationDataManager.rememberPermissions(
            onPermissionsResult = {
                // This only fires after the permissions state has changed
                if (sharingLocation) {
                    advance()
                }
            }
        )

    fun hideMaps(hide: Boolean) {
        coroutineScope.launch {
            settingsRepository.setSettings(mapOf(Settings.HideMaps to hide))
            advance()
        }
    }

    fun shareLocation() {
        if (skipLocationDialogue || permissions.permissions.any { it.status.isGranted }) {
            advance()
        } else {
            sharingLocation = true
            permissions.launchMultiplePermissionRequest()
        }
    }

    Column {
        when (screen) {
            OnboardingScreen.Feedback -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(stringResource(R.string.onboarding_feedback_header))
                    Text(stringResource(R.string.onboarding_feedback_body))
                    Button(onClick = advance) {
                        Text(stringResource(R.string.onboarding_feedback_advance))
                    }
                }
            }
            OnboardingScreen.HideMaps -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(stringResource(R.string.onboarding_hide_maps_header))
                    Text(stringResource(R.string.onboarding_hide_maps_body))
                    Button(onClick = { hideMaps(true) }) {
                        Text(stringResource(R.string.onboarding_hide_maps_hide))
                    }
                    Button(onClick = { hideMaps(false) }) {
                        Text(stringResource(R.string.onboarding_hide_maps_show))
                    }
                }
            }
            OnboardingScreen.Location -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(stringResource(R.string.onboarding_location_header))
                    Text(stringResource(R.string.onboarding_location_body))
                    Button(onClick = ::shareLocation) {
                        Text(stringResource(R.string.onboarding_location_advance))
                    }
                    Text(stringResource(R.string.onboarding_location_footer))
                }
            }
        }
    }
}
