package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class StopDetailsFilteredSheetHeaderTest {
    val builder = ObjectCollectionBuilder()
    val now = EasternTimeInstant.now()
    val route =
        builder.route {
            id = "route_1"
            type = RouteType.BUS
            color = "FF0000"
            directionNames = listOf("North", "South")
            directionDestinations = listOf("Downtown", "Uptown")
            longName = "Sample Route Long Name"
            shortName = "111"
            textColor = "000000"
            lineId = "line_1"
            routePatternIds = mutableListOf("pattern_1", "pattern_2")
        }
    val stop =
        builder.stop {
            id = "stop_1"
            name = "Sample Stop"
            locationType = LocationType.STOP
            latitude = 0.0
            longitude = 0.0
        }

    @get:Rule val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testStopDetailsFilteredHeaderContent() {
        composeTestRule.setContent {
            StopDetailsFilteredHeader(
                route = route,
                line = null,
                stop = stop,
                onPin = {},
                onClose = {},
            )
        }

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(hasText("at Sample Stop"))

        composeTestRule.onNode(hasText(route.shortName)).assertExists()
        composeTestRule.onNodeWithContentDescription("Star route").assertExists()
        composeTestRule.onNodeWithContentDescription("Close").assertExists()
        composeTestRule.onNode(hasText("at Sample Stop") and isHeading()).assertIsDisplayed()
    }

    @Test
    fun testStopDetailsFilteredHeaderCallbacks() {
        var closeCalled: Boolean = false
        var pinCalled: Boolean = false

        composeTestRule.setContent {
            StopDetailsFilteredHeader(
                route = route,
                line = null,
                stop = stop,
                pinned = false,
                onPin = { pinCalled = true },
                onClose = { closeCalled = true },
            )
        }

        composeTestRule.onNodeWithContentDescription("Close").assertExists().performClick()
        assertTrue(closeCalled)
        composeTestRule.onNodeWithContentDescription("Star route").assertExists().performClick()
        assertTrue(pinCalled)
    }
}
