package com.mbta.tid.mbta_app.android.onboarding

import android.util.TypedValue
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import kotlin.math.roundToInt
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
    val isDarkTheme = isSystemInDarkTheme()

    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeight =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            screenHeightDp.value,
            LocalContext.current.resources.displayMetrics
        )
    val screenWidth =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            screenWidthDp.value,
            LocalContext.current.resources.displayMetrics
        )
    val bottomPadding = if (screenHeight < 812f) 16.dp else 52.dp
    val sidePadding = if (screenWidth < 393f) 16.dp else 32.dp
    val locationHaloSize = screenWidth * 0.8f
    val moreHaloSize = screenWidth * 0.55f
    val haloOffset =
        if ((screenWidth / screenHeight) >= 390f / 844f) {
            val scaledHeight = (screenWidth / 390f) * 844f
            -((scaledHeight / 2f) - (scaledHeight / 3f) - 18f)
        } else {
            -((screenHeight / 2f) - (screenHeight / 3f))
        }
    val haloOffsetDp = with(LocalDensity.current) { haloOffset.roundToInt().toDp() }
    val buttonModifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
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
    val moreHaloSizeDp =
        with(LocalDensity.current) { moreHaloSize.roundToInt().toDp() * haloSizeMultiplier.value }
    val locationHaloSizeDp =
        with(LocalDensity.current) {
            locationHaloSize.roundToInt().toDp() * haloSizeMultiplier.value
        }

    val textScale = with(LocalDensity.current) { 1.sp.toPx() / 1.dp.toPx() }

    Column {
        when (screen) {
            OnboardingScreen.Feedback -> {

                Box(modifier = Modifier.fillMaxSize().background(colorResource(R.color.fill2))) {
                    if (textScale < 1.9f) {
                        Image(
                            painterResource(
                                if (isDarkTheme) {
                                    R.drawable.onboarding_more_button_dark
                                } else {
                                    R.drawable.onboarding_more_button
                                }
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().align(Alignment.Center)
                        )
                        Image(
                            painterResource(
                                if (isDarkTheme) {
                                    R.drawable.onboarding_halo_dark
                                } else {
                                    R.drawable.onboarding_halo
                                }
                            ),
                            contentDescription = null,
                            modifier =
                                Modifier.width(moreHaloSizeDp)
                                    .height(moreHaloSizeDp)
                                    .absoluteOffset(y = haloOffsetDp)
                                    .align(Alignment.Center)
                        )
                    }
                    Column(
                        modifier =
                            Modifier.align(Alignment.BottomCenter)
                                .padding(
                                    start = sidePadding,
                                    top = 16.dp,
                                    end = sidePadding,
                                    bottom = bottomPadding
                                ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            stringResource(R.string.onboarding_feedback_header),
                            style = Typography.title1Bold
                        )
                        Text(
                            stringResource(R.string.onboarding_feedback_body),
                            style = Typography.title3
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            modifier = buttonModifier,
                            onClick = advance,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                stringResource(R.string.onboarding_feedback_advance),
                                color = colorResource(R.color.fill3),
                                style = Typography.bodySemibold
                            )
                        }
                    }
                }
            }
            OnboardingScreen.HideMaps -> {
                Column(
                    modifier =
                        Modifier.fillMaxSize()
                            .paint(
                                painter =
                                    painterResource(
                                        id =
                                            if (isDarkTheme) {
                                                R.mipmap.onboarding_background_map_dark
                                            } else {
                                                R.mipmap.onboarding_background_map
                                            }
                                    ),
                                alignment = Alignment.Center,
                                contentScale = ContentScale.Crop
                            ),
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        modifier =
                            Modifier.padding(start = sidePadding, end = sidePadding)
                                .background(
                                    colorResource(R.color.fill2),
                                    shape = RoundedCornerShape(32.dp)
                                ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            stringResource(R.string.onboarding_hide_maps_header),
                            modifier = Modifier.padding(start = 32.dp, top = 32.dp, end = 32.dp),
                            style = Typography.title1Bold
                        )
                        Text(
                            stringResource(R.string.onboarding_hide_maps_body),
                            style = Typography.title3,
                            modifier = Modifier.padding(start = 32.dp, bottom = 32.dp, end = 32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        modifier = buttonModifier.padding(start = 32.dp, end = 32.dp),
                        shape = RoundedCornerShape(8.dp),
                        onClick = { hideMaps(true) },
                    ) {
                        Text(
                            stringResource(R.string.onboarding_hide_maps_hide),
                            style = Typography.bodySemibold,
                            color = colorResource(R.color.fill3)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        modifier =
                            buttonModifier
                                .padding(start = 32.dp, end = 32.dp)
                                .border(
                                    1.dp,
                                    color = colorResource(R.color.key),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                        shape = RoundedCornerShape(8.dp),
                        onClick = { hideMaps(false) },
                        colors =
                            buttonColors(
                                containerColor = colorResource(R.color.fill1),
                                contentColor = colorResource(R.color.key)
                            )
                    ) {
                        Text(
                            stringResource(R.string.onboarding_hide_maps_show),
                            style = Typography.body
                        )
                    }
                    Spacer(modifier = Modifier.height(56.dp))
                }
            }
            OnboardingScreen.Location -> {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .paint(
                                painter =
                                    painterResource(
                                        id =
                                            if (isDarkTheme) {
                                                R.mipmap.onboarding_background_map_dark
                                            } else {
                                                R.mipmap.onboarding_background_map
                                            }
                                    ),
                                alignment = Alignment.Center,
                                contentScale = ContentScale.Crop
                            ),
                ) {
                    if (textScale < 1.5f) {
                        Image(
                            painter =
                                painterResource(
                                    id =
                                        if (isDarkTheme) {
                                            R.drawable.onboarding_transit_lines_dark
                                        } else {
                                            R.drawable.onboarding_transit_lines
                                        }
                                ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.align(Alignment.Center).fillMaxSize()
                        )
                        Image(
                            painter =
                                painterResource(
                                    id =
                                        if (isDarkTheme) {
                                            R.drawable.onboarding_halo_dark
                                        } else {
                                            R.drawable.onboarding_halo
                                        }
                                ),
                            contentDescription = null,
                            modifier =
                                Modifier.align(Alignment.Center)
                                    .width(locationHaloSizeDp)
                                    .height(locationHaloSizeDp)
                                    .absoluteOffset(y = haloOffsetDp)
                        )
                    }
                    Column(
                        modifier =
                            Modifier.align(Alignment.BottomCenter)
                                .padding(
                                    start = sidePadding,
                                    top = 16.dp,
                                    end = sidePadding,
                                    bottom = bottomPadding
                                ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            stringResource(R.string.onboarding_location_header),
                            style = Typography.title1Bold
                        )
                        Text(
                            stringResource(R.string.onboarding_location_body),
                            style = Typography.title3
                        )
                        Button(
                            modifier = buttonModifier,
                            shape = RoundedCornerShape(8.dp),
                            onClick = ::shareLocation,
                        ) {
                            Text(
                                stringResource(R.string.onboarding_location_advance),
                                color = colorResource(R.color.fill3),
                                style = Typography.bodySemibold
                            )
                        }
                        Text(
                            stringResource(R.string.onboarding_location_footer),
                            style = Typography.body
                        )
                    }
                }
            }
            OnboardingScreen.StationAccessibility -> {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .paint(
                                painter =
                                    painterResource(
                                        id =
                                            if (isDarkTheme) {
                                                R.mipmap.onboarding_background_map_dark
                                            } else {
                                                R.mipmap.onboarding_background_map
                                            }
                                    ),
                                alignment = Alignment.Center,
                                contentScale = ContentScale.Crop
                            ),
                ) {
                    if (textScale < 2f) {
                        Image(
                            painter =
                                painterResource(id = R.drawable.accessibility_icon_accessible),
                            contentDescription = null,
                            modifier =
                                Modifier.align(Alignment.Center)
                                    .size(192.dp)
                                    .absoluteOffset(y = haloOffsetDp)
                        )
                    }
                    Column(
                        modifier =
                            Modifier.align(Alignment.BottomCenter)
                                .padding(
                                    start = sidePadding,
                                    top = 16.dp,
                                    end = sidePadding,
                                    bottom = bottomPadding
                                ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            stringResource(R.string.onboarding_station_accessibility_header),
                            style = Typography.title1Bold
                        )
                        Text(
                            stringResource(R.string.onboarding_station_accessibility_body),
                            style = Typography.title3
                        )

                        Button(
                            modifier = buttonModifier,
                            shape = RoundedCornerShape(8.dp),
                            onClick = { showStationAccessibility(true) },
                            colors =
                                buttonColors(
                                    containerColor = colorResource(R.color.key),
                                    contentColor = colorResource(R.color.fill1)
                                )
                        ) {
                            Text(
                                stringResource(R.string.onboarding_station_accessibility_show),
                                Modifier.align(Alignment.CenterVertically),
                                textAlign = TextAlign.Center,
                                style = Typography.bodySemibold,
                                color = colorResource(R.color.fill3)
                            )
                        }
                        Button(
                            modifier =
                                buttonModifier.border(
                                    1.dp,
                                    color = colorResource(R.color.key),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            shape = RoundedCornerShape(8.dp),
                            onClick = { showStationAccessibility(false) },
                            colors =
                                buttonColors(
                                    containerColor = colorResource(R.color.fill1),
                                    contentColor = colorResource(R.color.key)
                                )
                        ) {
                            Text(
                                stringResource(R.string.onboarding_station_accessibility_hide),
                                Modifier.align(Alignment.CenterVertically),
                                textAlign = TextAlign.Center,
                                style = Typography.body
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun OnboardingScreenViewPreview() {
    MyApplicationTheme {
        OnboardingScreenView(
            OnboardingScreen.StationAccessibility,
            advance = {},
            locationDataManager = LocationDataManager(),
            settingsRepository = MockSettingsRepository()
        )
    }
}
