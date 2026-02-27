package com.mbta.tid.mbta_app.android.notification

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.mbta.tid.mbta_app.android.testUtils.waitUntilDefaultTimeout
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.AlertSummary
import com.mbta.tid.mbta_app.model.response.PushNotificationPayload
import kotlin.test.assertEquals
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test

class NotificationContentTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testHasContent() {
        var result: NotificationContent? = null
        composeTestRule.setContent {
            val resources = LocalResources.current
            LaunchedEffect(Unit) {
                result =
                    NotificationContent.build(
                        resources,
                        PushNotificationPayload(
                            PushNotificationPayload.Title.BareLabel("Green Line D"),
                            AlertSummary(
                                Alert.Effect.Shuttle,
                                AlertSummary.Location.SuccessiveStops(
                                    startStopName = "Kenmore",
                                    endStopName = "Riverside",
                                ),
                                AlertSummary.Timeframe.Tomorrow,
                            ),
                            "alert",
                            emptyList(),
                            PushNotificationPayload.NotificationType.Notification,
                            Instant.DISTANT_PAST,
                        ),
                    )
            }
        }

        composeTestRule.waitUntilDefaultTimeout { result != null }
        val bold = SpanStyle(fontWeight = FontWeight.Bold)
        assertEquals(
            NotificationContent(
                "Green Line D",
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
