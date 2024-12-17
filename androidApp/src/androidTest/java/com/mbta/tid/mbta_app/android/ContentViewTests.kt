package com.mbta.tid.mbta_app.android

import android.app.Activity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.mbta.tid.mbta_app.android.location.MockFusedLocationProviderClient
import com.mbta.tid.mbta_app.android.util.LocalActivity
import com.mbta.tid.mbta_app.android.util.LocalLocationClient
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.network.MockPhoenixSocket
import com.mbta.tid.mbta_app.network.PhoenixSocket
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.KoinTest

class ContentViewTests : KoinTest {

    @get:Rule
    val runtimePermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)
    @get:Rule val composeTestRule = createComposeRule()

    val koinApplication = koinApplication {
        modules(
            repositoriesModule(MockRepositories.buildWithDefaults()),
            MainApplication.koinViewModelModule,
            module { single<PhoenixSocket> { MockPhoenixSocket() } },
        )
    }

    @Test
    fun testSwitchingTabs() {
        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                CompositionLocalProvider(
                    LocalActivity provides (LocalContext.current as Activity),
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
}
