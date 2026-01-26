package com.mbta.tid.mbta_app.android.onboarding

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.StarIcon
import com.mbta.tid.mbta_app.android.location.LocationDataManager
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.OnboardingScreen
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.datetime.Month
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreenView(
    screen: OnboardingScreen,
    advance: () -> Unit,
    locationDataManager: LocationDataManager,
    skipLocationDialogue: Boolean = false,
    settingsCache: SettingsCache = koinInject(),
) {
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

    fun shareLocation() {
        if (skipLocationDialogue || permissions.permissions.any { it.status.isGranted }) {
            advance()
        } else {
            sharingLocation = true
            permissions.launchMultiplePermissionRequest()
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
            label = "haloSizeMultiplier",
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
                        offsetY = haloOffset,
                    )
                }
                OnboardingContentColumn {
                    OnboardingPieces.PageDescription(
                        R.string.onboarding_feedback_header,
                        R.string.onboarding_feedback_body,
                        OnboardingPieces.Context.Onboarding,
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
            var localHideMapsSetting by rememberSaveable { mutableStateOf(true) }
            OnboardingPieces.PageBox(painterResource(R.mipmap.onboarding_background_map)) {
                OnboardingPieces.PageDescription(
                    R.string.onboarding_map_display_header,
                    R.string.onboarding_map_display_body,
                    OnboardingPieces.Context.Onboarding,
                    Modifier.align(Alignment.Center)
                        .padding(horizontal = 32.dp)
                        .background(colorResource(R.color.fill2), shape = RoundedCornerShape(32.dp))
                        .padding(32.dp),
                )
                OnboardingContentColumn {
                    OnboardingPieces.SettingsToggle(
                        currentSetting = !localHideMapsSetting,
                        toggleSetting = { localHideMapsSetting = !localHideMapsSetting },
                        label = stringResource(R.string.setting_toggle_map_display),
                    )
                    OnboardingPieces.KeyButton(
                        R.string.onboarding_continue,
                        onClick = {
                            settingsCache.set(Settings.HideMaps, localHideMapsSetting)
                            advance()
                        },
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
                        offsetY = haloOffset,
                    )
                    OnboardingImage(R.drawable.onboarding_transit_lines, size = null)
                }
                OnboardingContentColumn {
                    OnboardingPieces.PageDescription(
                        R.string.onboarding_location_header,
                        R.string.onboarding_location_body,
                        OnboardingPieces.Context.Onboarding,
                    )
                    OnboardingPieces.KeyButton(
                        R.string.onboarding_continue,
                        onClick = ::shareLocation,
                    )
                    Text(
                        stringResource(R.string.onboarding_location_footer),
                        color = colorResource(R.color.text),
                        style = Typography.body,
                    )
                }
            }
        }
        OnboardingScreen.NotificationsBeta -> {
            NotificationsBetaPage(advance)
        }
        OnboardingScreen.StationAccessibility -> {
            OnboardingPieces.PageBox(painterResource(R.mipmap.onboarding_background_map)) {
                OnboardingContentColumn {
                    if (textScale < 1.5f) {
                        Row(horizontalArrangement = Arrangement.Center) {
                            Image(
                                painterResource(R.drawable.accessibility_icon_accessible),
                                modifier = Modifier.size(192.dp).weight(1f),
                                contentDescription = null,
                            )
                        }
                    }
                    OnboardingPieces.PageDescription(
                        R.string.onboarding_station_accessibility_header,
                        R.string.onboarding_station_accessibility_body,
                        OnboardingPieces.Context.Onboarding,
                    )

                    val stationAccessibility = settingsCache.get(Settings.StationAccessibility)
                    OnboardingPieces.SettingsToggle(
                        currentSetting = stationAccessibility,
                        toggleSetting = {
                            settingsCache.set(Settings.StationAccessibility, !stationAccessibility)
                        },
                        label = stringResource(R.string.setting_station_accessibility),
                    )

                    OnboardingPieces.KeyButton(
                        R.string.onboarding_continue,
                        onClick = { advance() },
                    )
                }
            }
        }
    }
}

private enum class NotificationsScreenState {
    Initial,
    AfterFavorite,
    AfterSchedule,
    Final,
}

@Composable
private fun NotificationsBetaPage(advance: () -> Unit) {
    var screenState by remember { mutableStateOf(NotificationsScreenState.Initial) }
    val transition = updateTransition(screenState, label = "state")
    LaunchedEffect(null) {
        delay(2.seconds)
        screenState = NotificationsScreenState.AfterFavorite
        delay(2.seconds)
        screenState = NotificationsScreenState.AfterSchedule
        delay(2.seconds)
        screenState = NotificationsScreenState.Final
    }
    OnboardingPieces.PageBox(
        colorResource(if (isSystemInDarkTheme()) R.color.fill1 else R.color.fill2)
    ) {
        Column(
            Modifier.align(Alignment.TopCenter)
                .offset(y = (-200).dp)
                .scale(0.6f)
                .background(colorResource(R.color.key), RoundedCornerShape(32.dp))
                .border(16.dp, Color.Black, RoundedCornerShape(32.dp))
                .padding(8.dp)
        ) {
            val isDarkMode = isSystemInDarkTheme()
            Spacer(Modifier.height(250.dp))
            Row(
                Modifier.padding(16.dp)
                    .background(
                        if (isDarkMode) Color.Black.copy(alpha = 0.5f)
                        else Color.White.copy(alpha = 0.75f),
                        RoundedCornerShape(26.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(36.dp).clip(CircleShape)) {
                    Image(
                        painterResource(R.drawable.ic_launcher_background),
                        contentDescription = null,
                        modifier = Modifier.requiredSize(48.dp),
                    )
                    Image(
                        painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.requiredSize(48.dp),
                    )
                }

                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Orange Line",
                            color = colorResource(R.color.text),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Default,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "now",
                            Modifier.alpha(0.6f),
                            color = colorResource(R.color.text),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Default,
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            Modifier.alpha(0.6f).size(18.dp),
                        )
                    }
                    val alert =
                        FormattedAlert(
                            alert = null,
                            alertSummary =
                                AlertSummary(
                                    effect = Alert.Effect.Suspension,
                                    location =
                                        AlertSummary.Location.SuccessiveStops(
                                            startStopName = "Back Bay",
                                            endStopName = "Wellington",
                                        ),
                                    timeframe =
                                        AlertSummary.Timeframe.ThisWeek(
                                            EasternTimeInstant(2026, Month.JANUARY, 25, 12, 0)
                                        ),
                                ),
                        )
                    Text(
                        alert.alertCardMajorBody.toString(),
                        color = colorResource(R.color.text),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Default,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
        OnboardingContentColumn {
            val header = stringResource(R.string.promo_notifications_header)
            // setting explicit paneTitle resets focus to the text rather than the continue button
            // when navigating to the next screen
            // https://issuetracker.google.com/issues/272065229#comment8
            Spacer(Modifier.weight(1f))
            Column(
                Modifier.semantics { paneTitle = header },
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                val headerDescription =
                    stringResource(R.string.new_feature_screen_reader_header_prefix, header)
                Text(
                    header,
                    modifier =
                        Modifier.semantics {
                            contentDescription = headerDescription
                            heading()
                        },
                    color = colorResource(R.color.text),
                    style = Typography.title1Bold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val starColor by
                        transition.animateColor {
                            if (it >= NotificationsScreenState.AfterFavorite)
                                colorResource(R.color.key)
                            else colorResource(R.color.text).copy(alpha = 0.4f)
                        }
                    val textAlpha by
                        transition.animateFloat {
                            if (it >= NotificationsScreenState.AfterFavorite) 1f else 0.4f
                        }
                    StarIcon(
                        starred = screenState >= NotificationsScreenState.AfterFavorite,
                        color = starColor,
                        modifier = Modifier.width(52.dp),
                        size = 36.dp,
                    )
                    Text(
                        stringResource(R.string.promo_notifications_step1),
                        modifier = Modifier.alpha(textAlpha),
                        color = colorResource(R.color.text),
                        style = Typography.title3,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val textAlpha by
                        transition.animateFloat {
                            if (it >= NotificationsScreenState.AfterSchedule) 1f else 0.4f
                        }
                    Switch(
                        checked = screenState >= NotificationsScreenState.AfterSchedule,
                        onCheckedChange = null,
                    )
                    Text(
                        stringResource(R.string.promo_notifications_step2),
                        modifier = Modifier.alpha(textAlpha),
                        color = colorResource(R.color.text),
                        style = Typography.title3,
                    )
                }
                val textAlpha by
                    transition.animateFloat {
                        if (it >= NotificationsScreenState.Final) 1f else 0.4f
                    }
                Text(
                    stringResource(R.string.promo_notifications_body),
                    modifier = Modifier.alpha(textAlpha),
                    color = colorResource(R.color.text),
                    style = Typography.title3,
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                val composition by
                    rememberLottieComposition(
                        LottieCompositionSpec.RawRes(
                            if (isSystemInDarkTheme()) R.raw.notification_pop_dark
                            else R.raw.notification_pop_light
                        )
                    )
                val progress by
                    animateLottieCompositionAsState(
                        composition,
                        isPlaying = screenState >= NotificationsScreenState.Final,
                    )
                LottieAnimation(
                    composition,
                    progress = { progress },
                    Modifier.align(Alignment.BottomCenter),
                )
                Column { OnboardingPieces.KeyButton(R.string.got_it, onClick = advance) }
            }
        }
    }
}

@Preview(name = "Feedback")
@Composable
private fun OnboardingScreenViewFeedbackPreview() {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin { modules(module { single { SettingsCache(MockSettingsRepository()) } }) }
    }
    MyApplicationTheme {
        OnboardingScreenView(
            OnboardingScreen.Feedback,
            advance = {},
            locationDataManager = LocationDataManager(),
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
        )
    }
}

@Preview(name = "Notifications Beta")
@Composable
private fun OnboardingScreenViewNotificationsBetaPreview() {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin { modules(module { single { SettingsCache(MockSettingsRepository()) } }) }
    }
    MyApplicationTheme {
        OnboardingScreenView(
            OnboardingScreen.NotificationsBeta,
            advance = {},
            locationDataManager = LocationDataManager(),
        )
    }
}

@Preview(name = "StationAccessibility")
@Composable
private fun OnboardingScreenViewStationAccessibiityPreview() {
    MyApplicationTheme {
        OnboardingScreenView(
            OnboardingScreen.StationAccessibility,
            advance = {},
            locationDataManager = LocationDataManager(),
        )
    }
}
