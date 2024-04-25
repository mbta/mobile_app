package com.mbta.tid.mbta_app.android.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.route
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteType
import org.junit.Rule
import org.junit.Test

class RoutePillTest {
    @get:Rule val composeTestRule = createComposeRule()

    private fun init(route: Route) {
        composeTestRule.setContent { MyApplicationTheme { RoutePill(route) } }
    }

    @Test
    fun showsLightRailLongName() {
        init(
            route {
                color = "00843D"
                shortName = "D"
                longName = "Green Line D"
                textColor = "FFFFFF"
                type = RouteType.LIGHT_RAIL
            }
        )

        composeTestRule.onNodeWithText("Green Line D", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun showsBusShortName() {
        init(
            route {
                color = "FFC72C"
                shortName = "57"
                longName = "Watertown Yard - Kenmore Station"
                textColor = "000000"
                type = RouteType.BUS
            }
        )

        composeTestRule.onNodeWithText("57").assertIsDisplayed()
    }
}
