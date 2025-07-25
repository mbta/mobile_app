package com.mbta.tid.mbta_app.android

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.rule.GrantPermissionRule
import com.mbta.tid.mbta_app.android.location.MockFusedLocationProviderClient
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.android.util.LocalLocationClient
import com.mbta.tid.mbta_app.model.FeaturePromo
import com.mbta.tid.mbta_app.network.MockPhoenixSocket
import com.mbta.tid.mbta_app.repositories.MockOnboardingRepository
import com.mbta.tid.mbta_app.usecases.IFeaturePromoUseCase
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.test.KoinTest

@OptIn(ExperimentalTestApi::class)
class ContentViewTests : KoinTest {

    @get:Rule
    val runtimePermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)
    @get:Rule val composeTestRule = createComposeRule()

    val koinApplication = testKoinApplication()

    @Test
    fun testSwitchingTabs() {
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                CompositionLocalProvider(
                    LocalLocationClient provides MockFusedLocationProviderClient()
                ) {
                    ContentView()
                }
            }
        }

        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("MBTA Go").assertIsDisplayed()

        composeTestRule.onNodeWithText("Nearby").performClick()
        composeTestRule.onNodeWithText("Nearby Transit").assertIsDisplayed()
    }

    @Test
    fun testShowsPromos() {

        val mockFeaturePromos = mock<IFeaturePromoUseCase>(MockMode.autofill)

        everySuspend { mockFeaturePromos.getFeaturePromos() } returns
            listOf(FeaturePromo.EnhancedFavorites)

        val vm =
            ContentViewModel(
                featurePromoUseCase = mockFeaturePromos,
                onboardingRepository = MockOnboardingRepository(),
            )

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                CompositionLocalProvider(
                    LocalLocationClient provides MockFusedLocationProviderClient()
                ) {
                    ContentView(viewModel = vm)
                }
            }
        }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("Add your favorites"))
        composeTestRule
            .onNodeWithText("Now save your frequently used stops", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testSocketClosedOnPause() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        var onAttachCount = 0
        var onDetatchCount = 0

        val koinApplication =
            testKoinApplication(
                socket = MockPhoenixSocket({ onAttachCount += 1 }, { onDetatchCount += 1 })
            )

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                CompositionLocalProvider(
                    LocalLocationClient provides MockFusedLocationProviderClient(),
                    LocalLifecycleOwner provides lifecycleOwner,
                ) {
                    ContentView()
                }
            }
        }

        composeTestRule.waitUntil { onAttachCount == 1 && onDetatchCount == 0 }

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }

        composeTestRule.waitUntil { onAttachCount == 1 && onDetatchCount == 1 }

        composeTestRule.runOnIdle { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }

        composeTestRule.waitUntil { onAttachCount == 2 && onDetatchCount == 1 }
    }
}
