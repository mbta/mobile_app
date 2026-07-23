package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
import com.mbta.tid.mbta_app.android.testUtils.hasTextMatching
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
import kotlin.time.Duration.Companion.minutes
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

        composeTestRule.onNodeWithText("Detour ahead").assertCanBeDisplayed().performClick()
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

        composeTestRule.onNodeWithText("Suspension").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Alert header").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("View details").performClick()
        assertTrue { onViewDetailsClicked }
    }

    @Test
    fun testWarningAlertCard() {
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

        composeTestRule.onNodeWithText("Detour").assertCanBeDisplayed().performClick()
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

        composeTestRule.onNodeWithText("Alert header").assertCanBeDisplayed().performClick()
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
            .assertCanBeDisplayed()
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

        composeTestRule.onNodeWithText("Delays due to heavy ridership").assertCanBeDisplayed()
    }

    @Test
    fun testUpcomingDelayAlertCard() {
        val time = EasternTimeInstant(2025, Month.APRIL, 2, 9, 0)
        val alert =
            ObjectCollectionBuilder.Single.alert {
                activePeriod(time, null)
                header = "Alert header"
                effect = Alert.Effect.Delay
                cause = Alert.Cause.HeavyRidership
            }

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
                now = time - 15.minutes,
            )
        }

        composeTestRule
            .onNode(
                hasTextMatching(
                    Regex("Delay on Red Line starting 9:00\\sAM today", RegexOption.IGNORE_CASE)
                )
            )
            .assertCanBeDisplayed()
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

        composeTestRule.onNodeWithText("Delays").assertCanBeDisplayed()
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

        composeTestRule.onNodeWithText("Single Tracking").assertCanBeDisplayed()
    }

    @Test
    fun testMajorAlertCardSummary() {
        val alert =
            ObjectCollectionBuilder.Single.alert {
                header = "Alert header"
                effect = Alert.Effect.Suspension
            }

        // Fixed time so we can have a specific day of the week (sat)
        val endTime = EasternTimeInstant(2025, Month.APRIL, 6, 3, 0)
        composeTestRule.setContent {
            AlertCard(
                alert,
                AlertSummary.Standard(alert.effect, null, AlertSummary.Timeframe.ThisWeek(endTime)),
                AlertCardSpec.Takeover,
                routeAccents,
                {},
            )
        }

        composeTestRule.onNodeWithText("Suspension").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Service suspended through Saturday").assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Alert header").assertDoesNotExist()
        composeTestRule.onNodeWithText("View details").performClick()
    }

    @Test
    fun testWarningAlertCardSummary() {
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
            .onNode(hasTextMatching(Regex("Detour through 9:00\\sAM", RegexOption.IGNORE_CASE)))
            .assertCanBeDisplayed()
        composeTestRule.onNodeWithText("Alert header").assertIsNotDisplayed()
    }

    @Test
    fun testAllClearAlertCard() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert {
            effect = Alert.Effect.Suspension
            allClear()
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
            .assertCanBeDisplayed()
    }

    @Test
    fun testUpdateAlertCard() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert {
            effect = Alert.Effect.Shuttle
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
            .assertCanBeDisplayed()
    }

    @Test
    fun testTripCancellationAlertCard() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert {
            cause = Alert.Cause.MechanicalIssue
            effect = Alert.Effect.Cancellation
            informedEntity(trip = "trip")
        }
        composeTestRule.setContent {
            AlertCard(
                alert,
                TripSpecificAlertSummary(
                    TripSpecificAlertSummary.TripFrom(
                        EasternTimeInstant(2026, Month.MARCH, 9, 12, 13),
                        RouteType.COMMUTER_RAIL,
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
        composeTestRule.onNodeWithText("Train cancelled").assertCanBeDisplayed()

        composeTestRule
            .onNode(
                hasTextMatching(
                    Regex(
                        "12:13\\sPM train from Ruggles is cancelled today due to mechanical issue",
                        RegexOption.IGNORE_CASE,
                    )
                )
            )
            .assertCanBeDisplayed()
    }

    @Test
    fun testMultipleTripSuspensionAlertCard() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert {
            cause = Alert.Cause.Holiday
            effect = Alert.Effect.Suspension
            informedEntity(trip = "trip")
        }
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
        composeTestRule.onNodeWithText("Train suspended").assertCanBeDisplayed()

        composeTestRule
            .onNodeWithText("Multiple trips are suspended today due to holiday")
            .assertCanBeDisplayed()
    }

    @Test
    fun testTripShuttleAlertCard() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert {
            effect = Alert.Effect.Shuttle
            informedEntity(trip = "trip")
        }
        composeTestRule.setContent {
            AlertCard(
                alert,
                TripShuttleAlertSummary(
                    TripShuttleAlertSummary.SingleTrip(
                        EasternTimeInstant(2026, Month.MARCH, 9, 12, 13),
                        RouteType.COMMUTER_RAIL,
                        "Oak Grove",
                    ),
                    "Ruggles",
                    "Forest Hills",
                ),
                AlertCardSpec.Takeover,
                routeAccents.copy(type = RouteType.COMMUTER_RAIL),
                onViewDetails = {},
            )
        }
        composeTestRule.onNodeWithText("Shuttle bus").assertCanBeDisplayed()

        composeTestRule
            .onNode(
                hasTextMatching(
                    Regex(
                        "12:13\\sPM train from Oak Grove is replaced by shuttle buses from Ruggles to Forest Hills",
                        RegexOption.IGNORE_CASE,
                    )
                )
            )
            .assertCanBeDisplayed()
    }

    @Test
    fun testThisTripShuttleAlertCard() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert {
            effect = Alert.Effect.Shuttle
            informedEntity(trip = "trip")
        }
        composeTestRule.setContent {
            AlertCard(
                alert,
                TripShuttleAlertSummary(
                    TripShuttleAlertSummary.SingleTrip(
                        EasternTimeInstant(2026, Month.MARCH, 9, 12, 13),
                        RouteType.COMMUTER_RAIL,
                        null,
                    ),
                    "Ruggles",
                    "Forest Hills",
                ),
                AlertCardSpec.Takeover,
                routeAccents.copy(type = RouteType.COMMUTER_RAIL),
                onViewDetails = {},
            )
        }
        composeTestRule.onNodeWithText("Shuttle bus").assertCanBeDisplayed()

        composeTestRule
            .onNode(
                hasTextMatching(
                    Regex(
                        "Shuttle buses replace the 12:13\\sPM train from Ruggles to Forest Hills",
                        RegexOption.IGNORE_CASE,
                    )
                )
            )
            .assertCanBeDisplayed()
    }

    @Test
    fun testTripStationBypassAlertCard() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert {
            effect = Alert.Effect.StationClosure
            informedEntity(trip = "trip")
        }
        composeTestRule.setContent {
            AlertCard(
                alert,
                TripSpecificAlertSummary(
                    TripSpecificAlertSummary.TripTo(
                        EasternTimeInstant(2026, Month.MARCH, 9, 12, 13),
                        RouteType.COMMUTER_RAIL,
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
        composeTestRule.onNodeWithText("Stop skipped").assertCanBeDisplayed()

        composeTestRule
            .onNode(
                hasTextMatching(
                    Regex(
                        "12:13\\sPM train to Stoughton will not stop at Back Bay and Ruggles today",
                        RegexOption.IGNORE_CASE,
                    )
                )
            )
            .assertCanBeDisplayed()
    }

    @Test
    fun testStationBypassAlertCard() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert { effect = Alert.Effect.StationClosure }
        composeTestRule.setContent {
            AlertCard(
                alert,
                AlertSummary.Standard(
                    Alert.Effect.StationClosure,
                    AlertSummary.Location.AffectedStops(listOf("Back Bay", "Ruggles")),
                    AlertSummary.Timeframe.UntilFurtherNotice,
                ),
                AlertCardSpec.Takeover,
                routeAccents.copy(type = RouteType.COMMUTER_RAIL),
                onViewDetails = {},
            )
        }
        composeTestRule.onNodeWithText("Stop Skipped").assertCanBeDisplayed()

        composeTestRule
            .onNodeWithText("Trains will not stop at Back Bay and Ruggles until further notice")
            .assertCanBeDisplayed()
    }

    @Test
    fun testStopBypassAlertCard() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert { effect = Alert.Effect.StopClosure }
        composeTestRule.setContent {
            AlertCard(
                alert,
                AlertSummary.Standard(
                    Alert.Effect.StopClosure,
                    AlertSummary.Location.AffectedStops(listOf("Back Bay", "Ruggles")),
                    AlertSummary.Timeframe.UntilFurtherNotice,
                ),
                AlertCardSpec.Takeover,
                routeAccents.copy(type = RouteType.BUS),
                onViewDetails = {},
            )
        }
        composeTestRule.onNodeWithText("Stop Skipped").assertCanBeDisplayed()

        composeTestRule
            .onNodeWithText("Buses will not stop at Back Bay and Ruggles until further notice")
            .assertCanBeDisplayed()
    }

    @Test
    fun testTripSpecificReminder() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert {
            cause = Alert.Cause.MechanicalIssue
            effect = Alert.Effect.Cancellation
            informedEntity(trip = "trip")
        }
        composeTestRule.setContent {
            AlertCard(
                alert,
                TripSpecificAlertSummary(
                    TripSpecificAlertSummary.TripFrom(
                        EasternTimeInstant(2026, Month.MARCH, 9, 12, 13),
                        RouteType.COMMUTER_RAIL,
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
        composeTestRule.onNodeWithText("Train cancelled").assertCanBeDisplayed()

        composeTestRule
            .onNode(
                hasTextMatching(
                    Regex(
                        "12:13\\sPM train from Ruggles is cancelled tomorrow due to mechanical issue",
                        RegexOption.IGNORE_CASE,
                    )
                )
            )
            .assertCanBeDisplayed()
    }

    @Test
    fun testTripShuttleRecurrence() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert {
            effect = Alert.Effect.Shuttle
            informedEntity(trip = "trip")
        }
        composeTestRule.setContent {
            AlertCard(
                alert,
                TripShuttleAlertSummary(
                    TripShuttleAlertSummary.SingleTrip(
                        EasternTimeInstant(2026, Month.MARCH, 9, 12, 13),
                        RouteType.COMMUTER_RAIL,
                        "Oak Grove",
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
        composeTestRule.onNodeWithText("Shuttle bus").assertCanBeDisplayed()

        composeTestRule
            .onNode(
                hasTextMatching(
                    Regex(
                        "12:13\\sPM train from Oak Grove is replaced by shuttle buses from Ruggles to Forest Hills daily until Thursday",
                        RegexOption.IGNORE_CASE,
                    )
                )
            )
            .assertCanBeDisplayed()
    }

    @Test
    fun testThisTripShuttleRecurrence() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert {
            effect = Alert.Effect.Shuttle
            informedEntity(trip = "trip")
        }
        composeTestRule.setContent {
            AlertCard(
                alert,
                TripShuttleAlertSummary(
                    TripShuttleAlertSummary.ThisTrip(RouteType.COMMUTER_RAIL),
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
        composeTestRule.onNodeWithText("Shuttle bus").assertCanBeDisplayed()

        composeTestRule
            .onNode(
                hasTextMatching(
                    Regex(
                        "Shuttle buses replace this train from Ruggles to Forest Hills daily until Thursday"
                    )
                )
            )
            .assertCanBeDisplayed()
    }
}
