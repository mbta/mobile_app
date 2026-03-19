package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.hasTextMatching
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertCardSpec
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.Facility
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.TripShuttleAlertSummary
import com.mbta.tid.mbta_app.model.TripSpecificAlertSummary
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.Test

class AlertCardTests {

    @get:Rule val composeTestRule = createComposeRule()

    private val routeAccents =
        TripRouteAccents(Color.fromHex("ED8B00"), Color.fromHex("FFFFFF"), RouteType.BUS)

    @Test
    fun testDownstreamAlertCard() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Detour
            }
        var onViewDetailsClicked = false
        composeTestRule.setContent {
            AlertCard(
                alert,
                null,
                AlertCardSpec.Downstream,
                routeAccents,
                { onViewDetailsClicked = true },
            )
        }

        composeTestRule.onNodeWithText("Detour ahead").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Alert header").assertIsNotDisplayed()
        assertTrue { onViewDetailsClicked }
    }

    @Test
    fun testMajorAlertCard() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Suspension
            }
        var onViewDetailsClicked = false
        composeTestRule.setContent {
            AlertCard(
                alert,
                null,
                AlertCardSpec.Takeover,
                routeAccents,
                { onViewDetailsClicked = true },
            )
        }

        composeTestRule.onNodeWithText("Suspension").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alert header").assertIsDisplayed()
        composeTestRule.onNodeWithText("View details").performClick()
        assertTrue { onViewDetailsClicked }
    }

    @Test
    fun testSecondaryAlertCard() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Detour
            }
        var onViewDetailsClicked = false
        composeTestRule.setContent {
            AlertCard(
                alert,
                null,
                AlertCardSpec.Basic,
                routeAccents,
                { onViewDetailsClicked = true },
            )
        }

        composeTestRule.onNodeWithText("Detour").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Alert header").assertIsNotDisplayed()
        assertTrue { onViewDetailsClicked }
    }

    @Test
    fun testElevatorAlertCard() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.ElevatorClosure
            }
        var onViewDetailsClicked = false
        composeTestRule.setContent {
            AlertCard(
                alert,
                null,
                AlertCardSpec.Elevator,
                routeAccents,
                { onViewDetailsClicked = true },
            )
        }

        composeTestRule.onNodeWithText("Alert header").assertIsDisplayed().performClick()
        assertTrue { onViewDetailsClicked }
    }

    @Test
    fun testElevatorAlertWithFacilityCard() {
        val facility =
            ObjectCollectionBuilder.Single.facility {
                type = Facility.Type.Elevator
                shortName = "Elevator name"
            }
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.ElevatorClosure
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.UsingWheelchair),
                    facility = facility.id,
                )
                facilities = mapOf(facility.id to facility)
            }
        var onViewDetailsClicked = false
        composeTestRule.setContent {
            AlertCard(
                alert,
                null,
                AlertCardSpec.Elevator,
                routeAccents,
                { onViewDetailsClicked = true },
            )
        }

        composeTestRule
            .onNodeWithText("Elevator closure (Elevator name)")
            .assertIsDisplayed()
            .performClick()
        assertTrue { onViewDetailsClicked }
    }

    @Test
    fun testDelayAlertCard() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Delay
                cause = Alert.Cause.HeavyRidership
            }
        composeTestRule.setContent { AlertCard(alert, null, AlertCardSpec.Delay, routeAccents, {}) }

        composeTestRule.onNodeWithText("Delays due to heavy ridership").assertIsDisplayed()
    }

    @Test
    fun testUpcomingDelayAlertCard() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Delay
                cause = Alert.Cause.HeavyRidership
            }

        val time = EasternTimeInstant(2025, Month.APRIL, 2, 9, 0)
        composeTestRule.setContent {
            AlertCard(
                alert,
                AlertSummary.Standard(
                    Alert.Effect.Delay,
                    AlertSummary.Location.WholeRoute("Red Line", RouteType.HEAVY_RAIL),
                    timeframe = AlertSummary.Timeframe.StartingLaterToday(time),
                ),
                AlertCardSpec.Delay,
                routeAccents,
                {},
            )
        }

        composeTestRule
            .onNode(hasTextMatching(Regex("Delay on Red Line starting 9:00\\sAM today")))
            .assertIsDisplayed()
    }

    @Test
    fun testDelayAlertCardUnknownCause() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Delay
                cause = Alert.Cause.UnknownCause
            }
        composeTestRule.setContent { AlertCard(alert, null, AlertCardSpec.Delay, routeAccents, {}) }

        composeTestRule.onNodeWithText("Delays").assertIsDisplayed()
    }

    @Test
    fun testSingleTrackingInfoDelay() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Delay
                cause = Alert.Cause.SingleTracking
                severity = 1
            }
        composeTestRule.setContent { AlertCard(alert, null, AlertCardSpec.Delay, routeAccents, {}) }

        composeTestRule.onNodeWithText("Single Tracking").assertIsDisplayed()
    }

    @Test
    fun testMajorAlertCardSummary() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Suspension
            }

        // Fixed time so we can have a specific day of the week (sat)
        val endTime = EasternTimeInstant(2025, Month.APRIL, 5, 3, 0)
        composeTestRule.setContent {
            AlertCard(
                alert,
                AlertSummary.Standard(alert.effect, null, AlertSummary.Timeframe.ThisWeek(endTime)),
                AlertCardSpec.Takeover,
                routeAccents,
                {},
            )
        }

        composeTestRule.onNodeWithText("Suspension").assertIsDisplayed()
        composeTestRule.onNodeWithText("Service suspended through Saturday").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alert header").assertDoesNotExist()
        composeTestRule.onNodeWithText("View details").performClick()
    }

    @Test
    fun testSecondaryAlertCardSummary() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Detour
            }
        val endTime = EasternTimeInstant(2025, Month.APRIL, 2, 9, 0)
        composeTestRule.setContent {
            AlertCard(
                alert,
                AlertSummary.Standard(alert.effect, null, AlertSummary.Timeframe.Time(endTime)),
                AlertCardSpec.Basic,
                routeAccents,
                {},
            )
        }

        composeTestRule
            .onNode(hasTextMatching(Regex("Detour through 9:00\\sAM")))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Alert header").assertIsNotDisplayed()
    }

    @Test
    fun testAllClearAlertCard() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                activePeriod(now - 3.days, now - 1.days)
            }
        composeTestRule.setContent {
            AlertCard(
                alert,
                AlertSummary.AllClear(
                    location =
                        AlertSummary.Location.SuccessiveStops(
                            startStopName = "Start Stop",
                            endStopName = "End Stop",
                        )
                ),
                AlertCardSpec.Takeover,
                routeAccents,
                onViewDetails = {},
            )
        }

        composeTestRule
            .onNodeWithText("All clear: Regular service from Start Stop to End Stop")
            .assertIsDisplayed()
    }

    @Test
    fun testUpdateAlertCard() {
        val now = EasternTimeInstant.now()
        val objects = ObjectCollectionBuilder()
        val alert =
            objects.alert {
                effect = Alert.Effect.Shuttle
                activePeriod(now - 3.days, now + 3.days)
            }
        composeTestRule.setContent {
            AlertCard(
                alert,
                AlertSummary.Standard(
                    effect = Alert.Effect.Shuttle,
                    location =
                        AlertSummary.Location.SuccessiveStops(
                            startStopName = "Start Stop",
                            endStopName = "End Stop",
                        ),
                    timeframe = AlertSummary.Timeframe.Tomorrow,
                    recurrence = null,
                    isUpdate = true,
                ),
                AlertCardSpec.Takeover,
                routeAccents,
                onViewDetails = {},
            )
        }

        composeTestRule
            .onNodeWithText("Update: Shuttle buses from Start Stop to End Stop through tomorrow")
            .assertIsDisplayed()
    }

    @Test
    fun testTripCancellationAlertCard() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert { effect = Alert.Effect.Cancellation }
        composeTestRule.setContent {
            AlertCard(
                alert,
                TripSpecificAlertSummary(
                    TripSpecificAlertSummary.TripFrom(
                        EasternTimeInstant(2026, Month.MARCH, 9, 12, 13),
                        "Ruggles",
                    ),
                    Alert.Effect.Cancellation,
                    cause = Alert.Cause.MechanicalIssue,
                ),
                AlertCardSpec.Takeover,
                routeAccents.copy(type = RouteType.COMMUTER_RAIL),
                onViewDetails = {},
            )
        }
        composeTestRule.onNodeWithText("Train cancelled").assertIsDisplayed()

        composeTestRule
            .onNode(
                hasTextMatching(
                    Regex("12:13\\sPM from Ruggles is cancelled today due to mechanical issue")
                )
            )
            .assertIsDisplayed()
    }

    @Test
    fun testMultipleTripSuspensionAlertCard() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert { effect = Alert.Effect.Suspension }
        composeTestRule.setContent {
            AlertCard(
                alert,
                TripSpecificAlertSummary(
                    TripSpecificAlertSummary.MultipleTrips,
                    Alert.Effect.Suspension,
                    cause = Alert.Cause.Holiday,
                ),
                AlertCardSpec.Takeover,
                routeAccents.copy(type = RouteType.COMMUTER_RAIL),
                onViewDetails = {},
            )
        }
        composeTestRule.onNodeWithText("Train suspended").assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Multiple trips are suspended today due to holiday")
            .assertIsDisplayed()
    }

    @Test
    fun testTripShuttleAlertCard() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert { effect = Alert.Effect.Shuttle }
        composeTestRule.setContent {
            AlertCard(
                alert,
                TripShuttleAlertSummary(
                    TripShuttleAlertSummary.SingleTrip(
                        EasternTimeInstant(2026, Month.MARCH, 9, 12, 13),
                        RouteType.COMMUTER_RAIL,
                    ),
                    "Ruggles",
                    "Forest Hills",
                ),
                AlertCardSpec.Takeover,
                routeAccents.copy(type = RouteType.COMMUTER_RAIL),
                onViewDetails = {},
            )
        }
        composeTestRule.onNodeWithText("Shuttle bus").assertIsDisplayed()

        composeTestRule
            .onNode(
                hasTextMatching(
                    Regex(
                        "Shuttle buses replace the 12:13\\sPM train today from Ruggles to Forest Hills"
                    )
                )
            )
            .assertIsDisplayed()
    }

    @Test
    fun testTripStationBypassAlertCard() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert { effect = Alert.Effect.StationClosure }
        composeTestRule.setContent {
            AlertCard(
                alert,
                TripSpecificAlertSummary(
                    TripSpecificAlertSummary.TripTo(
                        EasternTimeInstant(2026, Month.MARCH, 9, 12, 13),
                        "Stoughton",
                    ),
                    Alert.Effect.StationClosure,
                    listOf("Back Bay", "Ruggles"),
                    cause = null,
                ),
                AlertCardSpec.Takeover,
                routeAccents.copy(type = RouteType.COMMUTER_RAIL),
                onViewDetails = {},
            )
        }
        composeTestRule.onNodeWithText("Stop skipped").assertIsDisplayed()

        composeTestRule
            .onNode(
                hasTextMatching(
                    Regex("12:13\\sPM to Stoughton will not stop at Back Bay and Ruggles today")
                )
            )
            .assertIsDisplayed()
    }

    @Test
    fun testTripSpecificReminder() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert { effect = Alert.Effect.Cancellation }
        composeTestRule.setContent {
            AlertCard(
                alert,
                TripSpecificAlertSummary(
                    TripSpecificAlertSummary.TripFrom(
                        EasternTimeInstant(2026, Month.MARCH, 9, 12, 13),
                        "Ruggles",
                    ),
                    Alert.Effect.Cancellation,
                    isToday = false,
                    cause = Alert.Cause.MechanicalIssue,
                ),
                AlertCardSpec.Takeover,
                routeAccents.copy(type = RouteType.COMMUTER_RAIL),
                onViewDetails = {},
            )
        }
        composeTestRule.onNodeWithText("Train cancelled").assertIsDisplayed()

        composeTestRule
            .onNode(
                hasTextMatching(
                    Regex("12:13\\sPM from Ruggles is cancelled tomorrow due to mechanical issue")
                )
            )
            .assertIsDisplayed()
    }

    @Test
    fun testTripShuttleRecurrence() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert { effect = Alert.Effect.Shuttle }
        composeTestRule.setContent {
            AlertCard(
                alert,
                TripShuttleAlertSummary(
                    TripShuttleAlertSummary.SingleTrip(
                        EasternTimeInstant(2026, Month.MARCH, 9, 12, 13),
                        RouteType.COMMUTER_RAIL,
                    ),
                    "Ruggles",
                    "Forest Hills",
                    recurrence =
                        AlertSummary.Recurrence.Daily(
                            ending =
                                AlertSummary.Timeframe.ThisWeek(
                                    EasternTimeInstant(2026, Month.MARCH, 12, 9, 18)
                                )
                        ),
                ),
                AlertCardSpec.Takeover,
                routeAccents.copy(type = RouteType.COMMUTER_RAIL),
                onViewDetails = {},
            )
        }
        composeTestRule.onNodeWithText("Shuttle bus").assertIsDisplayed()

        composeTestRule
            .onNode(
                hasTextMatching(
                    Regex(
                        "Shuttle buses replace the 12:13\\sPM train today from Ruggles to Forest Hills daily until Thursday"
                    )
                )
            )
            .assertIsDisplayed()
    }
}
