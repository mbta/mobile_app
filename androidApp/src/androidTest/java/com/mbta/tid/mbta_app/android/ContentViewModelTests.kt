package com.mbta.tid.mbta_app.android

import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.model.AppVersion
import com.mbta.tid.mbta_app.repositories.DefaultTab
import com.mbta.tid.mbta_app.repositories.ITabPreferencesRepository
import com.mbta.tid.mbta_app.repositories.MockCurrentAppVersionRepository
import com.mbta.tid.mbta_app.repositories.MockLastLaunchedAppVersionRepository
import com.mbta.tid.mbta_app.repositories.MockOnboardingRepository
import com.mbta.tid.mbta_app.usecases.FeaturePromoUseCase
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest

class ContentViewModelTests : KoinTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testDefaultTabSetsToNearbyAfterFirstLaunchWithFavorites() = runBlocking {
        val tabPreferencesRepository = mock<ITabPreferencesRepository>()
        var setTabCalledWith: DefaultTab? = null
        everySuspend { tabPreferencesRepository.getDefaultTab() } returns DefaultTab.Favorites
        everySuspend { tabPreferencesRepository.hasSeenFavorites() } returns false

        everySuspend { tabPreferencesRepository.setDefaultTab(any()) } calls
            {
                setTabCalledWith = it.arg(0)
            }

        composeTestRule.setContent {
            val vm =
                ContentViewModel(
                    featurePromoUseCase =
                        FeaturePromoUseCase(
                            MockCurrentAppVersionRepository(AppVersion(3u, 0u, 0u)),
                            MockLastLaunchedAppVersionRepository(AppVersion(1u, 0u, 0u)),
                        ),
                    onboardingRepository = MockOnboardingRepository(),
                    tabPreferencesRepository = tabPreferencesRepository,
                )
        }

        composeTestRule.waitUntil { setTabCalledWith == DefaultTab.Nearby }
    }

    @Test
    fun testDefaultTabSNotSetIfFavoritesAlreadySeen() = runBlocking {
        val tabPreferencesRepository = mock<ITabPreferencesRepository>()
        everySuspend { tabPreferencesRepository.getDefaultTab() } returns DefaultTab.Favorites
        everySuspend { tabPreferencesRepository.hasSeenFavorites() } returns true

        everySuspend { tabPreferencesRepository.setDefaultTab(any()) } calls {}

        composeTestRule.setContent {
            val vm =
                ContentViewModel(
                    featurePromoUseCase =
                        FeaturePromoUseCase(
                            MockCurrentAppVersionRepository(AppVersion(3u, 0u, 0u)),
                            MockLastLaunchedAppVersionRepository(AppVersion(1u, 0u, 0u)),
                        ),
                    onboardingRepository = MockOnboardingRepository(),
                    tabPreferencesRepository = tabPreferencesRepository,
                )
        }

        verifySuspend(VerifyMode.exhaustiveOrder) {
            tabPreferencesRepository.getDefaultTab()
            tabPreferencesRepository.hasSeenFavorites()
        }
    }
}
