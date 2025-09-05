package com.mbta.tid.mbta_app.android

import androidx.compose.ui.test.junit4.createComposeRule
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.AppVersion
import com.mbta.tid.mbta_app.repositories.DefaultTab
import com.mbta.tid.mbta_app.repositories.ITabPreferencesRepository
import com.mbta.tid.mbta_app.repositories.MockCurrentAppVersionRepository
import com.mbta.tid.mbta_app.repositories.MockLastLaunchedAppVersionRepository
import com.mbta.tid.mbta_app.repositories.MockOnboardingRepository
import com.mbta.tid.mbta_app.usecases.FeaturePromoUseCase
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest

class ContentViewModelTests : KoinTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testDefaultTabNearbyIfNotShownPromo() = runBlocking {
        val tabPreferencesRepository = mock<ITabPreferencesRepository>(MockMode.autofill)
        everySuspend { tabPreferencesRepository.getDefaultTab() } returns DefaultTab.Nearby

        lateinit var vm: ContentViewModel
        composeTestRule.setContent {
            vm =
                ContentViewModel(
                    featurePromoUseCase =
                        FeaturePromoUseCase(
                            MockCurrentAppVersionRepository(AppVersion(4u, 0u, 0u)),
                            // Favorites promo in version 2.0.0
                            MockLastLaunchedAppVersionRepository(AppVersion(3u, 0u, 0u)),
                        ),
                    onboardingRepository = MockOnboardingRepository(),
                    tabPreferencesRepository = tabPreferencesRepository,
                )
        }

        composeTestRule.waitUntilDefaultTimeout { vm.defaultTab.value == DefaultTab.Nearby }
    }

    @Test
    fun testDefaultTabFavoritesIfShownPromo() = runBlocking {
        val tabPreferencesRepository = mock<ITabPreferencesRepository>(MockMode.autofill)
        everySuspend { tabPreferencesRepository.getDefaultTab() } returns DefaultTab.Nearby

        lateinit var vm: ContentViewModel
        composeTestRule.setContent {
            vm =
                ContentViewModel(
                    featurePromoUseCase =
                        FeaturePromoUseCase(
                            MockCurrentAppVersionRepository(AppVersion(2u, 0u, 0u)),
                            MockLastLaunchedAppVersionRepository(AppVersion(1u, 0u, 0u)),
                        ),
                    onboardingRepository = MockOnboardingRepository(),
                    tabPreferencesRepository = tabPreferencesRepository,
                )
        }

        composeTestRule.waitUntilDefaultTimeout { vm.defaultTab.value == DefaultTab.Favorites }
    }
}
