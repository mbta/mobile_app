package com.mbta.tid.mbta_app.android.alertDetails

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class AlertDetailsPageTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testAlertDetailsPageParentStopResolution() {
        val objects = ObjectCollectionBuilder()
        val stop1 =
            objects.stop {
                name = "Stop 1"
                childStopIds = listOf("stop1a", "stop1b")
            }
        val stop1a =
            objects.stop {
                id = "stop1a"
                name = "Stop 1a"
                parentStationId = stop1.id
            }
        val stop1b =
            objects.stop {
                id = "stop1b"
                name = "Stop 1b"
                parentStationId = stop1.id
            }
        val stop2 =
            objects.stop {
                name = "Stop 2"
                childStopIds = listOf("stop2a")
            }
        val stop2a =
            objects.stop {
                id = "stop2a"
                name = "Stop 2a"
                parentStationId = stop2.id
            }
        val stop3 = objects.stop { name = "Stop 3" }

        val route = objects.route { longName = "Orange Line" }

        val now = Clock.System.now()

        val alert =
            objects.alert {
                activePeriod(now - 5.seconds, now + 5.seconds)
                description = "Long description"
                cause = Alert.Cause.Fire
                effect = Alert.Effect.StopClosure
                effectName = "Closure"
                header = "Alert header"
                informedEntity(
                    activities =
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Exit,
                            Alert.InformedEntity.Activity.Ride
                        ),
                    route = route.id,
                    stop = stop1.id
                )
                informedEntity(
                    activities =
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Exit,
                            Alert.InformedEntity.Activity.Ride
                        ),
                    directionId = 0,
                    route = route.id,
                    stop = stop1a.id
                )
                informedEntity(
                    activities =
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Exit,
                            Alert.InformedEntity.Activity.Ride
                        ),
                    directionId = 1,
                    route = route.id,
                    stop = stop1b.id
                )
                informedEntity(
                    activities =
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Exit,
                            Alert.InformedEntity.Activity.Ride
                        ),
                    route = route.id,
                    stop = stop2a.id
                )
                informedEntity(
                    activities =
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Exit,
                            Alert.InformedEntity.Activity.Ride
                        ),
                    route = route.id,
                    stop = stop3.id
                )
            }

        val koin = koinApplication {
            modules(
                module {
                    single<Analytics> { MockAnalytics() }
                    single<IErrorBannerStateRepository> { MockErrorBannerStateRepository() }
                    single<IGlobalRepository> {
                        MockGlobalRepository(response = GlobalResponse(objects))
                    }
                }
            )
        }

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                AlertDetailsPage(
                    alertId = alert.id,
                    line = null,
                    routes = listOf(route),
                    alerts = AlertsStreamDataResponse(objects),
                    stopId = null,
                    goBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Orange Line Stop Closure").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fire").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 affected stops").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 affected stops").performClick()
        composeTestRule.onNodeWithText("Stop 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stop 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stop 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stop 1a").assertDoesNotExist()
        composeTestRule.onNodeWithText("Stop 1b").assertDoesNotExist()
        composeTestRule.onNodeWithText("Stop 2a").assertDoesNotExist()
    }
}
