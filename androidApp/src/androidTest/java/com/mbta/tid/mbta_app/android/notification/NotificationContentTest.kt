package com.mbta.tid.mbta_app.android.notification

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import org.junit.Rule
import org.junit.Test
import org.koin.compose.koinInject

class NotificationContentTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testHasContent() {
        val objects = TestData.clone()
        val now = EasternTimeInstant.now()
        val alert =
            objects.alert {
                activePeriod(
                    now - 10.minutes,
                    EasternTimeInstant(
                        LocalDateTime(
                            (now + 1.days).serviceDate.plus(1, DateTimeUnit.DAY),
                            LocalTime(3, 0),
                        )
                    ),
                )
                cause = Alert.Cause.Maintenance
                effect = Alert.Effect.Shuttle
                val boardExitRide =
                    listOf(
                        Alert.InformedEntity.Activity.Board,
                        Alert.InformedEntity.Activity.Exit,
                        Alert.InformedEntity.Activity.Ride,
                    )
                val stations =
                    listOf(
                        "place-kencl",
                        "place-fenwy",
                        "place-longw",
                        "place-bvmnl",
                        "place-brkhl",
                        "place-bcnfd",
                        "place-rsmnl",
                        "place-chhil",
                        "place-newto",
                        "place-newtn",
                        "place-eliot",
                        "place-waban",
                        "place-woodl",
                        "place-river",
                    )
                for (station in stations) {
                    informedEntity(
                        boardExitRide,
                        directionId = 0,
                        route = "Green-D",
                        routeType = RouteType.LIGHT_RAIL,
                        stop = station,
                    )
                    informedEntity(
                        boardExitRide,
                        directionId = 1,
                        route = "Green-D",
                        routeType = RouteType.LIGHT_RAIL,
                        stop = station,
                    )
                }
                val westboundPlatforms =
                    listOf(
                        "70151",
                        "70187",
                        "70183",
                        "70181",
                        "70179",
                        "70177",
                        "70175",
                        "70173",
                        "70171",
                        "70169",
                        "70167",
                        "70165",
                        "70163",
                        "70161",
                    )
                val eastboundPlatforms =
                    listOf(
                        "70150",
                        "70186",
                        "70182",
                        "70180",
                        "70178",
                        "70176",
                        "70174",
                        "70172",
                        "70170",
                        "70168",
                        "70166",
                        "70164",
                        "70162",
                        "70160",
                    )
                for (platform in westboundPlatforms) {
                    informedEntity(
                        boardExitRide,
                        directionId = 0,
                        route = "Green-D",
                        routeType = RouteType.LIGHT_RAIL,
                        stop = platform,
                    )
                }
                for (platform in eastboundPlatforms) {
                    informedEntity(
                        boardExitRide,
                        directionId = 1,
                        route = "Green-D",
                        routeType = RouteType.LIGHT_RAIL,
                        stop = platform,
                    )
                }
            }

        loadKoinMocks(objects)

        var result: NotificationContent? = null
        composeTestRule.setContent {
            val context = LocalContext.current
            val alertsRepository: IAlertsRepository = koinInject()
            val globalRepository: IGlobalRepository = koinInject()
            LaunchedEffect(Unit) {
                result =
                    NotificationContent.build(
                        context,
                        alert.id,
                        listOf(RouteStopDirection(Line.Id("line-Green"), "place-boyls", 0)),
                        alertsRepository,
                        globalRepository,
                    )
            }
        }

        composeTestRule.waitUntilDefaultTimeout { result != null }
        val bold = SpanStyle(fontWeight = FontWeight.Bold)
        assertEquals(
            NotificationContent(
                AnnotatedString("Shuttle", bold),
                buildAnnotatedString {
                    withStyle(bold) { append("Shuttle buses") }
                    append(" from ")
                    withStyle(bold) { append("Kenmore") }
                    append(" to ")
                    withStyle(bold) { append("Riverside") }
                    append(" through tomorrow")
                },
            ),
            result,
        )
    }
}
