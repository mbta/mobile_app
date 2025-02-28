package com.mbta.tid.mbta_app.android.onboarding

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.location.LocationDataManager
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.model.OnboardingScreen
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
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

    fun showStationAccessibility(show: Boolean) {
        coroutineScope.launch {
            settingsRepository.setSettings(mapOf(Settings.ElevatorAccessibility to show))
            advance()
        }
    }

    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val locationHaloSize = screenWidth * 0.8f
    val moreHaloSize = screenWidth * 0.55f
    val haloOffset =
        if ((screenWidth / screenHeight) >= 390f / 844f) {
            val scaledHeight = (screenWidth / 390f) * 844f
            -((scaledHeight / 2f) - (scaledHeight / 3f) - 18.dp)
        } else {
            -((screenHeight / 2f) - (screenHeight / 3f))
        }
    val haloTransition = rememberInfiniteTransition(label = "infinite")
    val haloSizeMultiplier =
        haloTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.22f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1250, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "haloSizeMultiplier"
        )
    val moreHaloSizeDp = moreHaloSize * haloSizeMultiplier.value
    val locationHaloSizeDp = locationHaloSize * haloSizeMultiplier.value

    val textScale = with(LocalDensity.current) { 1.sp.toPx() / 1.dp.toPx() }

    when (screen) {
        OnboardingScreen.Feedback -> {
            OnboardingPieces.PageBox(colorResource(R.color.fill2)) {
                if (textScale < 1.9f) {
                    OnboardingImage(R.drawable.onboarding_more_button, size = null)
                    OnboardingImage(
                        R.drawable.onboarding_halo,
                        size = moreHaloSizeDp,
                        offsetY = haloOffset
                    )
                }
                OnboardingContentColumn {
                    OnboardingPieces.PageDescription(
                        R.string.onboarding_feedback_header,
                        R.string.onboarding_feedback_body
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OnboardingPieces.KeyButton(
                        R.string.onboarding_feedback_advance,
                        onClick = advance,
                    )
                }
            }
        }
        OnboardingScreen.HideMaps -> {
            OnboardingPieces.PageBox(painterResource(R.mipmap.onboarding_background_map)) {
                OnboardingPieces.PageDescription(
                    R.string.onboarding_hide_maps_header,
                    R.string.onboarding_hide_maps_body,
                    Modifier.align(Alignment.Center)
                        .offset(y = haloOffset)
                        .padding(horizontal = 32.dp)
                        .background(colorResource(R.color.fill2), shape = RoundedCornerShape(32.dp))
                        .padding(32.dp)
                )
                OnboardingContentColumn {
                    OnboardingPieces.KeyButton(
                        R.string.onboarding_hide_maps_hide,
                        onClick = { hideMaps(true) },
                    )
                    OnboardingPieces.SecondaryButton(
                        R.string.onboarding_hide_maps_show,
                        onClick = { hideMaps(false) },
                    )
                }
            }
        }
        OnboardingScreen.Location -> {
            OnboardingPieces.PageBox(painterResource(R.mipmap.onboarding_background_map)) {
                if (textScale < 1.5f) {
                    OnboardingImage(
                        R.drawable.onboarding_halo,
                        size = locationHaloSizeDp,
                        offsetY = haloOffset
                    )
                    OnboardingImage(R.drawable.onboarding_transit_lines, size = null)
                }
                OnboardingContentColumn {
                    OnboardingPieces.PageDescription(
                        R.string.onboarding_location_header,
                        R.string.onboarding_location_body
                    )
                    OnboardingPieces.KeyButton(
                        R.string.onboarding_location_advance,
                        onClick = ::shareLocation,
                    )
                    Text(
                        stringResource(R.string.onboarding_location_footer),
                        style = Typography.body
                    )
                }
            }
        }
        OnboardingScreen.StationAccessibility -> {
            OnboardingPieces.PageBox(painterResource(R.mipmap.onboarding_background_map)) {
                if (textScale < 2f) {
                    OnboardingImage(
                        R.drawable.accessibility_icon_accessible,
                        size = 192.dp,
                        offsetY = haloOffset
                    )
                }
                OnboardingContentColumn {
                    OnboardingPieces.PageDescription(
                        R.string.onboarding_station_accessibility_header,
                        R.string.onboarding_station_accessibility_body
                    )
                    OnboardingPieces.KeyButton(
                        R.string.onboarding_station_accessibility_show,
                        onClick = { showStationAccessibility(true) }
                    )
                    OnboardingPieces.SecondaryButton(
                        R.string.onboarding_station_accessibility_hide,
                        onClick = { showStationAccessibility(false) },
                    )
                }
            }
        }
    }
}

@Preview(name = "Feedback")
@Composable
private fun OnboardingScreenViewFeedbackPreview() {
    MyApplicationTheme {
        OnboardingScreenView(
            OnboardingScreen.Feedback,
            advance = {},
            locationDataManager = LocationDataManager(),
            settingsRepository = MockSettingsRepository()
        )
    }
}

@Preview(name = "HideMaps")
@Composable
private fun OnboardingScreenViewHideMapsPreview() {
    MyApplicationTheme {
        OnboardingScreenView(
            OnboardingScreen.HideMaps,
            advance = {},
            locationDataManager = LocationDataManager(),
            settingsRepository = MockSettingsRepository()
        )
    }
}

@Preview(name = "Location")
@Composable
private fun OnboardingScreenViewLocationPreview() {
    MyApplicationTheme {
        OnboardingScreenView(
            OnboardingScreen.Location,
            advance = {},
            locationDataManager = LocationDataManager(),
            settingsRepository = MockSettingsRepository()
        )
    }
}

@Preview(name = "StationAccessibility")
@Composable
private fun OnboardingScreenViewStationAccessibilityPreview() {
    MyApplicationTheme {
        OnboardingScreenView(
            OnboardingScreen.StationAccessibility,
            advance = {},
            locationDataManager = LocationDataManager(),
            settingsRepository = MockSettingsRepository()
        )
    }
}
