package com.mbta.tid.mbta_app.android.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.android.location.LocationDataManager
import com.mbta.tid.mbta_app.android.onboarding.OnboardingScreenView
import com.mbta.tid.mbta_app.model.OnboardingScreen
import com.mbta.tid.mbta_app.repositories.IOnboardingRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    var selectedIndex by remember { mutableStateOf<Int?>(0) }
    val coroutineScope = rememberCoroutineScope()
    val advanceMutex = Mutex()

    val screen = selectedIndex?.let { screens[it] } ?: return
    OnboardingScreenView(
        screen,
        { from ->
            coroutineScope.launch {
                advanceMutex.withLock {
                    val index = selectedIndex ?: return@launch
                    val latestScreen = screens[index]
                    if (from != latestScreen) return@launch

                    onboardingRepository.markOnboardingCompleted(latestScreen)

                    if (index < screens.lastIndex) {
                        selectedIndex = index + 1
                        onAdvance()
                    } else {
                        selectedIndex = null
                        onFinish()
                    }
                }
            }
        },
        locationDataManager,
        skipLocationDialogue,
    )
}
