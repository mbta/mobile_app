package com.mbta.tid.mbta_app.android.map

import com.mbta.tid.mbta_app.android.generated.drawableByName
import com.mbta.tid.mbta_app.map.AlertIcons
import com.mbta.tid.mbta_app.map.StopIcons
import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder.Single.alert
import com.mbta.tid.mbta_app.model.RealtimePatterns
import org.junit.Assert.assertNotNull
import org.junit.Test

class DrawableByNameTest {
    @Test
    fun `all stop icons are defined`() {
        for (icon in StopIcons.all) {
            assertNotNull(drawableByName(icon))
        }
    }

    @Test
    fun `all alert icons are defined`() {
        for (icon in AlertIcons.all) {
            assertNotNull(drawableByName(icon))
        }
    }

    @Test
    fun `all secondary alert icon names are defined`() {
        for (alertEffect in Alert.Effect.entries - Alert.Effect.ElevatorClosure) {
            val alert = alert { effect = alertEffect }
            for (route in MapStopRoute.entries + null) {
                val icon = RealtimePatterns.Format.SecondaryAlert(alert, route)
                assertNotNull(drawableByName(icon.iconName))
            }
        }
    }
}
