package com.mbta.tid.mbta_app.android.util

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class RouteModeLabelTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNameAndType() {
        val name = "1"
        val type = RouteType.BUS

        composeTestRule.setContent { Text(routeModeLabel(LocalContext.current, name, type)) }

        composeTestRule.onNodeWithText("1 bus").assertIsDisplayed()
    }

    @Test
    fun testPlural() {
        val name = "Red Line"
        val type = RouteType.HEAVY_RAIL

        composeTestRule.setContent { Text(routeModeLabel(LocalContext.current, name, type, false)) }

        composeTestRule.onNodeWithText("Red Line trains").assertIsDisplayed()
    }

    @Test
    fun testNameOnly() {
        val name = "Blue Line"

        composeTestRule.setContent { Text(routeModeLabel(LocalContext.current, name, null)) }

        composeTestRule.onNodeWithText(name).assertIsDisplayed()
    }

    @Test
    fun testTypeOnly() {
        val type = RouteType.FERRY

        composeTestRule.setContent { Text(routeModeLabel(LocalContext.current, null, type)) }

        composeTestRule.onNodeWithText("ferry").assertIsDisplayed()
    }

    @Test
    fun testEmpty() {
        val type = RouteType.FERRY

        composeTestRule.setContent {
            Text(
                routeModeLabel(LocalContext.current, name = null, type = null),
                Modifier.testTag("empty"),
            )
        }

        assertEquals(
            "",
            composeTestRule
                .onNodeWithTag("empty")
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.Text)
                ?.joinToString { it.text },
        )
    }

    @Test
    fun testRoute() {
        val route =
            ObjectCollectionBuilder.Single.route {
                longName = "Lynn Ferry"
                type = RouteType.FERRY
            }

        composeTestRule.setContent { Text(routeModeLabel(LocalContext.current, route)) }

        composeTestRule.onNodeWithText("Lynn Ferry").assertIsDisplayed()
    }

    @Test
    fun testLineAndRoute() {
        val line = ObjectCollectionBuilder.Single.line { longName = "Green Line" }
        val route =
            ObjectCollectionBuilder.Single.route {
                longName = "Green Line C"
                type = RouteType.LIGHT_RAIL
                lineId = line.id
            }

        composeTestRule.setContent { Text(routeModeLabel(LocalContext.current, line, route)) }

        composeTestRule.onNodeWithText("Green Line train").assertIsDisplayed()
    }

    @Test
    fun testLineOrRoute() {

        val line = ObjectCollectionBuilder.Single.line { longName = "Silver Line" }
        val route =
            ObjectCollectionBuilder.Single.route {
                shortName = "SL2"
                type = RouteType.BUS
                lineId = line.id
            }
        val lineOrRoute = RouteCardData.LineOrRoute.Line(line, setOf(route))

        composeTestRule.setContent { Text(routeModeLabel(LocalContext.current, lineOrRoute)) }

        composeTestRule.onNodeWithText("Silver Line bus").assertIsDisplayed()
    }
}
