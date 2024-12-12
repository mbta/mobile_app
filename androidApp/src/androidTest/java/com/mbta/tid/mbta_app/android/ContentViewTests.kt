package com.mbta.tid.mbta_app.android

import android.app.Activity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
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
            module { single<PhoenixSocket> { MockPhoenixSocket() } }
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

    @Test
    fun testSocketClosedOnPause() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        var onAttachCount = 0
        var onDetatchCount = 0

        val koinApplication = koinApplication {
            modules(
                repositoriesModule(MockRepositories.buildWithDefaults()),
                module {
                    single<PhoenixSocket> {
                        MockPhoenixSocket({ onAttachCount += 1 }, { onDetatchCount += 1 })
                    }
                }
            )
        }

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                CompositionLocalProvider(
                    LocalActivity provides (LocalContext.current as Activity),
                    LocalLocationClient provides MockFusedLocationProviderClient(),
                    LocalLifecycleOwner provides lifecycleOwner
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
