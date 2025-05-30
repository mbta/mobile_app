package com.mbta.tid.mbta_app.android.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.android.location.LocationDataManager
import com.mbta.tid.mbta_app.android.onboarding.OnboardingScreenView
import com.mbta.tid.mbta_app.model.OnboardingScreen
import com.mbta.tid.mbta_app.repositories.IOnboardingRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun OnboardingPage(
    screens: List<OnboardingScreen>,
    locationDataManager: LocationDataManager,
    onFinish: () -> Unit,
    onAdvance: () -> Unit = {},
    onboardingRepository: IOnboardingRepository = koinInject(),
    skipLocationDialogue: Boolean = false,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val screen = screens[selectedIndex]
    OnboardingScreenView(
        screen,
        {
            coroutineScope.launch {
                onboardingRepository.markOnboardingCompleted(screen)
                if (selectedIndex < screens.size - 1) {
                    selectedIndex += 1
                    onAdvance()
                } else {
                    onFinish()
                }
            }
        },
        locationDataManager,
        skipLocationDialogue,
    )
}
