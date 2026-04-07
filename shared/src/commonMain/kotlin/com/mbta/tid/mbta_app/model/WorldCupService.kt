package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

public object WorldCupService {
    private val objects = ObjectCollectionBuilder("WorldCupService")
    public val route: Route =
        objects.route {
            id = "⚽️WorldCup"
            type = RouteType.COMMUTER_RAIL
            color = "80276C"
            directionNames = listOf("Outbound", "Inbound")
            directionDestinations = listOf("Boston Stadium", "South Station")
            isListedRoute = false
            longName = "Boston Stadium Trains"
            shortName = ""
            sortOrder = 19999
            textColor = "FFFFFF"
        }
    public val routePatternOutbound: RoutePattern =
        objects.routePattern(route) {
            directionId = 0
            name = "South Station - Boston Stadium"
            sortOrder = 199990000
            typicality = RoutePattern.Typicality.Typical
            representativeTrip {
                headsign = "Boston Stadium"
                stopIds = listOf("place-sstat", "place-FS-0049")
            }
        }
    public val routePatternInbound: RoutePattern =
        objects.routePattern(route) {
            directionId = 1
            name = "Boston Stadium - South Station"
            sortOrder = 199990001
            typicality = RoutePattern.Typicality.Typical
            representativeTrip {
                headsign = "South Station"
                stopIds = listOf("place-FS-0049", "place-sstat")
            }
        }
    internal val globalData = GlobalResponse(objects)

    private val matchDays =
        listOf(
            LocalDate(2026, Month.JUNE, 13),
            LocalDate(2026, Month.JUNE, 16),
            LocalDate(2026, Month.JUNE, 19),
            LocalDate(2026, Month.JUNE, 23),
            LocalDate(2026, Month.JUNE, 26),
            LocalDate(2026, Month.JUNE, 29),
            LocalDate(2026, Month.JULY, 9),
        )

    public fun isMatchDay(day: LocalDate): Boolean = day in matchDays

    public val scheduleUrl: String = "https://mbta.com/bostonstadiumtrains"
}
