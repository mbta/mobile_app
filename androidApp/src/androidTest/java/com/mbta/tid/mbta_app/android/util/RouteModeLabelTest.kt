package com.mbta.tid.mbta_app.android.util

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.testUtils.assertCanBeDisplayed
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
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

        composeTestRule.setContent { Text(routeModeLabel(LocalResources.current, name, type)) }

        composeTestRule.onNodeWithText("1 bus").assertCanBeDisplayed()
    }

    @Test
    fun testPlural() {
        val name = "Red Line"
        val type = RouteType.HEAVY_RAIL

        composeTestRule.setContent {
            Text(routeModeLabel(LocalResources.current, name, type, false))
        }

        composeTestRule.onNodeWithText("Red Line trains").assertCanBeDisplayed()
    }

    @Test
    fun testNameOnly() {
        val name = "Blue Line"

        composeTestRule.setContent { Text(routeModeLabel(LocalResources.current, name, null)) }

        composeTestRule.onNodeWithText(name).assertCanBeDisplayed()
    }

    @Test
    fun testTypeOnly() {
        val type = RouteType.FERRY

        composeTestRule.setContent { Text(routeModeLabel(LocalResources.current, null, type)) }

        composeTestRule.onNodeWithText("ferry").assertCanBeDisplayed()
    }

    @Test
    fun testEmpty() {
        val type = RouteType.FERRY

        composeTestRule.setContent {
            Text(
                routeModeLabel(LocalResources.current, name = null, type = null),
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

        composeTestRule.setContent { Text(routeModeLabel(LocalResources.current, route)) }

        composeTestRule.onNodeWithText("Lynn Ferry").assertCanBeDisplayed()
    }

    @Test
    fun testLineAndRoute() {
        val line = ObjectCollectionBuilder.Single.line { longName = "Green Line" }
        val route =
            ObjectCollectionBuilder.Single.route {
                longName = "Green Line C"
                type = RouteType.LIGHT_RAIL
                lineId = line.id.idText
            }

        composeTestRule.setContent { Text(routeModeLabel(LocalResources.current, line, route)) }

        composeTestRule.onNodeWithText("Green Line train").assertCanBeDisplayed()
    }

    @Test
    fun testLineOrRoute() {

        val line = ObjectCollectionBuilder.Single.line { longName = "Silver Line" }
        val route =
            ObjectCollectionBuilder.Single.route {
                shortName = "SL2"
                type = RouteType.BUS
                lineId = line.id.idText
            }
        val lineOrRoute = LineOrRoute.Line(line, setOf(route))

        composeTestRule.setContent { Text(routeModeLabel(LocalResources.current, lineOrRoute)) }

        composeTestRule.onNodeWithText("Silver Line bus").assertCanBeDisplayed()
    }
}
