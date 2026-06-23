package com.mbta.tid.mbta_app.model

import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.units.Length

public data class StopCardData(val stop: Stop, val data: List<RouteCardData.Leaf>) {
    val elevatorAlerts: List<Alert>
        get() =
            data
                .flatMap { it.alertsHere() }
                .filter { alert -> alert.effect == Alert.Effect.ElevatorClosure }
                .distinct()

    val routeStopDirections: Set<RouteStopDirection> = data.map { it.routeStopDirection }.toSet()

    internal fun distanceFrom(position: Position): Length = this.stop.distanceFrom(position)

    public companion object {
        public fun fromRouteCardData(
            routeCardData: List<RouteCardData>,
            sortByDistanceFrom: Position?,
        ): List<StopCardData> {
            val stopCardData =
                routeCardData
                    .flatMap { it.stopData }
                    .groupBy { it.stop.id }
                    .values
                    .map { routeStops ->
                        val stop = routeStops.first().stop
                        val data = routeStops.flatMap { it.data }
                        StopCardData(stop, data.sort())
                    }
            return if (sortByDistanceFrom != null)
                stopCardData.sortedBy { it.distanceFrom(sortByDistanceFrom) }
            else stopCardData
        }
    }
}
