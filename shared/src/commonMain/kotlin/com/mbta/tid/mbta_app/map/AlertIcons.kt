package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.map.style.Exp
import com.mbta.tid.mbta_app.map.style.ResolvedImage
import com.mbta.tid.mbta_app.map.style.downcastToResolvedImage
import com.mbta.tid.mbta_app.model.MapStopRoute
import com.mbta.tid.mbta_app.model.StopAlertState

public object AlertIcons {
    internal val alertIconPrefix = "alert-"
    internal val alertZoomClosePrefix = "large-"
    internal val alertZoomWidePrefix = "small-"

    public val all: List<String> =
        listOf(alertZoomClosePrefix, alertZoomWidePrefix).flatMap { zoomPrefix ->
            MapStopRoute.entries.flatMap { routeType ->
                StopAlertState.entries
                    .filter { state ->
                        state !in
                            setOf(
                                StopAlertState.Elevator,
                                StopAlertState.Normal,
                                StopAlertState.AllClear,
                            )
                    }
                    .map { state ->
                        "${alertIconPrefix}${zoomPrefix}${(routeType.name.lowercase())}-${(state.name.lowercase())}"
                    }
            }
        }

    // Expression that's true if the specified route index has no service status set
    private fun alertEmpty(index: Int): Exp<Boolean> {
        return Exp.not(
            Exp.has(MapExp.routeAt(index), Exp.get(StopFeaturesBuilder.propServiceStatusKey))
        )
    }

    // Expression that returns the alert status string for the given route index
    private fun alertStatus(index: Int): Exp<String> {
        return Exp.get(MapExp.routeAt(index), Exp.get(StopFeaturesBuilder.propServiceStatusKey))
    }

    private fun getAlertIconName(zoomPrefix: String, index: Int, forBus: Boolean): Exp<String> {
        return MapExp.busSwitchExp(
            forBus,
            Exp.case(
                Exp.any(
                    // Check if the index is greater than the number of routes at this stop
                    Exp.ge(Exp(index), Exp.length(MapExp.routesExp)),
                    // Or if the alert status at this index is empty
                    alertEmpty(index),
                    // Or if it's normal
                    Exp.eq(alertStatus(index), Exp(StopAlertState.Normal.name)),
                ) to
                    // If any of the above are true, don't display an alert icon
                    Exp(""),
                // Otherwise, use the non-normal alert status and route to get its icon name
                Exp.concat(
                    Exp(alertIconPrefix),
                    Exp(zoomPrefix),
                    Exp.downcase(MapExp.routeAt(index)),
                    Exp("-"),
                    Exp.downcase(alertStatus(index)),
                ),
            ),
        )
    }

    internal fun getAlertLayerIcon(index: Int, forBus: Boolean = false): Exp<ResolvedImage> {
        return Exp.step(
                Exp.zoom(),
                getAlertIconName(alertZoomWidePrefix, index, forBus),
                Exp(MapDefaults.closeZoomThreshold) to
                    getAlertIconName(alertZoomClosePrefix, index, forBus),
            )
            .downcastToResolvedImage()
    }
}
