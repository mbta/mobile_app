package com.mbta.tid.mbta_app.utils

import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.WheelchairBoardingStatus

/**
 * A snapshot of some real system data, to be referenced in tests and previews that need real data.
 * Owned by the jvmRun project utils.
 *
 * Do not manually edit this file to make changes; if something in here doesn’t compile and it’s
 * blocking the ProjectUtils themselves from running to recreate it, delete all the
 * [ObjectCollectionBuilder.put] calls to unblock ProjectUtils.
 *
 * @see com.mbta.tid.mbta_app.ProjectUtils
 */
public val TestData: ObjectCollectionBuilder by lazy {
    val objects = ObjectCollectionBuilder("TestData")
    putLines(objects)
    putRoutes(objects)
    putRoutePatterns(objects)
    putStops(objects)
    putTrips(objects)
    objects
}

private fun putLines(objects: ObjectCollectionBuilder) {
    objects.put(
        Line(
            id = Line.Id("line-Green"),
            color = "00843D",
            longName = "Green Line",
            shortName = "",
            sortOrder = 10_032,
            textColor = "FFFFFF",
        )
    )
    objects.put(
        Line(
            id = Line.Id("line-SLWaterfront"),
            color = "7C878E",
            longName = "Silver Line SL1/SL2/SL3",
            shortName = "",
            sortOrder = 10_051,
            textColor = "FFFFFF",
        )
    )
}

private fun putRoutes(objects: ObjectCollectionBuilder) {
    objects.put(
        Route(
            id = Route.Id("Red"),
            type = RouteType.HEAVY_RAIL,
            color = "DA291C",
            directionNames = listOf("South", "North"),
            directionDestinations = listOf("Ashmont/Braintree", "Alewife"),
            isListedRoute = true,
            longName = "Red Line",
            shortName = "",
            sortOrder = 10_010,
            textColor = "FFFFFF",
            lineId = Line.Id("line-Red"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("Orange"),
            type = RouteType.HEAVY_RAIL,
            color = "ED8B00",
            directionNames = listOf("South", "North"),
            directionDestinations = listOf("Forest Hills", "Oak Grove"),
            isListedRoute = true,
            longName = "Orange Line",
            shortName = "",
            sortOrder = 10_020,
            textColor = "FFFFFF",
            lineId = Line.Id("line-Orange"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("Green-B"),
            type = RouteType.LIGHT_RAIL,
            color = "00843D",
            directionNames = listOf("West", "East"),
            directionDestinations = listOf("Boston College", "Government Center"),
            isListedRoute = true,
            longName = "Green Line B",
            shortName = "B",
            sortOrder = 10_032,
            textColor = "FFFFFF",
            lineId = Line.Id("line-Green"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("Green-C"),
            type = RouteType.LIGHT_RAIL,
            color = "00843D",
            directionNames = listOf("West", "East"),
            directionDestinations = listOf("Cleveland Circle", "Government Center"),
            isListedRoute = true,
            longName = "Green Line C",
            shortName = "C",
            sortOrder = 10_033,
            textColor = "FFFFFF",
            lineId = Line.Id("line-Green"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("Green-D"),
            type = RouteType.LIGHT_RAIL,
            color = "00843D",
            directionNames = listOf("West", "East"),
            directionDestinations = listOf("Riverside", "Union Square"),
            isListedRoute = true,
            longName = "Green Line D",
            shortName = "D",
            sortOrder = 10_034,
            textColor = "FFFFFF",
            lineId = Line.Id("line-Green"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("Green-E"),
            type = RouteType.LIGHT_RAIL,
            color = "00843D",
            directionNames = listOf("West", "East"),
            directionDestinations = listOf("Heath Street", "Medford/Tufts"),
            isListedRoute = true,
            longName = "Green Line E",
            shortName = "E",
            sortOrder = 10_035,
            textColor = "FFFFFF",
            lineId = Line.Id("line-Green"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("741"),
            type = RouteType.BUS,
            color = "7C878E",
            directionNames = listOf("Outbound", "Inbound"),
            directionDestinations = listOf("Logan Airport Terminals", "South Station"),
            isListedRoute = true,
            longName = "Logan Airport Terminals - South Station",
            shortName = "SL1",
            sortOrder = 10_051,
            textColor = "FFFFFF",
            lineId = Line.Id("line-SLWaterfront"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("742"),
            type = RouteType.BUS,
            color = "7C878E",
            directionNames = listOf("Outbound", "Inbound"),
            directionDestinations = listOf("Design Center", "South Station"),
            isListedRoute = true,
            longName = "Design Center - South Station",
            shortName = "SL2",
            sortOrder = 10_052,
            textColor = "FFFFFF",
            lineId = Line.Id("line-SLWaterfront"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("743"),
            type = RouteType.BUS,
            color = "7C878E",
            directionNames = listOf("Outbound", "Inbound"),
            directionDestinations = listOf("Chelsea Station", "South Station"),
            isListedRoute = true,
            longName = "Chelsea Station - South Station",
            shortName = "SL3",
            sortOrder = 10_053,
            textColor = "FFFFFF",
            lineId = Line.Id("line-SLWaterfront"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("746"),
            type = RouteType.BUS,
            color = "7C878E",
            directionNames = listOf("Outbound", "Inbound"),
            directionDestinations = listOf("Silver Line Way", "South Station"),
            isListedRoute = true,
            longName = "Silver Line Way - South Station",
            shortName = "SLW",
            sortOrder = 10_057,
            textColor = "FFFFFF",
            lineId = Line.Id("line-SLWaterfront"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("CR-Fitchburg"),
            type = RouteType.COMMUTER_RAIL,
            color = "80276C",
            directionNames = listOf("Outbound", "Inbound"),
            directionDestinations = listOf("Wachusett", "North Station"),
            isListedRoute = true,
            longName = "Fitchburg Line",
            shortName = "",
            sortOrder = 20_003,
            textColor = "FFFFFF",
            lineId = Line.Id("line-Fitchburg"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("CR-Haverhill"),
            type = RouteType.COMMUTER_RAIL,
            color = "80276C",
            directionNames = listOf("Outbound", "Inbound"),
            directionDestinations = listOf("Haverhill", "North Station"),
            isListedRoute = true,
            longName = "Haverhill Line",
            shortName = "",
            sortOrder = 20_007,
            textColor = "FFFFFF",
            lineId = Line.Id("line-Haverhill"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("CR-Lowell"),
            type = RouteType.COMMUTER_RAIL,
            color = "80276C",
            directionNames = listOf("Outbound", "Inbound"),
            directionDestinations = listOf("Lowell", "North Station"),
            isListedRoute = true,
            longName = "Lowell Line",
            shortName = "",
            sortOrder = 20_009,
            textColor = "FFFFFF",
            lineId = Line.Id("line-Lowell"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("CR-Newburyport"),
            type = RouteType.COMMUTER_RAIL,
            color = "80276C",
            directionNames = listOf("Outbound", "Inbound"),
            directionDestinations = listOf("Newburyport or Rockport", "North Station"),
            isListedRoute = true,
            longName = "Newburyport/Rockport Line",
            shortName = "",
            sortOrder = 20_012,
            textColor = "FFFFFF",
            lineId = Line.Id("line-Newburyport"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("CR-Providence"),
            type = RouteType.COMMUTER_RAIL,
            color = "80276C",
            directionNames = listOf("Outbound", "Inbound"),
            directionDestinations = listOf("Stoughton or Wickford Junction", "South Station"),
            isListedRoute = true,
            longName = "Providence/Stoughton Line",
            shortName = "",
            sortOrder = 20_013,
            textColor = "FFFFFF",
            lineId = Line.Id("line-Providence"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("15"),
            type = RouteType.BUS,
            color = "FFC72C",
            directionNames = listOf("Outbound", "Inbound"),
            directionDestinations = listOf("Fields Corner Station", "Ruggles Station"),
            isListedRoute = true,
            longName = "Fields Corner Station - Ruggles Station",
            shortName = "15",
            sortOrder = 50_150,
            textColor = "000000",
            lineId = Line.Id("line-15"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("67"),
            type = RouteType.BUS,
            color = "FFC72C",
            directionNames = listOf("Outbound", "Inbound"),
            directionDestinations = listOf("Turkey Hill", "Alewife Station"),
            isListedRoute = true,
            longName = "Turkey Hill - Alewife Station",
            shortName = "67",
            sortOrder = 50_670,
            textColor = "000000",
            lineId = Line.Id("line-6779"),
            routePatternIds = null,
        )
    )
    objects.put(
        Route(
            id = Route.Id("87"),
            type = RouteType.BUS,
            color = "FFC72C",
            directionNames = listOf("Outbound", "Inbound"),
            directionDestinations =
                listOf("Clarendon Hill or Arlington Center", "Lechmere Station"),
            isListedRoute = true,
            longName = "Clarendon Hill or Arlington Center - Lechmere Station",
            shortName = "87",
            sortOrder = 50_870,
            textColor = "000000",
            lineId = Line.Id("line-87"),
            routePatternIds = null,
        )
    )
}

private fun putRoutePatterns(objects: ObjectCollectionBuilder) {
    objects.put(
        RoutePattern(
            id = "Red-3-0",
            directionId = 0,
            name = "Alewife - Braintree",
            sortOrder = 100_100_040,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-Red-C1-0",
            routeId = Route.Id("Red"),
        )
    )
    objects.put(
        RoutePattern(
            id = "Red-1-0",
            directionId = 0,
            name = "Alewife - Ashmont",
            sortOrder = 100_100_041,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-Red-C2-0",
            routeId = Route.Id("Red"),
        )
    )
    objects.put(
        RoutePattern(
            id = "Red-3-1",
            directionId = 1,
            name = "Braintree - Alewife",
            sortOrder = 100_101_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-Red-C1-1",
            routeId = Route.Id("Red"),
        )
    )
    objects.put(
        RoutePattern(
            id = "Red-1-1",
            directionId = 1,
            name = "Ashmont - Alewife",
            sortOrder = 100_101_001,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-Red-C2-1",
            routeId = Route.Id("Red"),
        )
    )
    objects.put(
        RoutePattern(
            id = "Orange-3-0",
            directionId = 0,
            name = "Oak Grove - Forest Hills",
            sortOrder = 100_200_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-Orange-C1-0",
            routeId = Route.Id("Orange"),
        )
    )
    objects.put(
        RoutePattern(
            id = "Orange-3-1",
            directionId = 1,
            name = "Forest Hills - Oak Grove",
            sortOrder = 100_201_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-Orange-C1-1",
            routeId = Route.Id("Orange"),
        )
    )
    objects.put(
        RoutePattern(
            id = "Green-B-812-0",
            directionId = 0,
            name = "Government Center - Boston College",
            sortOrder = 100_320_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-Green-B-C1-0",
            routeId = Route.Id("Green-B"),
        )
    )
    objects.put(
        RoutePattern(
            id = "Green-B-812-1",
            directionId = 1,
            name = "Boston College - Government Center",
            sortOrder = 100_321_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-Green-B-C1-1",
            routeId = Route.Id("Green-B"),
        )
    )
    objects.put(
        RoutePattern(
            id = "Green-C-832-0",
            directionId = 0,
            name = "Government Center - Cleveland Circle",
            sortOrder = 100_330_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-Green-C-C1-0",
            routeId = Route.Id("Green-C"),
        )
    )
    objects.put(
        RoutePattern(
            id = "Green-C-832-1",
            directionId = 1,
            name = "Cleveland Circle - Government Center",
            sortOrder = 100_331_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-Green-C-C1-1",
            routeId = Route.Id("Green-C"),
        )
    )
    objects.put(
        RoutePattern(
            id = "Green-D-855-0",
            directionId = 0,
            name = "Union Square - Riverside",
            sortOrder = 100_340_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-Green-D-C1-0",
            routeId = Route.Id("Green-D"),
        )
    )
    objects.put(
        RoutePattern(
            id = "Green-D-855-1",
            directionId = 1,
            name = "Riverside - Union Square",
            sortOrder = 100_341_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-Green-D-C1-1",
            routeId = Route.Id("Green-D"),
        )
    )
    objects.put(
        RoutePattern(
            id = "Green-E-886-0",
            directionId = 0,
            name = "Medford/Tufts - Heath Street",
            sortOrder = 100_350_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-Green-E-C1-0",
            routeId = Route.Id("Green-E"),
        )
    )
    objects.put(
        RoutePattern(
            id = "Green-E-886-1",
            directionId = 1,
            name = "Heath Street - Medford/Tufts",
            sortOrder = 100_351_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-Green-E-C1-1",
            routeId = Route.Id("Green-E"),
        )
    )
    objects.put(
        RoutePattern(
            id = "741-_-0",
            directionId = 0,
            name = "South Station - Logan Airport Terminals",
            sortOrder = 100_510_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "72039742",
            routeId = Route.Id("741"),
        )
    )
    objects.put(
        RoutePattern(
            id = "741-_-1",
            directionId = 1,
            name = "Logan Airport Terminals - South Station",
            sortOrder = 100_511_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "72039744",
            routeId = Route.Id("741"),
        )
    )
    objects.put(
        RoutePattern(
            id = "742-3-0",
            directionId = 0,
            name = "South Station - Design Center (PM Route)",
            sortOrder = 100_520_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "72039786",
            routeId = Route.Id("742"),
        )
    )
    objects.put(
        RoutePattern(
            id = "742-3-1",
            directionId = 1,
            name = "Design Center - South Station (PM - Omits 88 Black Falcon Ave)",
            sortOrder = 100_521_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "72039768",
            routeId = Route.Id("742"),
        )
    )
    objects.put(
        RoutePattern(
            id = "743-_-0",
            directionId = 0,
            name = "South Station - Chelsea Station",
            sortOrder = 100_530_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "72040977",
            routeId = Route.Id("743"),
        )
    )
    objects.put(
        RoutePattern(
            id = "743-_-1",
            directionId = 1,
            name = "Chelsea Station - South Station",
            sortOrder = 100_531_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "72040978",
            routeId = Route.Id("743"),
        )
    )
    objects.put(
        RoutePattern(
            id = "746-_-0",
            directionId = 0,
            name = "South Station - Silver Line Way",
            sortOrder = 100_570_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "72039743",
            routeId = Route.Id("746"),
        )
    )
    objects.put(
        RoutePattern(
            id = "746-_-1",
            directionId = 1,
            name = "Silver Line Way - South Station",
            sortOrder = 100_571_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "72039750",
            routeId = Route.Id("746"),
        )
    )
    objects.put(
        RoutePattern(
            id = "CR-Fitchburg-2a5f6366-0",
            directionId = 0,
            name = "North Station - Wachusett",
            sortOrder = 200_030_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "HaverhillRestoredWKDY-744237-405",
            routeId = Route.Id("CR-Fitchburg"),
        )
    )
    objects.put(
        RoutePattern(
            id = "CR-Fitchburg-d82ea33a-1",
            directionId = 1,
            name = "Wachusett - North Station",
            sortOrder = 200_031_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "HaverhillRestoredWKDY-744235-400",
            routeId = Route.Id("CR-Fitchburg"),
        )
    )
    objects.put(
        RoutePattern(
            id = "CR-Haverhill-779dede9-0",
            directionId = 0,
            name = "North Station - Haverhill via Reading",
            sortOrder = 200_070_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-CR-Haverhill-C1-0",
            routeId = Route.Id("CR-Haverhill"),
        )
    )
    objects.put(
        RoutePattern(
            id = "CR-Haverhill-ebc58735-1",
            directionId = 1,
            name = "Haverhill - North Station via Reading",
            sortOrder = 200_071_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-CR-Haverhill-C1-1",
            routeId = Route.Id("CR-Haverhill"),
        )
    )
    objects.put(
        RoutePattern(
            id = "CR-Lowell-edb39c7b-0",
            directionId = 0,
            name = "North Station - Lowell",
            sortOrder = 200_090_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-CR-Lowell-C1-0",
            routeId = Route.Id("CR-Lowell"),
        )
    )
    objects.put(
        RoutePattern(
            id = "CR-Lowell-305fef81-1",
            directionId = 1,
            name = "Lowell - North Station",
            sortOrder = 200_091_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "canonical-CR-Lowell-C1-1",
            routeId = Route.Id("CR-Lowell"),
        )
    )
    objects.put(
        RoutePattern(
            id = "CR-Newburyport-79533330-0",
            directionId = 0,
            name = "North Station - Newburyport",
            sortOrder = 200_120_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "HaverhillRestoredWKDY-744341-125",
            routeId = Route.Id("CR-Newburyport"),
        )
    )
    objects.put(
        RoutePattern(
            id = "CR-Newburyport-e54dc640-0",
            directionId = 0,
            name = "North Station - Rockport",
            sortOrder = 200_120_010,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "HaverhillRestoredWKDY-744312-27",
            routeId = Route.Id("CR-Newburyport"),
        )
    )
    objects.put(
        RoutePattern(
            id = "CR-Newburyport-7e4857df-1",
            directionId = 1,
            name = "Newburyport - North Station",
            sortOrder = 200_121_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "HaverhillRestoredWKDY-744338-114",
            routeId = Route.Id("CR-Newburyport"),
        )
    )
    objects.put(
        RoutePattern(
            id = "CR-Newburyport-d47c5647-1",
            directionId = 1,
            name = "Rockport - North Station",
            sortOrder = 200_121_010,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "HaverhillRestoredWKDY-744311-24",
            routeId = Route.Id("CR-Newburyport"),
        )
    )
    objects.put(
        RoutePattern(
            id = "CR-Providence-9cf54fb3-0",
            directionId = 0,
            name = "South Station - Wickford Junction via Back Bay",
            sortOrder = 200_130_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "Sept8Read-767850-839",
            routeId = Route.Id("CR-Providence"),
        )
    )
    objects.put(
        RoutePattern(
            id = "CR-Providence-9515a09b-0",
            directionId = 0,
            name = "South Station - Stoughton via Back Bay",
            sortOrder = 200_130_010,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "Sept8Read-768056-925",
            routeId = Route.Id("CR-Providence"),
        )
    )
    objects.put(
        RoutePattern(
            id = "CR-Providence-e9395acc-1",
            directionId = 1,
            name = "Wickford Junction - South Station via Back Bay",
            sortOrder = 200_131_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "Sept8Read-767840-852",
            routeId = Route.Id("CR-Providence"),
        )
    )
    objects.put(
        RoutePattern(
            id = "CR-Providence-6cae46be-1",
            directionId = 1,
            name = "Stoughton - South Station via Back Bay",
            sortOrder = 200_131_010,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "Sept8Read-768057-930",
            routeId = Route.Id("CR-Providence"),
        )
    )
    objects.put(
        RoutePattern(
            id = "15-1-0",
            directionId = 0,
            name = "Ruggles Station - Fields Corner Station",
            sortOrder = 501_500_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "71373591",
            routeId = Route.Id("15"),
        )
    )
    objects.put(
        RoutePattern(
            id = "15-1-1",
            directionId = 1,
            name = "Fields Corner Station - Ruggles Station",
            sortOrder = 501_501_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "71373380",
            routeId = Route.Id("15"),
        )
    )
    objects.put(
        RoutePattern(
            id = "67-4-0",
            directionId = 0,
            name = "Alewife Station - Turkey Hill",
            sortOrder = 506_700_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "71694757",
            routeId = Route.Id("67"),
        )
    )
    objects.put(
        RoutePattern(
            id = "67-4-1",
            directionId = 1,
            name = "Turkey Hill - Alewife Station",
            sortOrder = 506_701_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "71694754",
            routeId = Route.Id("67"),
        )
    )
    objects.put(
        RoutePattern(
            id = "87-2-0",
            directionId = 0,
            name = "Lechmere Station - Arlington Center",
            sortOrder = 508_700_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "71694434",
            routeId = Route.Id("87"),
        )
    )
    objects.put(
        RoutePattern(
            id = "87-2-1",
            directionId = 1,
            name = "Arlington Center - Lechmere Station",
            sortOrder = 508_701_000,
            typicality = RoutePattern.Typicality.Typical,
            representativeTripId = "71694436",
            routeId = Route.Id("87"),
        )
    )
}

private fun putStops(objects: ObjectCollectionBuilder) {
    objects.put(
        Stop(
            id = "10642",
            latitude = 42.29985,
            longitude = -71.114261,
            name = "Forest Hills",
            locationType = LocationType.STOP,
            description = "Forest Hills - Upper Busway",
            platformCode = null,
            platformName = "Upper Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-forhl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "11531",
            latitude = 42.323074,
            longitude = -71.099546,
            name = "Jackson Square",
            locationType = LocationType.STOP,
            description = "Jackson Square - Busway",
            platformCode = null,
            platformName = "Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-jaksn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "121",
            latitude = 42.320511,
            longitude = -71.05178,
            name = "JFK/UMass",
            locationType = LocationType.STOP,
            description = "JFK/UMass - Busway",
            platformCode = null,
            platformName = "Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-jfk",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "13",
            latitude = 42.329962,
            longitude = -71.057625,
            name = "Andrew",
            locationType = LocationType.STOP,
            description = "Andrew - Busway",
            platformCode = null,
            platformName = "Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-andrw",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "14112",
            latitude = 42.395325,
            longitude = -71.141439,
            name = "Alewife",
            locationType = LocationType.STOP,
            description = "Alewife - Busway Berth A6",
            platformCode = "A6",
            platformName = "Busway Berth A6",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-alfcl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "14118",
            latitude = 42.396228,
            longitude = -71.141735,
            name = "Alewife",
            locationType = LocationType.STOP,
            description = "Alewife - Busway Berth B6",
            platformCode = "B6",
            platformName = "Busway Berth B6",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-alfcl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "14120",
            latitude = 42.3958,
            longitude = -71.141524,
            name = "Alewife",
            locationType = LocationType.STOP,
            description = "Alewife - Busway Berth B4",
            platformCode = "B4",
            platformName = "Busway Berth B4",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-alfcl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "14121",
            latitude = 42.395622,
            longitude = -71.141438,
            name = "Alewife",
            locationType = LocationType.STOP,
            description = "Alewife - Busway Berth B3",
            platformCode = "B3",
            platformName = "Busway Berth B3",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-alfcl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "14122",
            latitude = 42.395389,
            longitude = -71.141326,
            name = "Alewife",
            locationType = LocationType.STOP,
            description = "Alewife - Busway Berth B2",
            platformCode = "B2",
            platformName = "Busway Berth B2",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-alfcl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "14123",
            latitude = 42.395197,
            longitude = -71.141237,
            name = "Alewife",
            locationType = LocationType.STOP,
            description = "Alewife - Busway Berth B1",
            platformCode = "B1",
            platformName = "Busway Berth B1",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-alfcl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "1432",
            latitude = 42.364737,
            longitude = -71.178564,
            name = "Arsenal St @ Irving St",
            locationType = LocationType.STOP,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "14320",
            latitude = 42.253069,
            longitude = -71.017292,
            name = "Adams St @ Whitwell St",
            locationType = LocationType.STOP,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "170136",
            latitude = 42.351538,
            longitude = -71.119553,
            name = "Babcock Street",
            locationType = LocationType.STOP,
            description = "Babcock Street - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-babck",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "170137",
            latitude = 42.351695,
            longitude = -71.120257,
            name = "Babcock Street",
            locationType = LocationType.STOP,
            description = "Babcock Street - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-babck",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "170140",
            latitude = 42.350901,
            longitude = -71.114318,
            name = "Amory Street",
            locationType = LocationType.STOP,
            description = "Amory Street - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-amory",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "170141",
            latitude = 42.351066,
            longitude = -71.115027,
            name = "Amory Street",
            locationType = LocationType.STOP,
            description = "Amory Street - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-amory",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "17861",
            latitude = 42.336572,
            longitude = -71.089616,
            name = "Ruggles",
            locationType = LocationType.STOP,
            description = "Ruggles - Upper Busway",
            platformCode = null,
            platformName = "Upper Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rugg",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "17862",
            latitude = 42.336489,
            longitude = -71.089085,
            name = "Ruggles",
            locationType = LocationType.STOP,
            description = "Ruggles - Lower Busway Lane 1",
            platformCode = "1",
            platformName = "Lower Busway Lane 1",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rugg",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "17863",
            latitude = 42.33639,
            longitude = -71.08905,
            name = "Ruggles",
            locationType = LocationType.STOP,
            description = "Ruggles - Lower Busway Lane 2",
            platformCode = "2",
            platformName = "Lower Busway Lane 2",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rugg",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "21917",
            latitude = 42.335069,
            longitude = -71.14933,
            name = "Reservoir",
            locationType = LocationType.STOP,
            description = "Reservoir - Busway Berth 1",
            platformCode = "1",
            platformName = "Busway Berth 1",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rsmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "21918",
            latitude = 42.335032,
            longitude = -71.148928,
            name = "Reservoir",
            locationType = LocationType.STOP,
            description = "Reservoir - Busway Berth 2",
            platformCode = "2",
            platformName = "Busway Berth 2",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rsmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "23391",
            latitude = 42.347441,
            longitude = -71.074592,
            name = "Back Bay",
            locationType = LocationType.STOP,
            description = "Back Bay - Busway",
            platformCode = null,
            platformName = "Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bbsta",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "2595",
            latitude = 42.381176,
            longitude = -71.099677,
            name = "Somerville Ave @ Carlton St",
            locationType = LocationType.STOP,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "26131",
            latitude = 42.380273,
            longitude = -71.096622,
            name = "Bow St @ Warren Ave",
            locationType = LocationType.STOP,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "29001",
            latitude = 42.384598,
            longitude = -71.076472,
            name = "Sullivan Square",
            locationType = LocationType.STOP,
            description = "Sullivan Square - Upper Busway Berth A6",
            platformCode = "A6",
            platformName = "Upper Busway Berth A6",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sull",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "29002",
            latitude = 42.384386,
            longitude = -71.076502,
            name = "Sullivan Square",
            locationType = LocationType.STOP,
            description = "Sullivan Square - Upper Busway Berth A5",
            platformCode = "A5",
            platformName = "Upper Busway Berth A5",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sull",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "29003",
            latitude = 42.384266,
            longitude = -71.076519,
            name = "Sullivan Square",
            locationType = LocationType.STOP,
            description = "Sullivan Square - Upper Busway Berth A4",
            platformCode = "A4",
            platformName = "Upper Busway Berth A4",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sull",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "29004",
            latitude = 42.384143,
            longitude = -71.076527,
            name = "Sullivan Square",
            locationType = LocationType.STOP,
            description = "Sullivan Square - Upper Busway Berth A3",
            platformCode = "A3",
            platformName = "Upper Busway Berth A3",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sull",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "29005",
            latitude = 42.384032,
            longitude = -71.076534,
            name = "Sullivan Square",
            locationType = LocationType.STOP,
            description = "Sullivan Square - Upper Busway Berth A2",
            platformCode = "A2",
            platformName = "Upper Busway Berth A2",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sull",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "29006",
            latitude = 42.38391,
            longitude = -71.076532,
            name = "Sullivan Square",
            locationType = LocationType.STOP,
            description = "Sullivan Square - Upper Busway Berth A1",
            platformCode = "A1",
            platformName = "Upper Busway Berth A1",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sull",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "29007",
            latitude = 42.384346,
            longitude = -71.076339,
            name = "Sullivan Square",
            locationType = LocationType.STOP,
            description = "Sullivan Square - Lower Busway Berth B4",
            platformCode = "B4",
            platformName = "Lower Busway Berth B4",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sull",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "29008",
            latitude = 42.384062,
            longitude = -71.076342,
            name = "Sullivan Square",
            locationType = LocationType.STOP,
            description = "Sullivan Square - Lower Busway Berth B3",
            platformCode = "B3",
            platformName = "Lower Busway Berth B3",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sull",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "29009",
            latitude = 42.383661,
            longitude = -71.076353,
            name = "Sullivan Square",
            locationType = LocationType.STOP,
            description = "Sullivan Square - Lower Busway Berth B2",
            platformCode = "B2",
            platformName = "Lower Busway Berth B2",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sull",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "29010",
            latitude = 42.38392,
            longitude = -71.076318,
            name = "Sullivan Square",
            locationType = LocationType.STOP,
            description = "Sullivan Square - Lower Busway Berth B1",
            platformCode = "B1",
            platformName = "Lower Busway Berth B1",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sull",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "29011",
            latitude = 42.38436,
            longitude = -71.07619,
            name = "Sullivan Square",
            locationType = LocationType.STOP,
            description = "Sullivan Square - Lower Busway Berth C4",
            platformCode = "C4",
            platformName = "Lower Busway Berth C4",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sull",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "29012",
            latitude = 42.384059,
            longitude = -71.076192,
            name = "Sullivan Square",
            locationType = LocationType.STOP,
            description = "Sullivan Square - Lower Busway Berth C3",
            platformCode = "C3",
            platformName = "Lower Busway Berth C3",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sull",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "29013",
            latitude = 42.383825,
            longitude = -71.076194,
            name = "Sullivan Square",
            locationType = LocationType.STOP,
            description = "Sullivan Square - Lower Busway Berth C2",
            platformCode = "C2",
            platformName = "Lower Busway Berth C2",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sull",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "3125",
            latitude = 42.275585,
            longitude = -71.028752,
            name = "North Quincy",
            locationType = LocationType.STOP,
            description = "North Quincy - Busway",
            platformCode = null,
            platformName = "Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-nqncy",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "32001",
            latitude = 42.251696,
            longitude = -71.004973,
            name = "Quincy Center",
            locationType = LocationType.STOP,
            description = "Quincy Center - Berth 1",
            platformCode = null,
            platformName = "Berth 1",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-qnctr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "32002",
            latitude = 42.251772,
            longitude = -71.005099,
            name = "Quincy Center",
            locationType = LocationType.STOP,
            description = "Quincy Center - Berth 2",
            platformCode = null,
            platformName = "Berth 2",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-qnctr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "32003",
            latitude = 42.251847,
            longitude = -71.005168,
            name = "Quincy Center",
            locationType = LocationType.STOP,
            description = "Quincy Center - Berth 3",
            platformCode = null,
            platformName = "Berth 3",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-qnctr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "32004",
            latitude = 42.251908,
            longitude = -71.0052,
            name = "Quincy Center",
            locationType = LocationType.STOP,
            description = "Quincy Center - Berth 4",
            platformCode = null,
            platformName = "Berth 4",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-qnctr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "32005",
            latitude = 42.251975,
            longitude = -71.005231,
            name = "Quincy Center",
            locationType = LocationType.STOP,
            description = "Quincy Center - Berth 5",
            platformCode = null,
            platformName = "Berth 5",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-qnctr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "323",
            latitude = 42.299807,
            longitude = -71.062219,
            name = "Fields Corner",
            locationType = LocationType.STOP,
            description = "Fields Corner - Busway",
            platformCode = null,
            platformName = "Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-fldcr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "334",
            latitude = 42.284195,
            longitude = -71.063879,
            name = "Ashmont",
            locationType = LocationType.STOP,
            description = "Ashmont - Busway",
            platformCode = null,
            platformName = "Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-asmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "38155",
            latitude = 42.336666,
            longitude = -71.253304,
            name = "Riverside",
            locationType = LocationType.STOP,
            description = "Riverside - Busway",
            platformCode = null,
            platformName = "Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-river",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "38671",
            latitude = 42.207213,
            longitude = -71.001278,
            name = "Braintree",
            locationType = LocationType.STOP,
            description = "Braintree - Busway",
            platformCode = null,
            platformName = "Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-brntn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "41031",
            latitude = 42.233238,
            longitude = -71.007244,
            name = "Quincy Adams",
            locationType = LocationType.STOP,
            description = "Quincy Adams - Busway",
            platformCode = null,
            platformName = "Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-qamnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "5072",
            latitude = 42.426729,
            longitude = -71.074659,
            name = "Malden Center",
            locationType = LocationType.STOP,
            description = "Malden Center - West Busway",
            platformCode = null,
            platformName = "West Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-mlmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "5104",
            latitude = 42.396646,
            longitude = -71.121879,
            name = "Davis",
            locationType = LocationType.STOP,
            description = "Davis - Busway",
            platformCode = null,
            platformName = "Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-davis",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "52710",
            latitude = 42.402533,
            longitude = -71.075586,
            name = "Wellington",
            locationType = LocationType.STOP,
            description = "Wellington - Busway Berth A1",
            platformCode = "A1",
            platformName = "Busway Berth A1",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-welln",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "52711",
            latitude = 42.402469,
            longitude = -71.075826,
            name = "Wellington",
            locationType = LocationType.STOP,
            description = "Wellington - Busway Berth A2",
            platformCode = "A2",
            platformName = "Busway Berth A2",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-welln",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "52712",
            latitude = 42.402463,
            longitude = -71.07609,
            name = "Wellington",
            locationType = LocationType.STOP,
            description = "Wellington - Busway Berth A3",
            platformCode = "A3",
            platformName = "Busway Berth A3",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-welln",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "52713",
            latitude = 42.40246,
            longitude = -71.0763,
            name = "Wellington",
            locationType = LocationType.STOP,
            description = "Wellington - Busway Berth A4",
            platformCode = "A4",
            platformName = "Busway Berth A4",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-welln",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "52714",
            latitude = 42.402616,
            longitude = -71.075739,
            name = "Wellington",
            locationType = LocationType.STOP,
            description = "Wellington - Busway Berth B1",
            platformCode = "B1",
            platformName = "Busway Berth B1",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-welln",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "52715",
            latitude = 42.402597,
            longitude = -71.076024,
            name = "Wellington",
            locationType = LocationType.STOP,
            description = "Wellington - Busway Berth B2",
            platformCode = "B2",
            platformName = "Busway Berth B2",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-welln",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "52716",
            latitude = 42.402602,
            longitude = -71.076337,
            name = "Wellington",
            locationType = LocationType.STOP,
            description = "Wellington - Busway Berth B3",
            platformCode = "B3",
            platformName = "Busway Berth B3",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-welln",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "52720",
            latitude = 42.402517,
            longitude = -71.076577,
            name = "Wellington",
            locationType = LocationType.STOP,
            description = "Wellington - Busway Drop-off Only",
            platformCode = null,
            platformName = "Busway Drop-off Only",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-welln",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "5327",
            latitude = 42.426507,
            longitude = -71.073919,
            name = "Malden Center",
            locationType = LocationType.STOP,
            description = "Malden Center - East Busway Platform 2",
            platformCode = "2",
            platformName = "East Busway Platform 2",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-mlmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "53270",
            latitude = 42.426565,
            longitude = -71.074107,
            name = "Malden Center",
            locationType = LocationType.STOP,
            description = "Malden Center - East Busway Platform 1",
            platformCode = "1",
            platformName = "East Busway Platform 1",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-mlmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70001",
            latitude = 42.300926,
            longitude = -71.114129,
            name = "Forest Hills",
            locationType = LocationType.STOP,
            description = "Forest Hills - Orange Line",
            platformCode = null,
            platformName = "Orange Line",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-forhl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70002",
            latitude = 42.309832,
            longitude = -71.108059,
            name = "Green Street",
            locationType = LocationType.STOP,
            description = "Green Street - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-grnst",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70003",
            latitude = 42.309796,
            longitude = -71.107988,
            name = "Green Street",
            locationType = LocationType.STOP,
            description = "Green Street - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-grnst",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70004",
            latitude = 42.317062,
            longitude = -71.104248,
            name = "Stony Brook",
            locationType = LocationType.STOP,
            description = "Stony Brook - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sbmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70005",
            latitude = 42.317062,
            longitude = -71.104248,
            name = "Stony Brook",
            locationType = LocationType.STOP,
            description = "Stony Brook - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sbmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70006",
            latitude = 42.323132,
            longitude = -71.099592,
            name = "Jackson Square",
            locationType = LocationType.STOP,
            description = "Jackson Square - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-jaksn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70007",
            latitude = 42.323132,
            longitude = -71.099592,
            name = "Jackson Square",
            locationType = LocationType.STOP,
            description = "Jackson Square - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-jaksn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70008",
            latitude = 42.330671,
            longitude = -71.096121,
            name = "Roxbury Crossing",
            locationType = LocationType.STOP,
            description = "Roxbury Crossing - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rcmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70009",
            latitude = 42.33064,
            longitude = -71.09604,
            name = "Roxbury Crossing",
            locationType = LocationType.STOP,
            description = "Roxbury Crossing - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rcmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70010",
            latitude = 42.33649,
            longitude = -71.089684,
            name = "Ruggles",
            locationType = LocationType.STOP,
            description = "Ruggles - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rugg",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70011",
            latitude = 42.336451,
            longitude = -71.089635,
            name = "Ruggles",
            locationType = LocationType.STOP,
            description = "Ruggles - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rugg",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70012",
            latitude = 42.340927,
            longitude = -71.084188,
            name = "Massachusetts Avenue",
            locationType = LocationType.STOP,
            description = "Massachusetts Avenue - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-masta",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70013",
            latitude = 42.340892,
            longitude = -71.084124,
            name = "Massachusetts Avenue",
            locationType = LocationType.STOP,
            description = "Massachusetts Avenue - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-masta",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70014",
            latitude = 42.34735,
            longitude = -71.075727,
            name = "Back Bay",
            locationType = LocationType.STOP,
            description = "Back Bay - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bbsta",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70015",
            latitude = 42.34735,
            longitude = -71.075727,
            name = "Back Bay",
            locationType = LocationType.STOP,
            description = "Back Bay - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bbsta",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70016",
            latitude = 42.349662,
            longitude = -71.063917,
            name = "Tufts Medical Center",
            locationType = LocationType.STOP,
            description = "Tufts Medical Center - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-tumnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70017",
            latitude = 42.349662,
            longitude = -71.063917,
            name = "Tufts Medical Center",
            locationType = LocationType.STOP,
            description = "Tufts Medical Center - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-tumnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70018",
            latitude = 42.352547,
            longitude = -71.062752,
            name = "Chinatown",
            locationType = LocationType.STOP,
            description = "Chinatown - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-chncl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70019",
            latitude = 42.352547,
            longitude = -71.062752,
            name = "Chinatown",
            locationType = LocationType.STOP,
            description = "Chinatown - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-chncl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70020",
            latitude = 42.355518,
            longitude = -71.060225,
            name = "Downtown Crossing",
            locationType = LocationType.STOP,
            description = "Downtown Crossing - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-dwnxg",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70021",
            latitude = 42.355518,
            longitude = -71.060225,
            name = "Downtown Crossing",
            locationType = LocationType.STOP,
            description = "Downtown Crossing - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-dwnxg",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70022",
            latitude = 42.358978,
            longitude = -71.057598,
            name = "State",
            locationType = LocationType.STOP,
            description = "State - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-state",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70023",
            latitude = 42.358978,
            longitude = -71.057598,
            name = "State",
            locationType = LocationType.STOP,
            description = "State - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-state",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70024",
            latitude = 42.363021,
            longitude = -71.05829,
            name = "Haymarket",
            locationType = LocationType.STOP,
            description = "Haymarket - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-haecl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70025",
            latitude = 42.363021,
            longitude = -71.05829,
            name = "Haymarket",
            locationType = LocationType.STOP,
            description = "Haymarket - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-haecl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70026",
            latitude = 42.36528,
            longitude = -71.060205,
            name = "North Station",
            locationType = LocationType.STOP,
            description = "North Station - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70027",
            latitude = 42.36528,
            longitude = -71.060205,
            name = "North Station",
            locationType = LocationType.STOP,
            description = "North Station - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70028",
            latitude = 42.373622,
            longitude = -71.069533,
            name = "Community College",
            locationType = LocationType.STOP,
            description = "Community College - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-ccmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70029",
            latitude = 42.373622,
            longitude = -71.069533,
            name = "Community College",
            locationType = LocationType.STOP,
            description = "Community College - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-ccmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70030",
            latitude = 42.383975,
            longitude = -71.076994,
            name = "Sullivan Square",
            locationType = LocationType.STOP,
            description = "Sullivan Square - Orange Line - Forest Hills",
            platformCode = "1",
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sull",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70031",
            latitude = 42.383975,
            longitude = -71.076994,
            name = "Sullivan Square",
            locationType = LocationType.STOP,
            description = "Sullivan Square - Orange Line - Oak Grove",
            platformCode = "2",
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sull",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70032",
            latitude = 42.401505,
            longitude = -71.077252,
            name = "Wellington",
            locationType = LocationType.STOP,
            description = "Wellington - Orange Line - Forest Hills",
            platformCode = "1",
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-welln",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70033",
            latitude = 42.401507,
            longitude = -71.07715,
            name = "Wellington",
            locationType = LocationType.STOP,
            description = "Wellington - Orange Line - Oak Grove",
            platformCode = "2",
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-welln",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70034",
            latitude = 42.426677,
            longitude = -71.074381,
            name = "Malden Center",
            locationType = LocationType.STOP,
            description = "Malden Center - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-mlmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70035",
            latitude = 42.426662,
            longitude = -71.074314,
            name = "Malden Center",
            locationType = LocationType.STOP,
            description = "Malden Center - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-mlmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70036",
            latitude = 42.437735,
            longitude = -71.070875,
            name = "Oak Grove",
            locationType = LocationType.STOP,
            description = "Oak Grove - Orange Line",
            platformCode = null,
            platformName = "Orange Line",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-ogmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70039",
            latitude = 42.359705,
            longitude = -71.059215,
            name = "Government Center",
            locationType = LocationType.STOP,
            description = "Government Center - Blue Line - Bowdoin",
            platformCode = null,
            platformName = "Bowdoin",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-gover",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70040",
            latitude = 42.359705,
            longitude = -71.059215,
            name = "Government Center",
            locationType = LocationType.STOP,
            description = "Government Center - Blue Line - Wonderland",
            platformCode = null,
            platformName = "Wonderland",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-gover",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70041",
            latitude = 42.358978,
            longitude = -71.057598,
            name = "State",
            locationType = LocationType.STOP,
            description = "State - Blue Line - Bowdoin",
            platformCode = null,
            platformName = "Bowdoin",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-state",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70042",
            latitude = 42.358978,
            longitude = -71.057598,
            name = "State",
            locationType = LocationType.STOP,
            description = "State - Blue Line - Wonderland",
            platformCode = null,
            platformName = "Wonderland",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-state",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70043",
            latitude = 42.359784,
            longitude = -71.051652,
            name = "Aquarium",
            locationType = LocationType.STOP,
            description = "Aquarium - Blue Line - Bowdoin",
            platformCode = "1",
            platformName = "Bowdoin",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-aqucl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70044",
            latitude = 42.359784,
            longitude = -71.051652,
            name = "Aquarium",
            locationType = LocationType.STOP,
            description = "Aquarium - Blue Line - Wonderland",
            platformCode = "2",
            platformName = "Wonderland",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-aqucl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70061",
            latitude = 42.396148,
            longitude = -71.140698,
            name = "Alewife",
            locationType = LocationType.STOP,
            description = "Alewife - Red Line",
            platformCode = null,
            platformName = "Red Line",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-alfcl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70063",
            latitude = 42.39674,
            longitude = -71.121815,
            name = "Davis",
            locationType = LocationType.STOP,
            description = "Davis - Red Line - Ashmont/Braintree",
            platformCode = null,
            platformName = "Ashmont/Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-davis",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70064",
            latitude = 42.39674,
            longitude = -71.121815,
            name = "Davis",
            locationType = LocationType.STOP,
            description = "Davis - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-davis",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70065",
            latitude = 42.3884,
            longitude = -71.119149,
            name = "Porter",
            locationType = LocationType.STOP,
            description = "Porter - Red Line - Ashmont/Braintree",
            platformCode = null,
            platformName = "Ashmont/Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-portr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70066",
            latitude = 42.3884,
            longitude = -71.119149,
            name = "Porter",
            locationType = LocationType.STOP,
            description = "Porter - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-portr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70067",
            latitude = 42.374663,
            longitude = -71.118814,
            name = "Harvard",
            locationType = LocationType.STOP,
            description = "Harvard - Red Line - Ashmont/Braintree",
            platformCode = null,
            platformName = "Ashmont/Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-harsq",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70068",
            latitude = 42.374663,
            longitude = -71.118814,
            name = "Harvard",
            locationType = LocationType.STOP,
            description = "Harvard - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-harsq",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70069",
            latitude = 42.365304,
            longitude = -71.103621,
            name = "Central",
            locationType = LocationType.STOP,
            description = "Central - Red Line - Ashmont/Braintree",
            platformCode = null,
            platformName = "Ashmont/Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-cntsq",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70070",
            latitude = 42.365379,
            longitude = -71.103554,
            name = "Central",
            locationType = LocationType.STOP,
            description = "Central - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-cntsq",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70071",
            latitude = 42.362355,
            longitude = -71.085605,
            name = "Kendall/MIT",
            locationType = LocationType.STOP,
            description = "Kendall/MIT - Red Line - Ashmont/Braintree",
            platformCode = null,
            platformName = "Ashmont/Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-knncl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70072",
            latitude = 42.362434,
            longitude = -71.085591,
            name = "Kendall/MIT",
            locationType = LocationType.STOP,
            description = "Kendall/MIT - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-knncl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70073",
            latitude = 42.361187,
            longitude = -71.071505,
            name = "Charles/MGH",
            locationType = LocationType.STOP,
            description = "Charles/MGH - Red Line - Ashmont/Braintree",
            platformCode = null,
            platformName = "Ashmont/Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-chmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70074",
            latitude = 42.361263,
            longitude = -71.071602,
            name = "Charles/MGH",
            locationType = LocationType.STOP,
            description = "Charles/MGH - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-chmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70075",
            latitude = 42.356395,
            longitude = -71.062424,
            name = "Park Street",
            locationType = LocationType.STOP,
            description = "Park Street - Red Line - Ashmont/Braintree",
            platformCode = null,
            platformName = "Ashmont/Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-pktrm",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70076",
            latitude = 42.356395,
            longitude = -71.062424,
            name = "Park Street",
            locationType = LocationType.STOP,
            description = "Park Street - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-pktrm",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70077",
            latitude = 42.355518,
            longitude = -71.060225,
            name = "Downtown Crossing",
            locationType = LocationType.STOP,
            description = "Downtown Crossing - Red Line - Ashmont/Braintree",
            platformCode = null,
            platformName = "Ashmont/Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-dwnxg",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70078",
            latitude = 42.355518,
            longitude = -71.060225,
            name = "Downtown Crossing",
            locationType = LocationType.STOP,
            description = "Downtown Crossing - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-dwnxg",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70079",
            latitude = 42.352271,
            longitude = -71.055242,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Red Line - Ashmont/Braintree",
            platformCode = null,
            platformName = "Ashmont/Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70080",
            latitude = 42.352271,
            longitude = -71.055242,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70081",
            latitude = 42.342622,
            longitude = -71.056967,
            name = "Broadway",
            locationType = LocationType.STOP,
            description = "Broadway - Red Line - Ashmont/Braintree",
            platformCode = null,
            platformName = "Ashmont/Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-brdwy",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70082",
            latitude = 42.342622,
            longitude = -71.056967,
            name = "Broadway",
            locationType = LocationType.STOP,
            description = "Broadway - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-brdwy",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70083",
            latitude = 42.330154,
            longitude = -71.057655,
            name = "Andrew",
            locationType = LocationType.STOP,
            description = "Andrew - Red Line - Ashmont/Braintree",
            platformCode = "1",
            platformName = "Ashmont/Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-andrw",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70084",
            latitude = 42.330154,
            longitude = -71.057655,
            name = "Andrew",
            locationType = LocationType.STOP,
            description = "Andrew - Red Line - Alewife",
            platformCode = "2",
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-andrw",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70085",
            latitude = 42.320632,
            longitude = -71.052514,
            name = "JFK/UMass",
            locationType = LocationType.STOP,
            description = "JFK/UMass - Red Line - Ashmont",
            platformCode = null,
            platformName = "Ashmont",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-jfk",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70086",
            latitude = 42.320637,
            longitude = -71.052473,
            name = "JFK/UMass",
            locationType = LocationType.STOP,
            description = "JFK/UMass - Red Line - Alewife (from Ashmont)",
            platformCode = null,
            platformName = "Alewife (from Ashmont)",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-jfk",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70087",
            latitude = 42.310603,
            longitude = -71.053678,
            name = "Savin Hill",
            locationType = LocationType.STOP,
            description = "Savin Hill - Red Line - Ashmont",
            platformCode = null,
            platformName = "Ashmont",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-shmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70088",
            latitude = 42.310591,
            longitude = -71.053612,
            name = "Savin Hill",
            locationType = LocationType.STOP,
            description = "Savin Hill - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-shmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70089",
            latitude = 42.299993,
            longitude = -71.062021,
            name = "Fields Corner",
            locationType = LocationType.STOP,
            description = "Fields Corner - Red Line - Ashmont",
            platformCode = null,
            platformName = "Ashmont",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-fldcr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70090",
            latitude = 42.299935,
            longitude = -71.061945,
            name = "Fields Corner",
            locationType = LocationType.STOP,
            description = "Fields Corner - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-fldcr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70091",
            latitude = 42.293126,
            longitude = -71.065738,
            name = "Shawmut",
            locationType = LocationType.STOP,
            description = "Shawmut - Red Line - Ashmont",
            platformCode = null,
            platformName = "Ashmont",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-smmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70092",
            latitude = 42.293126,
            longitude = -71.065738,
            name = "Shawmut",
            locationType = LocationType.STOP,
            description = "Shawmut - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-smmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70093",
            latitude = 42.284508,
            longitude = -71.063833,
            name = "Ashmont",
            locationType = LocationType.STOP,
            description = "Ashmont - Red Line - Exit Only",
            platformCode = null,
            platformName = "Exit Only",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-asmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70094",
            latitude = 42.28453,
            longitude = -71.063725,
            name = "Ashmont",
            locationType = LocationType.STOP,
            description = "Ashmont - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-asmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70095",
            latitude = 42.320418,
            longitude = -71.052287,
            name = "JFK/UMass",
            locationType = LocationType.STOP,
            description = "JFK/UMass - Red Line - Braintree",
            platformCode = null,
            platformName = "Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-jfk",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70096",
            latitude = 42.320434,
            longitude = -71.052215,
            name = "JFK/UMass",
            locationType = LocationType.STOP,
            description = "JFK/UMass - Red Line - Alewife (from Braintree)",
            platformCode = null,
            platformName = "Alewife (from Braintree)",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-jfk",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70097",
            latitude = 42.27577,
            longitude = -71.030194,
            name = "North Quincy",
            locationType = LocationType.STOP,
            description = "North Quincy - Red Line - Braintree",
            platformCode = "2",
            platformName = "Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-nqncy",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70098",
            latitude = 42.275802,
            longitude = -71.030144,
            name = "North Quincy",
            locationType = LocationType.STOP,
            description = "North Quincy - Red Line - Alewife",
            platformCode = "1",
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-nqncy",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70099",
            latitude = 42.266762,
            longitude = -71.020542,
            name = "Wollaston",
            locationType = LocationType.STOP,
            description = "Wollaston - Red Line - Braintree",
            platformCode = null,
            platformName = "Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-wlsta",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70100",
            latitude = 42.266788,
            longitude = -71.020457,
            name = "Wollaston",
            locationType = LocationType.STOP,
            description = "Wollaston - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-wlsta",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70101",
            latitude = 42.251809,
            longitude = -71.005409,
            name = "Quincy Center",
            locationType = LocationType.STOP,
            description = "Quincy Center - Red Line - Braintree",
            platformCode = "2",
            platformName = "Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-qnctr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70102",
            latitude = 42.251809,
            longitude = -71.005409,
            name = "Quincy Center",
            locationType = LocationType.STOP,
            description = "Quincy Center - Red Line - Alewife",
            platformCode = "1",
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-qnctr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70103",
            latitude = 42.233391,
            longitude = -71.007153,
            name = "Quincy Adams",
            locationType = LocationType.STOP,
            description = "Quincy Adams - Red Line - Braintree",
            platformCode = null,
            platformName = "Braintree",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-qamnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70104",
            latitude = 42.233391,
            longitude = -71.007153,
            name = "Quincy Adams",
            locationType = LocationType.STOP,
            description = "Quincy Adams - Red Line - Alewife",
            platformCode = null,
            platformName = "Alewife",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-qamnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70105",
            latitude = 42.207424,
            longitude = -71.001645,
            name = "Braintree",
            locationType = LocationType.STOP,
            description = "Braintree - Red Line",
            platformCode = null,
            platformName = "Red Line",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-brntn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70106",
            latitude = 42.340149,
            longitude = -71.167029,
            name = "Boston College",
            locationType = LocationType.STOP,
            description = "Boston College - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-lake",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70107",
            latitude = 42.34024,
            longitude = -71.166849,
            name = "Boston College",
            locationType = LocationType.STOP,
            description = "Boston College - Green Line - Exit Only",
            platformCode = null,
            platformName = "Exit Only",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-lake",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70110",
            latitude = 42.339371,
            longitude = -71.157057,
            name = "South Street",
            locationType = LocationType.STOP,
            description = "South Street - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sougr",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70111",
            latitude = 42.339581,
            longitude = -71.157499,
            name = "South Street",
            locationType = LocationType.STOP,
            description = "South Street - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sougr",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70112",
            latitude = 42.33873,
            longitude = -71.152526,
            name = "Chestnut Hill Avenue",
            locationType = LocationType.STOP,
            description = "Chestnut Hill Avenue - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-chill",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70113",
            latitude = 42.33829,
            longitude = -71.153025,
            name = "Chestnut Hill Avenue",
            locationType = LocationType.STOP,
            description = "Chestnut Hill Avenue - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-chill",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70114",
            latitude = 42.340808,
            longitude = -71.150633,
            name = "Chiswick Road",
            locationType = LocationType.STOP,
            description = "Chiswick Road - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-chswk",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70115",
            latitude = 42.34054,
            longitude = -71.15114,
            name = "Chiswick Road",
            locationType = LocationType.STOP,
            description = "Chiswick Road - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-chswk",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70116",
            latitude = 42.341589,
            longitude = -71.146089,
            name = "Sutherland Road",
            locationType = LocationType.STOP,
            description = "Sutherland Road - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sthld",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70117",
            latitude = 42.341577,
            longitude = -71.146607,
            name = "Sutherland Road",
            locationType = LocationType.STOP,
            description = "Sutherland Road - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sthld",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70120",
            latitude = 42.344329,
            longitude = -71.142385,
            name = "Washington Street",
            locationType = LocationType.STOP,
            description = "Washington Street - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-wascm",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70121",
            latitude = 42.343974,
            longitude = -71.142731,
            name = "Washington Street",
            locationType = LocationType.STOP,
            description = "Washington Street - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-wascm",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70124",
            latitude = 42.348285,
            longitude = -71.140436,
            name = "Warren Street",
            locationType = LocationType.STOP,
            description = "Warren Street - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-wrnst",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70125",
            latitude = 42.348819,
            longitude = -71.140051,
            name = "Warren Street",
            locationType = LocationType.STOP,
            description = "Warren Street - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-wrnst",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70126",
            latitude = 42.348649,
            longitude = -71.137881,
            name = "Allston Street",
            locationType = LocationType.STOP,
            description = "Allston Street - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-alsgr",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70127",
            latitude = 42.348546,
            longitude = -71.137362,
            name = "Allston Street",
            locationType = LocationType.STOP,
            description = "Allston Street - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-alsgr",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70128",
            latitude = 42.348747,
            longitude = -71.1345,
            name = "Griggs Street",
            locationType = LocationType.STOP,
            description = "Griggs Street - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-grigg",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70129",
            latitude = 42.348919,
            longitude = -71.134305,
            name = "Griggs Street",
            locationType = LocationType.STOP,
            description = "Griggs Street - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-grigg",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70130",
            latitude = 42.350263,
            longitude = -71.131298,
            name = "Harvard Avenue",
            locationType = LocationType.STOP,
            description = "Harvard Avenue - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-harvd",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70131",
            latitude = 42.350602,
            longitude = -71.130727,
            name = "Harvard Avenue",
            locationType = LocationType.STOP,
            description = "Harvard Avenue - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-harvd",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70134",
            latitude = 42.351891,
            longitude = -71.125067,
            name = "Packard's Corner",
            locationType = LocationType.STOP,
            description = "Packard's Corner - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-brico",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70135",
            latitude = 42.351651,
            longitude = -71.12551,
            name = "Packard's Corner",
            locationType = LocationType.STOP,
            description = "Packard's Corner - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-brico",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70144",
            latitude = 42.350013,
            longitude = -71.106902,
            name = "Boston University Central",
            locationType = LocationType.STOP,
            description = "Boston University Central - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bucen",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70145",
            latitude = 42.350148,
            longitude = -71.107455,
            name = "Boston University Central",
            locationType = LocationType.STOP,
            description = "Boston University Central - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bucen",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70146",
            latitude = 42.349659,
            longitude = -71.103989,
            name = "Boston University East",
            locationType = LocationType.STOP,
            description = "Boston University East - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-buest",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70147",
            latitude = 42.349811,
            longitude = -71.104657,
            name = "Boston University East",
            locationType = LocationType.STOP,
            description = "Boston University East - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-buest",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70148",
            latitude = 42.349165,
            longitude = -71.099821,
            name = "Blandford Street",
            locationType = LocationType.STOP,
            description = "Blandford Street - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bland",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70149",
            latitude = 42.349276,
            longitude = -71.100213,
            name = "Blandford Street",
            locationType = LocationType.STOP,
            description = "Blandford Street - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bland",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70150",
            latitude = 42.348949,
            longitude = -71.095169,
            name = "Kenmore",
            locationType = LocationType.STOP,
            description = "Kenmore - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-kencl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70151",
            latitude = 42.348949,
            longitude = -71.095169,
            name = "Kenmore",
            locationType = LocationType.STOP,
            description = "Kenmore - Green Line - (C) Cleveland Circle, (D) Riverside",
            platformCode = null,
            platformName = "Cleveland Circle, Riverside",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-kencl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70152",
            latitude = 42.347888,
            longitude = -71.087903,
            name = "Hynes Convention Center",
            locationType = LocationType.STOP,
            description = "Hynes Convention Center - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-hymnl",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70153",
            latitude = 42.347888,
            longitude = -71.087903,
            name = "Hynes Convention Center",
            locationType = LocationType.STOP,
            description = "Hynes Convention Center - Green Line - Kenmore & West",
            platformCode = null,
            platformName = "Kenmore & West",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-hymnl",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70154",
            latitude = 42.349871,
            longitude = -71.078049,
            name = "Copley",
            locationType = LocationType.STOP,
            description = "Copley - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-coecl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70155",
            latitude = 42.350126,
            longitude = -71.077376,
            name = "Copley",
            locationType = LocationType.STOP,
            description = "Copley - Green Line - Kenmore & West, (E) Heath Street",
            platformCode = null,
            platformName = "Kenmore & West, Heath Street",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-coecl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70156",
            latitude = 42.351902,
            longitude = -71.070893,
            name = "Arlington",
            locationType = LocationType.STOP,
            description = "Arlington - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-armnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70157",
            latitude = 42.351902,
            longitude = -71.070893,
            name = "Arlington",
            locationType = LocationType.STOP,
            description = "Arlington - Green Line - Copley & West",
            platformCode = null,
            platformName = "Copley & West",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-armnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70158",
            latitude = 42.352531,
            longitude = -71.064682,
            name = "Boylston",
            locationType = LocationType.STOP,
            description = "Boylston - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-boyls",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70159",
            latitude = 42.353214,
            longitude = -71.064545,
            name = "Boylston",
            locationType = LocationType.STOP,
            description = "Boylston - Green Line - Copley & West",
            platformCode = null,
            platformName = "Copley & West",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-boyls",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70160",
            latitude = 42.337317,
            longitude = -71.252256,
            name = "Riverside",
            locationType = LocationType.STOP,
            description = "Riverside - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-river",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70161",
            latitude = 42.337348,
            longitude = -71.252236,
            name = "Riverside",
            locationType = LocationType.STOP,
            description = "Riverside - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-river",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70162",
            latitude = 42.332703,
            longitude = -71.243055,
            name = "Woodland",
            locationType = LocationType.STOP,
            description = "Woodland - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-woodl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70163",
            latitude = 42.333094,
            longitude = -71.243659,
            name = "Woodland",
            locationType = LocationType.STOP,
            description = "Woodland - Green Line - (D) Riverside",
            platformCode = null,
            platformName = "Riverside",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-woodl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70164",
            latitude = 42.325695,
            longitude = -71.230476,
            name = "Waban",
            locationType = LocationType.STOP,
            description = "Waban - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-waban",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70165",
            latitude = 42.325967,
            longitude = -71.230714,
            name = "Waban",
            locationType = LocationType.STOP,
            description = "Waban - Green Line - (D) Riverside",
            platformCode = null,
            platformName = "Riverside",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-waban",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70166",
            latitude = 42.318871,
            longitude = -71.21642,
            name = "Eliot",
            locationType = LocationType.STOP,
            description = "Eliot - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-eliot",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70167",
            latitude = 42.319214,
            longitude = -71.216949,
            name = "Eliot",
            locationType = LocationType.STOP,
            description = "Eliot - Green Line - (D) Riverside",
            platformCode = null,
            platformName = "Riverside",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-eliot",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70168",
            latitude = 42.322738,
            longitude = -71.205082,
            name = "Newton Highlands",
            locationType = LocationType.STOP,
            description = "Newton Highlands - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-newtn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70169",
            latitude = 42.32253,
            longitude = -71.205421,
            name = "Newton Highlands",
            locationType = LocationType.STOP,
            description = "Newton Highlands - Green Line - (D) Riverside",
            platformCode = null,
            platformName = "Riverside",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-newtn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70170",
            latitude = 42.329552,
            longitude = -71.192024,
            name = "Newton Centre",
            locationType = LocationType.STOP,
            description = "Newton Centre - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-newto",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70171",
            latitude = 42.3294,
            longitude = -71.192622,
            name = "Newton Centre",
            locationType = LocationType.STOP,
            description = "Newton Centre - Green Line - (D) Riverside",
            platformCode = null,
            platformName = "Riverside",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-newto",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70172",
            latitude = 42.326799,
            longitude = -71.164146,
            name = "Chestnut Hill",
            locationType = LocationType.STOP,
            description = "Chestnut Hill - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-chhil",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70173",
            latitude = 42.326782,
            longitude = -71.16478,
            name = "Chestnut Hill",
            locationType = LocationType.STOP,
            description = "Chestnut Hill - Green Line - (D) Riverside",
            platformCode = null,
            platformName = "Riverside",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-chhil",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70174",
            latitude = 42.335181,
            longitude = -71.147879,
            name = "Reservoir",
            locationType = LocationType.STOP,
            description = "Reservoir - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rsmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70175",
            latitude = 42.335163,
            longitude = -71.148601,
            name = "Reservoir",
            locationType = LocationType.STOP,
            description = "Reservoir - Green Line - (D) Riverside",
            platformCode = null,
            platformName = "Riverside",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rsmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70176",
            latitude = 42.335625,
            longitude = -71.140112,
            name = "Beaconsfield",
            locationType = LocationType.STOP,
            description = "Beaconsfield - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bcnfd",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70177",
            latitude = 42.335912,
            longitude = -71.140903,
            name = "Beaconsfield",
            locationType = LocationType.STOP,
            description = "Beaconsfield - Green Line - (D) Riverside",
            platformCode = null,
            platformName = "Riverside",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bcnfd",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70178",
            latitude = 42.331293,
            longitude = -71.126406,
            name = "Brookline Hills",
            locationType = LocationType.STOP,
            description = "Brookline Hills - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-brkhl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70179",
            latitude = 42.331413,
            longitude = -71.127025,
            name = "Brookline Hills",
            locationType = LocationType.STOP,
            description = "Brookline Hills - Green Line - (D) Riverside",
            platformCode = null,
            platformName = "Riverside",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-brkhl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70180",
            latitude = 42.332614,
            longitude = -71.116751,
            name = "Brookline Village",
            locationType = LocationType.STOP,
            description = "Brookline Village - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bvmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70181",
            latitude = 42.33257,
            longitude = -71.117041,
            name = "Brookline Village",
            locationType = LocationType.STOP,
            description = "Brookline Village - Green Line - (D) Riverside",
            platformCode = null,
            platformName = "Riverside",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bvmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70182",
            latitude = 42.341808,
            longitude = -71.109777,
            name = "Longwood",
            locationType = LocationType.STOP,
            description = "Longwood - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-longw",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70183",
            latitude = 42.341571,
            longitude = -71.110147,
            name = "Longwood",
            locationType = LocationType.STOP,
            description = "Longwood - Green Line - (D) Riverside",
            platformCode = null,
            platformName = "Riverside",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-longw",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70186",
            latitude = 42.345328,
            longitude = -71.104269,
            name = "Fenway",
            locationType = LocationType.STOP,
            description = "Fenway - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-fenwy",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70187",
            latitude = 42.345029,
            longitude = -71.104968,
            name = "Fenway",
            locationType = LocationType.STOP,
            description = "Fenway - Green Line - (D) Riverside",
            platformCode = null,
            platformName = "Riverside",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-fenwy",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70196",
            latitude = 42.356395,
            longitude = -71.062424,
            name = "Park Street",
            locationType = LocationType.STOP,
            description = "Park Street - Green Line - (B) Boston College",
            platformCode = "B",
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-pktrm",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70197",
            latitude = 42.356395,
            longitude = -71.062424,
            name = "Park Street",
            locationType = LocationType.STOP,
            description = "Park Street - Green Line - (C) Cleveland Circle",
            platformCode = "C",
            platformName = "Cleveland Circle",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-pktrm",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70198",
            latitude = 42.356395,
            longitude = -71.062424,
            name = "Park Street",
            locationType = LocationType.STOP,
            description = "Park Street - Green Line - (D) Riverside",
            platformCode = "D",
            platformName = "Riverside",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-pktrm",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70199",
            latitude = 42.356395,
            longitude = -71.062424,
            name = "Park Street",
            locationType = LocationType.STOP,
            description = "Park Street - Green Line - (E) Heath Street",
            platformCode = "E",
            platformName = "Heath Street",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-pktrm",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70200",
            latitude = 42.356395,
            longitude = -71.062424,
            name = "Park Street",
            locationType = LocationType.STOP,
            description = "Park Street - Green Line - Government Center & North",
            platformCode = null,
            platformName = "Government Center & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-pktrm",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70201",
            latitude = 42.359705,
            longitude = -71.059215,
            name = "Government Center",
            locationType = LocationType.STOP,
            description = "Government Center - Green Line - North Station & North",
            platformCode = null,
            platformName = "North Station & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-gover",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70202",
            latitude = 42.359705,
            longitude = -71.059215,
            name = "Government Center",
            locationType = LocationType.STOP,
            description = "Government Center - Green Line - Copley & West",
            platformCode = null,
            platformName = "Copley & West",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-gover",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70203",
            latitude = 42.363021,
            longitude = -71.05829,
            name = "Haymarket",
            locationType = LocationType.STOP,
            description = "Haymarket - Green Line - North Station & North",
            platformCode = null,
            platformName = "North Station & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-haecl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70204",
            latitude = 42.363021,
            longitude = -71.05829,
            name = "Haymarket",
            locationType = LocationType.STOP,
            description = "Haymarket - Green Line - Copley & West",
            platformCode = null,
            platformName = "Copley & West",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-haecl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70205",
            latitude = 42.36528,
            longitude = -71.060205,
            name = "North Station",
            locationType = LocationType.STOP,
            description = "North Station - Green Line - Lechmere & North",
            platformCode = null,
            platformName = "Lechmere & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70206",
            latitude = 42.36528,
            longitude = -71.060205,
            name = "North Station",
            locationType = LocationType.STOP,
            description = "North Station - Green Line - Copley & West",
            platformCode = null,
            platformName = "Copley & West",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70207",
            latitude = 42.366664,
            longitude = -71.067666,
            name = "Science Park/West End",
            locationType = LocationType.STOP,
            description = "Science Park/West End - Green Line - Lechmere & North",
            platformCode = null,
            platformName = "Lechmere & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-spmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70208",
            latitude = 42.366664,
            longitude = -71.067666,
            name = "Science Park/West End",
            locationType = LocationType.STOP,
            description = "Science Park/West End - Green Line - Copley & West",
            platformCode = null,
            platformName = "Copley & West",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-spmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70211",
            latitude = 42.345884,
            longitude = -71.107697,
            name = "Saint Mary's Street",
            locationType = LocationType.STOP,
            description = "Saint Mary's Street - Green Line - (C) Cleveland Circle",
            platformCode = null,
            platformName = "Cleveland Circle",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-smary",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70212",
            latitude = 42.346007,
            longitude = -71.107166,
            name = "Saint Mary's Street",
            locationType = LocationType.STOP,
            description = "Saint Mary's Street - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-smary",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70213",
            latitude = 42.344758,
            longitude = -71.111761,
            name = "Hawes Street",
            locationType = LocationType.STOP,
            description = "Hawes Street - Green Line - (C) Cleveland Circle",
            platformCode = null,
            platformName = "Cleveland Circle",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-hwsst",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70214",
            latitude = 42.344867,
            longitude = -71.111157,
            name = "Hawes Street",
            locationType = LocationType.STOP,
            description = "Hawes Street - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-hwsst",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70215",
            latitude = 42.344117,
            longitude = -71.114097,
            name = "Kent Street",
            locationType = LocationType.STOP,
            description = "Kent Street - Green Line - (C) Cleveland Circle",
            platformCode = null,
            platformName = "Cleveland Circle",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-kntst",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70216",
            latitude = 42.343927,
            longitude = -71.114569,
            name = "Kent Street",
            locationType = LocationType.STOP,
            description = "Kent Street - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-kntst",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70217",
            latitude = 42.34334,
            longitude = -71.116927,
            name = "Saint Paul Street",
            locationType = LocationType.STOP,
            description = "Saint Paul Street - Green Line - (C) Cleveland Circle",
            platformCode = null,
            platformName = "Cleveland Circle",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-stpul",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70218",
            latitude = 42.343118,
            longitude = -71.117498,
            name = "Saint Paul Street",
            locationType = LocationType.STOP,
            description = "Saint Paul Street - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-stpul",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70219",
            latitude = 42.342274,
            longitude = -71.120915,
            name = "Coolidge Corner",
            locationType = LocationType.STOP,
            description = "Coolidge Corner - Green Line - (C) Cleveland Circle",
            platformCode = null,
            platformName = "Cleveland Circle",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-cool",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70220",
            latitude = 42.342028,
            longitude = -71.121685,
            name = "Coolidge Corner",
            locationType = LocationType.STOP,
            description = "Coolidge Corner - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-cool",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70223",
            latitude = 42.34112,
            longitude = -71.125652,
            name = "Summit Avenue",
            locationType = LocationType.STOP,
            description = "Summit Avenue - Green Line - (C) Cleveland Circle",
            platformCode = null,
            platformName = "Cleveland Circle",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sumav",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70224",
            latitude = 42.341027,
            longitude = -71.125759,
            name = "Summit Avenue",
            locationType = LocationType.STOP,
            description = "Summit Avenue - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sumav",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70225",
            latitude = 42.340053,
            longitude = -71.128869,
            name = "Brandon Hall",
            locationType = LocationType.STOP,
            description = "Brandon Hall - Green Line - (C) Cleveland Circle",
            platformCode = null,
            platformName = "Cleveland Circle",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bndhl",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70226",
            latitude = 42.340038,
            longitude = -71.12866,
            name = "Brandon Hall",
            locationType = LocationType.STOP,
            description = "Brandon Hall - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bndhl",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70227",
            latitude = 42.33969,
            longitude = -71.131228,
            name = "Fairbanks Street",
            locationType = LocationType.STOP,
            description = "Fairbanks Street - Green Line - (C) Cleveland Circle",
            platformCode = null,
            platformName = "Cleveland Circle",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-fbkst",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70228",
            latitude = 42.339644,
            longitude = -71.131078,
            name = "Fairbanks Street",
            locationType = LocationType.STOP,
            description = "Fairbanks Street - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-fbkst",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70229",
            latitude = 42.339471,
            longitude = -71.135139,
            name = "Washington Square",
            locationType = LocationType.STOP,
            description = "Washington Square - Green Line - (C) Cleveland Circle",
            platformCode = null,
            platformName = "Cleveland Circle",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bcnwa",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70230",
            latitude = 42.339519,
            longitude = -71.134587,
            name = "Washington Square",
            locationType = LocationType.STOP,
            description = "Washington Square - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bcnwa",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70231",
            latitude = 42.338498,
            longitude = -71.138731,
            name = "Tappan Street",
            locationType = LocationType.STOP,
            description = "Tappan Street - Green Line - (C) Cleveland Circle",
            platformCode = null,
            platformName = "Cleveland Circle",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-tapst",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70232",
            latitude = 42.338567,
            longitude = -71.13819,
            name = "Tappan Street",
            locationType = LocationType.STOP,
            description = "Tappan Street - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-tapst",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70233",
            latitude = 42.337807,
            longitude = -71.141753,
            name = "Dean Road",
            locationType = LocationType.STOP,
            description = "Dean Road - Green Line - (C) Cleveland Circle",
            platformCode = null,
            platformName = "Cleveland Circle",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-denrd",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70234",
            latitude = 42.337628,
            longitude = -71.142309,
            name = "Dean Road",
            locationType = LocationType.STOP,
            description = "Dean Road - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-denrd",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70235",
            latitude = 42.336964,
            longitude = -71.145867,
            name = "Englewood Avenue",
            locationType = LocationType.STOP,
            description = "Englewood Avenue - Green Line - (C) Cleveland Circle",
            platformCode = null,
            platformName = "Cleveland Circle",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-engav",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70236",
            latitude = 42.337011,
            longitude = -71.145368,
            name = "Englewood Avenue",
            locationType = LocationType.STOP,
            description = "Englewood Avenue - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-engav",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70237",
            latitude = 42.336216,
            longitude = -71.149201,
            name = "Cleveland Circle",
            locationType = LocationType.STOP,
            description = "Cleveland Circle - Green Line - Exit Only",
            platformCode = null,
            platformName = "Exit Only",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-clmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70238",
            latitude = 42.336252,
            longitude = -71.148774,
            name = "Cleveland Circle",
            locationType = LocationType.STOP,
            description = "Cleveland Circle - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-clmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70239",
            latitude = 42.34557,
            longitude = -71.081696,
            name = "Prudential",
            locationType = LocationType.STOP,
            description = "Prudential - Green Line - (E) Heath Street",
            platformCode = null,
            platformName = "Heath Street",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-prmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70240",
            latitude = 42.34557,
            longitude = -71.081696,
            name = "Prudential",
            locationType = LocationType.STOP,
            description = "Prudential - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-prmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70241",
            latitude = 42.342687,
            longitude = -71.085056,
            name = "Symphony",
            locationType = LocationType.STOP,
            description = "Symphony - Green Line - (E) Heath Street",
            platformCode = null,
            platformName = "Heath Street",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-symcl",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70242",
            latitude = 42.342687,
            longitude = -71.085056,
            name = "Symphony",
            locationType = LocationType.STOP,
            description = "Symphony - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-symcl",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70243",
            latitude = 42.339897,
            longitude = -71.09021,
            name = "Northeastern University",
            locationType = LocationType.STOP,
            description = "Northeastern University - Green Line - (E) Heath Street",
            platformCode = null,
            platformName = "Heath Street",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-nuniv",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70244",
            latitude = 42.340222,
            longitude = -71.0892,
            name = "Northeastern University",
            locationType = LocationType.STOP,
            description = "Northeastern University - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-nuniv",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70245",
            latitude = 42.337875,
            longitude = -71.09524,
            name = "Museum of Fine Arts",
            locationType = LocationType.STOP,
            description = "Museum of Fine Arts - Green Line - (E) Heath Street",
            platformCode = null,
            platformName = "Heath Street",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-mfa",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70246",
            latitude = 42.338017,
            longitude = -71.094682,
            name = "Museum of Fine Arts",
            locationType = LocationType.STOP,
            description = "Museum of Fine Arts - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-mfa",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70247",
            latitude = 42.33608,
            longitude = -71.099883,
            name = "Longwood Medical Area",
            locationType = LocationType.STOP,
            description = "Longwood Medical Area - Green Line - (E) Heath Street",
            platformCode = null,
            platformName = "Heath Street",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-lngmd",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70248",
            latitude = 42.336217,
            longitude = -71.099328,
            name = "Longwood Medical Area",
            locationType = LocationType.STOP,
            description = "Longwood Medical Area - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-lngmd",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70249",
            latitude = 42.334331,
            longitude = -71.104487,
            name = "Brigham Circle",
            locationType = LocationType.STOP,
            description = "Brigham Circle - Green Line - (E) Heath Street",
            platformCode = null,
            platformName = "Heath Street",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-brmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70250",
            latitude = 42.334846,
            longitude = -71.103017,
            name = "Brigham Circle",
            locationType = LocationType.STOP,
            description = "Brigham Circle - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-brmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70251",
            latitude = 42.33374,
            longitude = -71.105721,
            name = "Fenwood Road",
            locationType = LocationType.STOP,
            description = "Fenwood Road - Green Line - (E) Heath Street",
            platformCode = null,
            platformName = "Heath Street",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-fenwd",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70252",
            latitude = 42.333706,
            longitude = -71.105583,
            name = "Fenwood Road",
            locationType = LocationType.STOP,
            description = "Fenwood Road - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-fenwd",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70253",
            latitude = 42.333279,
            longitude = -71.109276,
            name = "Mission Park",
            locationType = LocationType.STOP,
            description = "Mission Park - Green Line - (E) Heath Street",
            platformCode = null,
            platformName = "Heath Street",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-mispk",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70254",
            latitude = 42.333092,
            longitude = -71.10968,
            name = "Mission Park",
            locationType = LocationType.STOP,
            description = "Mission Park - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-mispk",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70255",
            latitude = 42.331391,
            longitude = -71.111925,
            name = "Riverway",
            locationType = LocationType.STOP,
            description = "Riverway - Green Line - (E) Heath Street",
            platformCode = null,
            platformName = "Heath Street",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rvrwy",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70256",
            latitude = 42.331871,
            longitude = -71.111961,
            name = "Riverway",
            locationType = LocationType.STOP,
            description = "Riverway - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rvrwy",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70257",
            latitude = 42.329369,
            longitude = -71.111046,
            name = "Back of the Hill",
            locationType = LocationType.STOP,
            description = "Back of the Hill - Green Line - (E) Heath Street",
            platformCode = null,
            platformName = "Heath Street",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bckhl",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70258",
            latitude = 42.329399,
            longitude = -71.110931,
            name = "Back of the Hill",
            locationType = LocationType.STOP,
            description = "Back of the Hill - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bckhl",
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70260",
            latitude = 42.328316,
            longitude = -71.110252,
            name = "Heath Street",
            locationType = LocationType.STOP,
            description = "Heath Street - Green Line",
            platformCode = null,
            platformName = "Green Line",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-hsmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70261",
            latitude = 42.283768,
            longitude = -71.063191,
            name = "Ashmont",
            locationType = LocationType.STOP,
            description = "Ashmont - Mattapan Trolley",
            platformCode = null,
            platformName = "Mattapan Trolley",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-asmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70278",
            latitude = 42.392331,
            longitude = -71.077262,
            name = "Assembly",
            locationType = LocationType.STOP,
            description = "Assembly - Orange Line - Forest Hills",
            platformCode = null,
            platformName = "Forest Hills",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-astao",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70279",
            latitude = 42.392336,
            longitude = -71.07718,
            name = "Assembly",
            locationType = LocationType.STOP,
            description = "Assembly - Orange Line - Oak Grove",
            platformCode = null,
            platformName = "Oak Grove",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-astao",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70500",
            latitude = 42.371962,
            longitude = -71.07752,
            name = "Lechmere",
            locationType = LocationType.STOP,
            description = "Lechmere - Water St Busway",
            platformCode = null,
            platformName = "Water St Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-lech",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70501",
            latitude = 42.371566,
            longitude = -71.076457,
            name = "Lechmere",
            locationType = LocationType.STOP,
            description = "Lechmere - Green Line - (D) Union Square, (E) Medford/Tufts",
            platformCode = null,
            platformName = "Union Square, Medford/Tufts",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-lech",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70502",
            latitude = 42.370989,
            longitude = -71.07586,
            name = "Lechmere",
            locationType = LocationType.STOP,
            description = "Lechmere - Green Line - Copley & West",
            platformCode = null,
            platformName = "Copley & West",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-lech",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70503",
            latitude = 42.377024,
            longitude = -71.093964,
            name = "Union Square",
            locationType = LocationType.STOP,
            description = "Union Square - Green Line - Copley & West",
            platformCode = null,
            platformName = "Copley & West",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-unsqu",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70504",
            latitude = 42.37699,
            longitude = -71.093993,
            name = "Union Square",
            locationType = LocationType.STOP,
            description = "Union Square - Green Line - Copley & West",
            platformCode = null,
            platformName = "Copley & West",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-unsqu",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70505",
            latitude = 42.388043,
            longitude = -71.096956,
            name = "Gilman Square",
            locationType = LocationType.STOP,
            description = "Gilman Square - Green Line - (E) Medford/Tufts",
            platformCode = null,
            platformName = "Medford/Tufts",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-gilmn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70506",
            latitude = 42.387831,
            longitude = -71.096629,
            name = "Gilman Square",
            locationType = LocationType.STOP,
            description = "Gilman Square - Green Line - Copley & West",
            platformCode = null,
            platformName = "Copley & West",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-gilmn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70507",
            latitude = 42.394357,
            longitude = -71.106824,
            name = "Magoun Square",
            locationType = LocationType.STOP,
            description = "Magoun Square - Green Line - (E) Medford/Tufts",
            platformCode = null,
            platformName = "Medford/Tufts",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-mgngl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70508",
            latitude = 42.39405,
            longitude = -71.106688,
            name = "Magoun Square",
            locationType = LocationType.STOP,
            description = "Magoun Square - Green Line - Copley & West",
            platformCode = null,
            platformName = "Copley & West",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-mgngl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70509",
            latitude = 42.40038,
            longitude = -71.111331,
            name = "Ball Square",
            locationType = LocationType.STOP,
            description = "Ball Square - Green Line - (E) Medford/Tufts",
            platformCode = null,
            platformName = "Medford/Tufts",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-balsq",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70510",
            latitude = 42.400103,
            longitude = -71.111207,
            name = "Ball Square",
            locationType = LocationType.STOP,
            description = "Ball Square - Green Line - Copley & West",
            platformCode = null,
            platformName = "Copley & West",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-balsq",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70511",
            latitude = 42.408306,
            longitude = -71.11728,
            name = "Medford/Tufts",
            locationType = LocationType.STOP,
            description = "Medford/Tufts - Green Line - (E) Medford/Tufts",
            platformCode = null,
            platformName = "Medford/Tufts",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-mdftf",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70512",
            latitude = 42.40828,
            longitude = -71.117339,
            name = "Medford/Tufts",
            locationType = LocationType.STOP,
            description = "Medford/Tufts - Green Line - Copley & West",
            platformCode = null,
            platformName = "Copley & West",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-mdftf",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70513",
            latitude = 42.379881,
            longitude = -71.087032,
            name = "East Somerville",
            locationType = LocationType.STOP,
            description = "East Somerville - Green Line - (E) Medford/Tufts",
            platformCode = null,
            platformName = "Medford/Tufts",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-esomr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "70514",
            latitude = 42.379624,
            longitude = -71.086829,
            name = "East Somerville",
            locationType = LocationType.STOP,
            description = "East Somerville - Green Line - Copley & West",
            platformCode = null,
            platformName = "Copley & West",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-esomr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "71150",
            latitude = 42.348949,
            longitude = -71.095169,
            name = "Kenmore",
            locationType = LocationType.STOP,
            description = "Kenmore - Green Line - Park Street & North",
            platformCode = null,
            platformName = "Park Street & North",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-kencl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "71151",
            latitude = 42.348949,
            longitude = -71.095169,
            name = "Kenmore",
            locationType = LocationType.STOP,
            description = "Kenmore - Green Line - (B) Boston College",
            platformCode = null,
            platformName = "Boston College",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-kencl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "71199",
            latitude = 42.356395,
            longitude = -71.062424,
            name = "Park Street",
            locationType = LocationType.STOP,
            description = "Park Street - Green Line - Drop-off Only",
            platformCode = null,
            platformName = "Drop-off Only",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-pktrm",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "74611",
            latitude = 42.352271,
            longitude = -71.055242,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Silver Line - Airport/Design Center/Chelsea",
            platformCode = null,
            platformName = "Airport/Design Center/Chelsea",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "74617",
            latitude = 42.352271,
            longitude = -71.055242,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Silver Line - Exit Only",
            platformCode = null,
            platformName = "Exit Only",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "76121",
            latitude = 42.37372,
            longitude = -71.119121,
            name = "Harvard",
            locationType = LocationType.STOP,
            description = "Harvard - Upper Busway Berth B1",
            platformCode = "B1",
            platformName = "Upper Busway Berth B1",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-harsq",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "76122",
            latitude = 42.373604,
            longitude = -71.119242,
            name = "Harvard",
            locationType = LocationType.STOP,
            description = "Harvard - Upper Busway Berth B2",
            platformCode = "B2",
            platformName = "Upper Busway Berth B2",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-harsq",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "76123",
            latitude = 42.373516,
            longitude = -71.119582,
            name = "Harvard",
            locationType = LocationType.STOP,
            description = "Harvard - Upper Busway Berth B3",
            platformCode = "B3",
            platformName = "Upper Busway Berth B3",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-harsq",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "76124",
            latitude = 42.373483,
            longitude = -71.119771,
            name = "Harvard",
            locationType = LocationType.STOP,
            description = "Harvard - Upper Busway Berth B4",
            platformCode = "B4",
            platformName = "Upper Busway Berth B4",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-harsq",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "76125",
            latitude = 42.373448,
            longitude = -71.119959,
            name = "Harvard",
            locationType = LocationType.STOP,
            description = "Harvard - Upper Busway Berth B5",
            platformCode = "B5",
            platformName = "Upper Busway Berth B5",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-harsq",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "76126",
            latitude = 42.373414,
            longitude = -71.120134,
            name = "Harvard",
            locationType = LocationType.STOP,
            description = "Harvard - Upper Busway Berth B6",
            platformCode = "B6",
            platformName = "Upper Busway Berth B6",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-harsq",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "76129",
            latitude = 42.373612,
            longitude = -71.119323,
            name = "Harvard",
            locationType = LocationType.STOP,
            description = "Harvard - Lower Busway Berth A3",
            platformCode = "A3",
            platformName = "Lower Busway Berth A3",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-harsq",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "8155",
            latitude = 42.322203,
            longitude = -71.206537,
            name = "Newton Highlands",
            locationType = LocationType.STOP,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-newtn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "8203",
            latitude = 42.321916,
            longitude = -71.206479,
            name = "Newton Highlands",
            locationType = LocationType.STOP,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-newtn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "875",
            latitude = 42.300479,
            longitude = -71.113634,
            name = "Forest Hills",
            locationType = LocationType.STOP,
            description = "Forest Hills - Lower Busway",
            platformCode = null,
            platformName = "Lower Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-forhl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "899",
            latitude = 42.348918,
            longitude = -71.095456,
            name = "Kenmore",
            locationType = LocationType.STOP,
            description = "Kenmore - Busway",
            platformCode = null,
            platformName = "Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-kencl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "9070061",
            latitude = 42.396091,
            longitude = -71.141886,
            name = "Alewife",
            locationType = LocationType.STOP,
            description = "Alewife - Busway Berth A2",
            platformCode = "A2",
            platformName = "Busway Berth A2",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-alfcl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "9070168",
            latitude = 42.322368,
            longitude = -71.205831,
            name = "Newton Highlands",
            locationType = LocationType.STOP,
            description = "Newton Highlands - Green Line Shuttle",
            platformCode = null,
            platformName = "Green Line Shuttle",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-newtn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "9328",
            latitude = 42.436807,
            longitude = -71.070338,
            name = "Oak Grove Busway",
            locationType = LocationType.STOP,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-ogmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "Alewife-01",
            latitude = 42.396218,
            longitude = -71.1407,
            name = "Alewife",
            locationType = LocationType.STOP,
            description = "Alewife - Red Line - Track 1",
            platformCode = "1",
            platformName = "Track 1",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-alfcl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "Alewife-02",
            latitude = 42.396071,
            longitude = -71.140695,
            name = "Alewife",
            locationType = LocationType.STOP,
            description = "Alewife - Red Line - Track 2",
            platformCode = "2",
            platformName = "Track 2",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-alfcl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "BNT-0000",
            latitude = 42.366417,
            longitude = -71.062326,
            name = "North Station",
            locationType = LocationType.STOP,
            description = "North Station - Commuter Rail",
            platformCode = null,
            platformName = "Commuter Rail",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "BNT-0000-01",
            latitude = 42.366761,
            longitude = -71.062365,
            name = "North Station",
            locationType = LocationType.STOP,
            description = "North Station - Commuter Rail - Track 1",
            platformCode = "1",
            platformName = "Commuter Rail - Track 1",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "BNT-0000-02",
            latitude = 42.366761,
            longitude = -71.062365,
            name = "North Station",
            locationType = LocationType.STOP,
            description = "North Station - Commuter Rail - Track 2",
            platformCode = "2",
            platformName = "Commuter Rail - Track 2",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "BNT-0000-03",
            latitude = 42.366687,
            longitude = -71.062475,
            name = "North Station",
            locationType = LocationType.STOP,
            description = "North Station - Commuter Rail - Track 3",
            platformCode = "3",
            platformName = "Commuter Rail - Track 3",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "BNT-0000-04",
            latitude = 42.366687,
            longitude = -71.062475,
            name = "North Station",
            locationType = LocationType.STOP,
            description = "North Station - Commuter Rail - Track 4",
            platformCode = "4",
            platformName = "Commuter Rail - Track 4",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "BNT-0000-05",
            latitude = 42.366618,
            longitude = -71.062601,
            name = "North Station",
            locationType = LocationType.STOP,
            description = "North Station - Commuter Rail - Track 5",
            platformCode = "5",
            platformName = "Commuter Rail - Track 5",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "BNT-0000-06",
            latitude = 42.366618,
            longitude = -71.062601,
            name = "North Station",
            locationType = LocationType.STOP,
            description = "North Station - Commuter Rail - Track 6",
            platformCode = "6",
            platformName = "Commuter Rail - Track 6",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "BNT-0000-07",
            latitude = 42.366561,
            longitude = -71.062713,
            name = "North Station",
            locationType = LocationType.STOP,
            description = "North Station - Commuter Rail - Track 7",
            platformCode = "7",
            platformName = "Commuter Rail - Track 7",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "BNT-0000-08",
            latitude = 42.366561,
            longitude = -71.062713,
            name = "North Station",
            locationType = LocationType.STOP,
            description = "North Station - Commuter Rail - Track 8",
            platformCode = "8",
            platformName = "Commuter Rail - Track 8",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "BNT-0000-09",
            latitude = 42.366493,
            longitude = -71.062829,
            name = "North Station",
            locationType = LocationType.STOP,
            description = "North Station - Commuter Rail - Track 9",
            platformCode = "9",
            platformName = "Commuter Rail - Track 9",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "BNT-0000-10",
            latitude = 42.366493,
            longitude = -71.062829,
            name = "North Station",
            locationType = LocationType.STOP,
            description = "North Station - Commuter Rail - Track 10",
            platformCode = "10",
            platformName = "Commuter Rail - Track 10",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "BNT-0000-B1",
            latitude = 42.366034,
            longitude = -71.060356,
            name = "North Station - Causeway St opp Beverly St",
            locationType = LocationType.STOP,
            description = "North Station - Commuter Rail Shuttle - Causeway St opp Beverly St",
            platformCode = null,
            platformName = "Commuter Rail Shuttle",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "BNT-0000-B2",
            latitude = 42.365641,
            longitude = -71.064022,
            name = "North Station - Nashua St @ Red Auerbach Way",
            locationType = LocationType.STOP,
            description = "North Station - Commuter Rail Shuttle - Nashua St @ Red Auerbach Way",
            platformCode = null,
            platformName = "Commuter Rail Shuttle",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-north",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "Braintree-01",
            latitude = 42.207428,
            longitude = -71.0016,
            name = "Braintree",
            locationType = LocationType.STOP,
            description = "Braintree - Red Line - Track 1",
            platformCode = "1",
            platformName = "Track 1",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-brntn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "Braintree-02",
            latitude = 42.207428,
            longitude = -71.001704,
            name = "Braintree",
            locationType = LocationType.STOP,
            description = "Braintree - Red Line - Track 2",
            platformCode = "2",
            platformName = "Track 2",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-brntn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "FR-0034",
            latitude = 42.388401,
            longitude = -71.119148,
            name = "Porter",
            locationType = LocationType.STOP,
            description = "Porter - Commuter Rail",
            platformCode = null,
            platformName = "Commuter Rail",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-portr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "FR-0034-01",
            latitude = 42.3884,
            longitude = -71.119149,
            name = "Porter",
            locationType = LocationType.STOP,
            description = "Porter - Commuter Rail - Track 1 (Outbound)",
            platformCode = "1",
            platformName = "Commuter Rail - Track 1 (Outbound)",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-portr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "FR-0034-02",
            latitude = 42.3884,
            longitude = -71.119149,
            name = "Porter",
            locationType = LocationType.STOP,
            description = "Porter - Commuter Rail - Track 2 (Boston)",
            platformCode = "2",
            platformName = "Commuter Rail - Track 2 (Boston)",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-portr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "Forest Hills-01",
            latitude = 42.30088,
            longitude = -71.114005,
            name = "Forest Hills",
            locationType = LocationType.STOP,
            description = "Forest Hills - Orange Line - Track 1",
            platformCode = "1",
            platformName = "Track 1",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-forhl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "Forest Hills-02",
            latitude = 42.300972,
            longitude = -71.114283,
            name = "Forest Hills",
            locationType = LocationType.STOP,
            description = "Forest Hills - Orange Line - Track 2",
            platformCode = "2",
            platformName = "Track 2",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-forhl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "Government Center-Brattle",
            latitude = 42.359705,
            longitude = -71.059215,
            name = "Government Center",
            locationType = LocationType.STOP,
            description = "Government Center - Green Line - Loop Platform",
            platformCode = null,
            platformName = "Loop Platform",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-gover",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "MM-0023-S",
            latitude = 42.320685,
            longitude = -71.052391,
            name = "JFK/UMass",
            locationType = LocationType.STOP,
            description = "JFK/UMass - Commuter Rail - Track 1 (All Trains)",
            platformCode = "1",
            platformName = "Track 1 (All Trains)",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-jfk",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "MM-0079-S",
            latitude = 42.251809,
            longitude = -71.005409,
            name = "Quincy Center",
            locationType = LocationType.STOP,
            description = "Quincy Center - Commuter Rail - Track 1 (All Trains)",
            platformCode = "1",
            platformName = "Track 1 (All Trains)",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-qnctr",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "MM-0109",
            latitude = 42.209959,
            longitude = -71.001053,
            name = "Braintree",
            locationType = LocationType.STOP,
            description = "Braintree - Commuter Rail",
            platformCode = null,
            platformName = "Commuter Rail",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-brntn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "MM-0109-CS",
            latitude = 42.209961,
            longitude = -71.001031,
            name = "Braintree",
            locationType = LocationType.STOP,
            description = "Braintree - Commuter Rail - Track 2",
            platformCode = "2",
            platformName = "Commuter Rail - Track 2",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-brntn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "MM-0109-S",
            latitude = 42.209964,
            longitude = -71.001088,
            name = "Braintree",
            locationType = LocationType.STOP,
            description = "Braintree - Commuter Rail - Track 1",
            platformCode = "1",
            platformName = "Commuter Rail - Track 1",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-brntn",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2237",
            latitude = 42.301085,
            longitude = -71.113551,
            name = "Forest Hills",
            locationType = LocationType.STOP,
            description = "Forest Hills - Commuter Rail",
            platformCode = null,
            platformName = "Commuter Rail",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-forhl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2237-03",
            latitude = 42.301065,
            longitude = -71.113491,
            name = "Forest Hills",
            locationType = LocationType.STOP,
            description = "Forest Hills - Commuter Rail - Track 3",
            platformCode = "3",
            platformName = "Commuter Rail - Track 3",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-forhl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2237-05",
            latitude = 42.301105,
            longitude = -71.113625,
            name = "Forest Hills",
            locationType = LocationType.STOP,
            description = "Forest Hills - Commuter Rail - Track 5",
            platformCode = "5",
            platformName = "Commuter Rail - Track 5",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-forhl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2265",
            latitude = 42.336608,
            longitude = -71.089208,
            name = "Ruggles",
            locationType = LocationType.STOP,
            description = "Ruggles - Commuter Rail",
            platformCode = null,
            platformName = "Commuter Rail",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rugg",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2265-01",
            latitude = 42.336339,
            longitude = -71.089517,
            name = "Ruggles",
            locationType = LocationType.STOP,
            description = "Ruggles - Commuter Rail - Track 1",
            platformCode = "1",
            platformName = "Commuter Rail - Track 1",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rugg",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2265-02",
            latitude = 42.337659,
            longitude = -71.087737,
            name = "Ruggles",
            locationType = LocationType.STOP,
            description = "Ruggles - Commuter Rail - Track 2",
            platformCode = "2",
            platformName = "Commuter Rail - Track 2",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rugg",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2265-03",
            latitude = 42.336368,
            longitude = -71.089554,
            name = "Ruggles",
            locationType = LocationType.STOP,
            description = "Ruggles - Commuter Rail - Track 3",
            platformCode = "3",
            platformName = "Commuter Rail - Track 3",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-rugg",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2276",
            latitude = 42.34735,
            longitude = -71.075727,
            name = "Back Bay",
            locationType = LocationType.STOP,
            description = "Back Bay - Commuter Rail",
            platformCode = null,
            platformName = "Commuter Rail",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bbsta",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2276-01",
            latitude = 42.347283,
            longitude = -71.075312,
            name = "Back Bay",
            locationType = LocationType.STOP,
            description = "Back Bay - Commuter Rail - Track 1",
            platformCode = "1",
            platformName = "Commuter Rail - Track 1",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bbsta",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2276-02",
            latitude = 42.347196,
            longitude = -71.075299,
            name = "Back Bay",
            locationType = LocationType.STOP,
            description = "Back Bay - Commuter Rail - Track 2",
            platformCode = "2",
            platformName = "Commuter Rail - Track 2",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bbsta",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2276-03",
            latitude = 42.347283,
            longitude = -71.075312,
            name = "Back Bay",
            locationType = LocationType.STOP,
            description = "Back Bay - Commuter Rail - Track 3",
            platformCode = "3",
            platformName = "Commuter Rail - Track 3",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bbsta",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2276-B",
            latitude = 42.347497,
            longitude = -71.075894,
            name = "Back Bay",
            locationType = LocationType.STOP,
            description = "Back Bay - Commuter Rail Shuttle",
            platformCode = null,
            platformName = "Commuter Rail Shuttle",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bbsta",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2287",
            latitude = 42.35141,
            longitude = -71.055417,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Commuter Rail",
            platformCode = null,
            platformName = "Commuter Rail",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2287-01",
            latitude = 42.351302,
            longitude = -71.05571,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Commuter Rail - Track 1",
            platformCode = "1",
            platformName = "Commuter Rail - Track 1",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2287-02",
            latitude = 42.351261,
            longitude = -71.055552,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Commuter Rail - Track 2",
            platformCode = "2",
            platformName = "Commuter Rail - Track 2",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2287-03",
            latitude = 42.351261,
            longitude = -71.055552,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Commuter Rail - Track 3",
            platformCode = "3",
            platformName = "Commuter Rail - Track 3",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2287-04",
            latitude = 42.35122,
            longitude = -71.055396,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Commuter Rail - Track 4",
            platformCode = "4",
            platformName = "Commuter Rail - Track 4",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2287-05",
            latitude = 42.35122,
            longitude = -71.055396,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Commuter Rail - Track 5",
            platformCode = "5",
            platformName = "Commuter Rail - Track 5",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2287-06",
            latitude = 42.351178,
            longitude = -71.055238,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Commuter Rail - Track 6",
            platformCode = "6",
            platformName = "Commuter Rail - Track 6",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2287-07",
            latitude = 42.351178,
            longitude = -71.055238,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Commuter Rail - Track 7",
            platformCode = "7",
            platformName = "Commuter Rail - Track 7",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2287-08",
            latitude = 42.351136,
            longitude = -71.055081,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Commuter Rail - Track 8",
            platformCode = "8",
            platformName = "Commuter Rail - Track 8",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2287-09",
            latitude = 42.351136,
            longitude = -71.055081,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Commuter Rail - Track 9",
            platformCode = "9",
            platformName = "Commuter Rail - Track 9",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2287-10",
            latitude = 42.351034,
            longitude = -71.054958,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Commuter Rail - Track 10",
            platformCode = "10",
            platformName = "Commuter Rail - Track 10",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2287-11",
            latitude = 42.351034,
            longitude = -71.054958,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Commuter Rail - Track 11",
            platformCode = "11",
            platformName = "Commuter Rail - Track 11",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2287-12",
            latitude = 42.350742,
            longitude = -71.05493,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Commuter Rail - Track 12",
            platformCode = "12",
            platformName = "Commuter Rail - Track 12",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2287-13",
            latitude = 42.350742,
            longitude = -71.05493,
            name = "South Station",
            locationType = LocationType.STOP,
            description = "South Station - Commuter Rail - Track 13",
            platformCode = "13",
            platformName = "Commuter Rail - Track 13",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "NEC-2287-B",
            latitude = 42.352293,
            longitude = -71.055529,
            name = "South Station - Atlantic Ave @ Summer St",
            locationType = LocationType.STOP,
            description = "South Station - Commuter Rail Shuttle - Atlantic Ave @ Summer St",
            platformCode = null,
            platformName = "Commuter Rail Shuttle",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-sstat",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "Oak Grove-01",
            latitude = 42.437735,
            longitude = -71.070837,
            name = "Oak Grove",
            locationType = LocationType.STOP,
            description = "Oak Grove - Orange Line - Track 1",
            platformCode = "1",
            platformName = "Track 1",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-ogmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "Oak Grove-02",
            latitude = 42.437727,
            longitude = -71.070905,
            name = "Oak Grove",
            locationType = LocationType.STOP,
            description = "Oak Grove - Orange Line - Track 2",
            platformCode = "2",
            platformName = "Track 2",
            vehicleType = RouteType.HEAVY_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-ogmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "Union Square-01",
            latitude = 42.377024,
            longitude = -71.093964,
            name = "Union Square",
            locationType = LocationType.STOP,
            description = "Union Square - Green Line - Track 1",
            platformCode = "1",
            platformName = "Track 1",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-unsqu",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "Union Square-02",
            latitude = 42.37699,
            longitude = -71.093993,
            name = "Union Square",
            locationType = LocationType.STOP,
            description = "Union Square - Green Line - Track 2",
            platformCode = "2",
            platformName = "Track 2",
            vehicleType = RouteType.LIGHT_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-unsqu",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "WML-0012-05",
            latitude = 42.34759,
            longitude = -71.075393,
            name = "Back Bay",
            locationType = LocationType.STOP,
            description = "Back Bay - Commuter Rail - Track 5",
            platformCode = "5",
            platformName = "Commuter Rail - Track 5",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bbsta",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "WML-0012-07",
            latitude = 42.34759,
            longitude = -71.075393,
            name = "Back Bay",
            locationType = LocationType.STOP,
            description = "Back Bay - Commuter Rail - Track 7",
            platformCode = "7",
            platformName = "Commuter Rail - Track 7",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-bbsta",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "WR-0045-S",
            latitude = 42.426632,
            longitude = -71.07411,
            name = "Malden Center",
            locationType = LocationType.STOP,
            description = "Malden Center - Commuter Rail - Track 1 (All Trains)",
            platformCode = "1",
            platformName = "Track 1 (All Trains)",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-mlmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "WR-0053-S",
            latitude = 42.437731,
            longitude = -71.070705,
            name = "Oak Grove",
            locationType = LocationType.STOP,
            description = "Oak Grove - Commuter Rail - Track 1 (All Trains)",
            platformCode = "1",
            platformName = "Commuter Rail - Track 1 (All Trains)",
            vehicleType = RouteType.COMMUTER_RAIL,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-ogmnl",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-alfcl",
            latitude = 42.39583,
            longitude = -71.141287,
            name = "Alewife",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "14112",
                    "14118",
                    "14120",
                    "14121",
                    "14122",
                    "14123",
                    "70061",
                    "9070061",
                    "Alewife-01",
                    "Alewife-02",
                    "door-alfcl-alewife",
                    "door-alfcl-alewifes",
                    "door-alfcl-cambridgepark",
                    "door-alfcl-pathbusway",
                    "door-alfcl-russell",
                    "door-alfcl-steel",
                    "node-349-bottom",
                    "node-349-top",
                    "node-350-bottom",
                    "node-350-top",
                    "node-351-bottom",
                    "node-351-top",
                    "node-352-bottom",
                    "node-352-top",
                    "node-353-bottom",
                    "node-353-top",
                    "node-354-bottom",
                    "node-354-top",
                    "node-355-bottom",
                    "node-355-top",
                    "node-356-bottom",
                    "node-356-top",
                    "node-357-bottom",
                    "node-357-top",
                    "node-813-bottom",
                    "node-813-top",
                    "node-814-concourse",
                    "node-814-fifth",
                    "node-814-fourth",
                    "node-814-ground",
                    "node-814-second",
                    "node-814-third",
                    "node-815-concourse",
                    "node-815-fifth",
                    "node-815-fourth",
                    "node-815-ground",
                    "node-815-second",
                    "node-815-third",
                    "node-961-bottom",
                    "node-961-top",
                    "node-alfcl-buswaynorthstairs-bottom",
                    "node-alfcl-buswaynorthstairs-top",
                    "node-alfcl-buswayplatform-middle",
                    "node-alfcl-buswayramp-bottom",
                    "node-alfcl-buswayramp-landing",
                    "node-alfcl-buswayramp-top",
                    "node-alfcl-buswayrampstairs-top",
                    "node-alfcl-buswaysouthstairs-bottom",
                    "node-alfcl-buswaysouthstairs-top",
                    "node-alfcl-cambridgesidepickstairs-top",
                    "node-alfcl-cambridgesideramp-bottom",
                    "node-alfcl-cambridgesideramp-landing",
                    "node-alfcl-cambridgesideramp-top",
                    "node-alfcl-eaststairs-bottom",
                    "node-alfcl-eaststairs-top",
                    "node-alfcl-egaragestairs-fifth",
                    "node-alfcl-egaragestairs-fourth",
                    "node-alfcl-egaragestairs-second",
                    "node-alfcl-egaragestairs-third",
                    "node-alfcl-fifth",
                    "node-alfcl-fourth",
                    "node-alfcl-groundparkingstairs-bottom",
                    "node-alfcl-groundparkingstairs-top",
                    "node-alfcl-main-farepaid",
                    "node-alfcl-main-fareunpaid",
                    "node-alfcl-mainstairs-concourse",
                    "node-alfcl-mainstairs-ground",
                    "node-alfcl-mainstairs-second",
                    "node-alfcl-pick",
                    "node-alfcl-pickstairs-bottom",
                    "node-alfcl-pickstairs-top",
                    "node-alfcl-russell-farepaid",
                    "node-alfcl-russell-fareunpaid",
                    "node-alfcl-russellstairs-bottom",
                    "node-alfcl-russellstairs-top",
                    "node-alfcl-second",
                    "node-alfcl-steelplramp-bottom",
                    "node-alfcl-steelplramp-landing",
                    "node-alfcl-steelplramp-top",
                    "node-alfcl-steelplstairs-top",
                    "node-alfcl-third",
                    "node-alfcl-weststairs-bottom",
                    "node-alfcl-weststairs-top",
                    "node-alfcl-wgaragestairs-concourse",
                    "node-alfcl-wgaragestairs-fifth",
                    "node-alfcl-wgaragestairs-fourth",
                    "node-alfcl-wgaragestairs-ground",
                    "node-alfcl-wgaragestairs-second",
                    "node-alfcl-wgaragestairs-third",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-alsgr",
            latitude = 42.348701,
            longitude = -71.137955,
            name = "Allston Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70126", "70127"),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-amory",
            latitude = 42.350992,
            longitude = -71.114748,
            name = "Amory Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "170140",
                    "170141",
                    "door-amory-ebamory",
                    "door-amory-ebbuick",
                    "door-amory-wbamory",
                    "door-amory-wbbuick",
                ),
            connectingStopIds = listOf<String>("956"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-andrw",
            latitude = 42.330154,
            longitude = -71.057655,
            name = "Andrew",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "13",
                    "70083",
                    "70084",
                    "door-andrw-busway",
                    "door-andrw-dotave",
                    "door-andrw-elevator",
                    "door-andrw-exitonly",
                    "node-123-platform",
                    "node-124-platform",
                    "node-301-platform",
                    "node-872-lobby",
                    "node-879-lobby",
                    "node-879-platform",
                    "node-880-lobby",
                    "node-880-platform",
                    "node-andrw-dotstairs-lobby",
                    "node-andrw-farepaid",
                    "node-andrw-fareunpaid",
                    "node-andrw-ibramp-top",
                    "node-andrw-nbusstairs-lobby",
                    "node-andrw-nibstairs-lobby",
                    "node-andrw-nibstairs-platform",
                    "node-andrw-nlobbycenter",
                    "node-andrw-nobstairs-lobby",
                    "node-andrw-nobstairs-platform",
                    "node-andrw-obelstairs-lobby",
                    "node-andrw-sbusstairs-lobby",
                    "node-andrw-sibingress",
                    "node-andrw-sibstairs-platform",
                    "node-andrw-slobbycenter",
                    "node-andrw-sobstairs-lobby",
                    "node-andrw-sobstairs-platform",
                ),
            connectingStopIds = listOf<String>("9070083", "9170084"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-aqucl",
            latitude = 42.359784,
            longitude = -71.051652,
            name = "Aquarium",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70043",
                    "70044",
                    "door-aqucl-atlantic",
                    "door-aqucl-atlanticelev",
                    "door-aqucl-mcknorth",
                    "door-aqucl-mcksouth",
                    "node-394-lobby",
                    "node-394-platform",
                    "node-395-lobby",
                    "node-395-platform",
                    "node-396-landing",
                    "node-396-lobby",
                    "node-397-landing",
                    "node-397-street",
                    "node-405-lobby",
                    "node-405-street",
                    "node-406-lobby",
                    "node-406-street",
                    "node-913-lobby",
                    "node-913-platform",
                    "node-914-lobby",
                    "node-914-platform",
                    "node-915-lobby",
                    "node-923-lobby",
                    "node-923-platform",
                    "node-924-lobby",
                    "node-924-platform",
                    "node-925-lobby",
                    "node-aqucl-atlanticstairs-lobby",
                    "node-aqucl-atlanticstairs-street",
                    "node-aqucl-atlfarepaid",
                    "node-aqucl-atlfareunpaid",
                    "node-aqucl-ebatlanticstairs-lobby",
                    "node-aqucl-ebatlanticstairs-platform",
                    "node-aqucl-ebstatestairs-lobby",
                    "node-aqucl-ebstatestairs-platform",
                    "node-aqucl-nstatestairs-landing",
                    "node-aqucl-nstatestairs-landingtop",
                    "node-aqucl-nstatestairs-lobby",
                    "node-aqucl-nstatestairs-street",
                    "node-aqucl-sstatestairs-lobby",
                    "node-aqucl-statefarepaid",
                    "node-aqucl-statefareunpaid",
                    "node-aqucl-wbatlanticstairs-lobby",
                    "node-aqucl-wbatlanticstairs-platform",
                    "node-aqucl-wbstatestairs-lobby",
                    "node-aqucl-wbstatestairs-platform",
                ),
            connectingStopIds =
                listOf<String>(
                    "224",
                    "9070043",
                    "Boat-Long-South",
                    "236",
                    "9170043",
                    "Boat-Long",
                    "Boat-Aquarium",
                ),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-armnl",
            latitude = 42.351902,
            longitude = -71.070893,
            name = "Arlington",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70156",
                    "70157",
                    "door-armnl-arlte",
                    "door-armnl-arltw",
                    "door-armnl-elevator",
                    "door-armnl-garden",
                    "node-435-lobby",
                    "node-435-platform",
                    "node-436-lobby",
                    "node-436-platform",
                    "node-962-lobby",
                    "node-962-platform",
                    "node-963-lobby",
                    "node-963-platform",
                    "node-964-lobby",
                    "node-armnl-ebstairs-lobby",
                    "node-armnl-ebstairs-platform",
                    "node-armnl-eescstairs-escalator",
                    "node-armnl-eescstairs-lobby",
                    "node-armnl-farepaid",
                    "node-armnl-fareunpaid",
                    "node-armnl-nstairs-lobby",
                    "node-armnl-nwstairs-lobby",
                    "node-armnl-sestairs-lobby",
                    "node-armnl-swstairs-lobby",
                    "node-armnl-wbstairs-lobby",
                    "node-armnl-wbstairs-platform",
                    "node-armnl-wescstairs-escalator",
                    "node-armnl-wescstairs-lobby",
                ),
            connectingStopIds = listOf<String>("9070156", "145", "177"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-asmnl",
            latitude = 42.28452,
            longitude = -71.063777,
            name = "Ashmont",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "334",
                    "70093",
                    "70094",
                    "70261",
                    "door-asmnl-beale",
                    "door-asmnl-matpn",
                    "door-asmnl-peabody",
                    "door-asmnl-radford",
                    "door-asmnl-rdexits",
                    "node-437-lobby",
                    "node-437-platform",
                    "node-438-lobby",
                    "node-438-platform",
                    "node-968-lobby",
                    "node-968-platform",
                    "node-969-lobby",
                    "node-969-platform",
                    "node-970-lobby",
                    "node-970-platform",
                    "node-asmnl-bealeramp",
                    "node-asmnl-mtpfarepaid",
                    "node-asmnl-mtpfareunpaid",
                    "node-asmnl-mtpnbstair-lobby",
                    "node-asmnl-mtpnbstair-platform",
                    "node-asmnl-peafarepaid",
                    "node-asmnl-peafareunpaid",
                    "node-asmnl-peanbstair-lobby",
                    "node-asmnl-peanbstair-platform",
                    "node-asmnl-peasbstair-lobby",
                    "node-asmnl-peasbstair-platform",
                    "node-asmnl-radfarepaid",
                    "node-asmnl-radfareunpaid",
                    "node-asmnl-radfordramp",
                    "node-asmnl-rdexstair-lobby",
                    "node-asmnl-rdexstair-platform",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-astao",
            latitude = 42.392811,
            longitude = -71.077257,
            name = "Assembly",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70278",
                    "70279",
                    "door-astao-foley",
                    "door-astao-revol",
                    "node-astao-442-ground",
                    "node-astao-442-lobby",
                    "node-astao-443-lobby",
                    "node-astao-443-platform",
                    "node-astao-716-ground",
                    "node-astao-716-lobby",
                    "node-astao-717-lobby",
                    "node-astao-717-platform",
                    "node-astao-718-ground",
                    "node-astao-718-lobby",
                    "node-astao-719-lobby",
                    "node-astao-719-platform",
                    "node-astao-fol-farepaid",
                    "node-astao-fol-fareunpaid",
                    "node-astao-foldoorstair-ground",
                    "node-astao-foldoorstair-lobby",
                    "node-astao-folplatstair-lobby",
                    "node-astao-folplatstair-platform",
                    "node-astao-rev-farepaid",
                    "node-astao-rev-fareunpaid",
                    "node-astao-revdoorstair-ground",
                    "node-astao-revdoorstair-lobby",
                    "node-astao-revplatstair-lobby",
                    "node-astao-revplatstair-platform",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-babck",
            latitude = 42.351616,
            longitude = -71.119924,
            name = "Babcock Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "170136",
                    "170137",
                    "door-babck-ebagganis",
                    "door-babck-ebbabcock",
                    "door-babck-wbagganis",
                    "door-babck-wbbabcock",
                ),
            connectingStopIds = listOf<String>("933", "958", "934"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-balsq",
            latitude = 42.399889,
            longitude = -71.111003,
            name = "Ball Square",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70509",
                    "70510",
                    "door-balsq-boston",
                    "door-balsq-broadway",
                    "node-769-bottom",
                    "node-769-top",
                    "node-balsq-ramp-bottom",
                    "node-balsq-ramp-top",
                    "node-balsq-stairs-bottom",
                    "node-balsq-stairs-top",
                ),
            connectingStopIds = listOf<String>("9070509", "2697", "2736"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-bbsta",
            latitude = 42.34735,
            longitude = -71.075727,
            name = "Back Bay",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "23391",
                    "70014",
                    "70015",
                    "NEC-2276",
                    "NEC-2276-01",
                    "NEC-2276-02",
                    "NEC-2276-03",
                    "NEC-2276-B",
                    "WML-0012-05",
                    "WML-0012-07",
                    "door-bbsta-buswayn",
                    "door-bbsta-busways",
                    "door-bbsta-clarcolum",
                    "door-bbsta-clarendon1",
                    "door-bbsta-clarendon2",
                    "door-bbsta-dartmn",
                    "door-bbsta-dartms",
                    "door-bbsta-dartmw",
                    "door-bbsta-garage",
                    "door-bbsta-underpass",
                    "node-140-lobby",
                    "node-140-platform",
                    "node-141-lobby",
                    "node-141-platform",
                    "node-142-lobby",
                    "node-142-platform",
                    "node-143-lobby",
                    "node-143-platform",
                    "node-144-lobby",
                    "node-144-platform",
                    "node-853-lobby",
                    "node-853-platform",
                    "node-854-lobby",
                    "node-854-platform",
                    "node-855-lobby",
                    "node-855-platform",
                    "node-856-lobby",
                    "node-856-platform",
                    "node-bbsta-13clarexit-bottom",
                    "node-bbsta-13clarexit-farepaid",
                    "node-bbsta-13clarexit-fareunpaid",
                    "node-bbsta-13stairs-lobby",
                    "node-bbsta-13stairs-platform",
                    "node-bbsta-2clarexit-bottom",
                    "node-bbsta-2clarexit-farepaid",
                    "node-bbsta-2clarexit-fareunpaid",
                    "node-bbsta-2stairs-lobby",
                    "node-bbsta-2stairs-platform",
                    "node-bbsta-clar-farepaid",
                    "node-bbsta-clar-fareunpaid",
                    "node-bbsta-dartexit-farepaid",
                    "node-bbsta-dartexit-fareunpaid",
                    "node-bbsta-dartexit-platform",
                    "node-bbsta-e57stairs-lobby",
                    "node-bbsta-e57stairs-platform",
                    "node-bbsta-lower-farepaid",
                    "node-bbsta-lower-fareunpaid",
                    "node-bbsta-olclarexit-bottom",
                    "node-bbsta-olestairs-lobby",
                    "node-bbsta-olestairs-platform",
                    "node-bbsta-olwstairs-lobby",
                    "node-bbsta-olwstairs-platform",
                    "node-bbsta-upassestairs-bottom",
                    "node-bbsta-upassestairs-top",
                    "node-bbsta-upassmezstairs-top",
                    "node-bbsta-upasswstairs-bottom",
                    "node-bbsta-upper-farepaid",
                    "node-bbsta-upper-fareunpaid",
                    "node-bbsta-w57stairs-lobby",
                    "node-bbsta-w57stairs-platform",
                ),
            connectingStopIds = listOf<String>("11384", "176", "9270014", "71855", "34585"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-bckhl",
            latitude = 42.330139,
            longitude = -71.111313,
            name = "Back of the Hill",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70257", "70258"),
            connectingStopIds = listOf<String>("65741", "22365"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-bcnfd",
            latitude = 42.335765,
            longitude = -71.140455,
            name = "Beaconsfield",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70176",
                    "70177",
                    "door-bcnfd-bcnfield",
                    "door-bcnfd-clark",
                    "door-bcnfd-dean",
                    "door-bcnfd-lot",
                    "node-bcnfd-stairs-platform",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-bcnwa",
            latitude = 42.339394,
            longitude = -71.13533,
            name = "Washington Square",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70229", "70230"),
            connectingStopIds = listOf<String>("1292", "1276", "9070229"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-bland",
            latitude = 42.349293,
            longitude = -71.100258,
            name = "Blandford Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70148", "70149"),
            connectingStopIds = listOf<String>("951", "941"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-bndhl",
            latitude = 42.340023,
            longitude = -71.129082,
            name = "Brandon Hall",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70225", "70226"),
            connectingStopIds = listOf<String>("9070226", "9070225"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-boyls",
            latitude = 42.35302,
            longitude = -71.06459,
            name = "Boylston",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70158",
                    "70159",
                    "door-boyls-inbound",
                    "door-boyls-outbound",
                    "node-boyls-in-farepaid",
                    "node-boyls-in-fareunpaid",
                    "node-boyls-instair-platform",
                    "node-boyls-out-farepaid",
                    "node-boyls-out-fareunpaid",
                    "node-boyls-outstair-platform",
                ),
            connectingStopIds = listOf<String>("8279"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-brdwy",
            latitude = 42.342622,
            longitude = -71.056967,
            name = "Broadway",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70081",
                    "70082",
                    "door-brdwy-main",
                    "door-brdwy-traveler",
                    "node-367-lobby",
                    "node-367-platform",
                    "node-372-lobby",
                    "node-867-lobby",
                    "node-867-platform",
                    "node-868-lobby",
                    "node-brdwy-farepaid",
                    "node-brdwy-fareunpaid",
                    "node-brdwy-mainstairs-lobby",
                    "node-brdwy-platstairs-lobby",
                    "node-brdwy-platstairs-platform",
                    "node-brdwy-travelerstairs-lobby",
                ),
            connectingStopIds = listOf<String>("151", "150"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-brico",
            latitude = 42.351967,
            longitude = -71.125031,
            name = "Packard's Corner",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70134", "70135"),
            connectingStopIds = listOf<String>("9070135", "960", "9070134"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-brkhl",
            latitude = 42.331316,
            longitude = -71.126683,
            name = "Brookline Hills",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70178",
                    "70179",
                    "door-brkhl-bhs",
                    "door-brkhl-cypress",
                    "node-brkhl-eastramp-top",
                ),
            connectingStopIds = listOf<String>("9170178"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-brmnl",
            latitude = 42.334229,
            longitude = -71.104609,
            name = "Brigham Circle",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70249", "70250"),
            connectingStopIds = listOf<String>("1362", "1319", "21317", "92391"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-brntn",
            latitude = 42.207854,
            longitude = -71.001138,
            name = "Braintree",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "38671",
                    "70105",
                    "Braintree-01",
                    "Braintree-02",
                    "MM-0109",
                    "MM-0109-CS",
                    "MM-0109-S",
                    "door-brntn-busway",
                    "door-brntn-faregates",
                    "node-333-bottom",
                    "node-333-top",
                    "node-334-lobby",
                    "node-334-platform",
                    "node-335-lobby",
                    "node-335-platform",
                    "node-811-lobby",
                    "node-811-platform",
                    "node-brntn-crramp-bottom",
                    "node-brntn-crramp-top",
                    "node-brntn-crstairs-bottom",
                    "node-brntn-crstairs-top",
                    "node-brntn-farepaid",
                    "node-brntn-fareunpaid",
                    "node-brntn-nplatstairs-lobby",
                    "node-brntn-nplatstairs-platform",
                    "node-brntn-parking",
                    "node-brntn-splatstairs-lobby",
                    "node-brntn-splatstairs-platform",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-bucen",
            latitude = 42.350082,
            longitude = -71.106865,
            name = "Boston University Central",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70144", "70145"),
            connectingStopIds = listOf<String>("953", "938"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-buest",
            latitude = 42.349735,
            longitude = -71.103889,
            name = "Boston University East",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70146", "70147"),
            connectingStopIds = listOf<String>("952", "939"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-bvmnl",
            latitude = 42.332608,
            longitude = -71.116857,
            name = "Brookline Village",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70180",
                    "70181",
                    "door-bvmnl-boylston",
                    "door-bvmnl-kent",
                    "door-bvmnl-place",
                    "door-bvmnl-washington",
                    "node-bvmnl-stairs-platform",
                ),
            connectingStopIds = listOf<String>("11366", "9070180", "1526", "1555"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-ccmnl",
            latitude = 42.373622,
            longitude = -71.069533,
            name = "Community College",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70028",
                    "70029",
                    "door-ccmnl-main",
                    "node-ccmnl-305-lobby",
                    "node-ccmnl-305-platform",
                    "node-ccmnl-933-lobby",
                    "node-ccmnl-933-platform",
                    "node-ccmnl-farepaid",
                    "node-ccmnl-fareunpaid",
                    "node-ccmnl-lobby",
                    "node-ccmnl-stairs-lobby",
                    "node-ccmnl-stairs-platform",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-chhil",
            latitude = 42.326753,
            longitude = -71.164699,
            name = "Chestnut Hill",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70172",
                    "70173",
                    "door-chhil-bc",
                    "door-chhil-museum",
                    "door-chhil-parke",
                    "door-chhil-parkw",
                    "node-chhil-parkstairs-platform",
                    "node-chhil-roadstairs-platform",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-chill",
            latitude = 42.338169,
            longitude = -71.15316,
            name = "Chestnut Hill Avenue",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70112", "70113"),
            connectingStopIds = listOf<String>("1029", "1090", "9070113", "9070112"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-chmnl",
            latitude = 42.361166,
            longitude = -71.070628,
            name = "Charles/MGH",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70073",
                    "70074",
                    "door-chmnl-main",
                    "node-425-lobby",
                    "node-425-platform",
                    "node-426-lobby",
                    "node-426-platform",
                    "node-951-lobby",
                    "node-951-platform",
                    "node-952-lobby",
                    "node-952-platform",
                    "node-chmnl-farepaid",
                    "node-chmnl-fareunpaid",
                    "node-chmnl-nbstairs-lobby",
                    "node-chmnl-nbstairs-platform",
                    "node-chmnl-sbstairs-lobby",
                    "node-chmnl-sbstairs-platform",
                ),
            connectingStopIds = listOf<String>("9070074"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-chncl",
            latitude = 42.352547,
            longitude = -71.062752,
            name = "Chinatown",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70018",
                    "70019",
                    "door-chncl-fhelevator",
                    "door-chncl-foresthills",
                    "door-chncl-ogelevator",
                    "door-chncl-ogessex",
                    "node-366-landing",
                    "node-366-lobby",
                    "node-377-landing",
                    "node-377-lobby",
                    "node-876-lobby",
                    "node-922-lobby",
                    "node-chncl-fhfarepaid",
                    "node-chncl-fhfareunpaid",
                    "node-chncl-fhlower-landing",
                    "node-chncl-fhlower-lobby",
                    "node-chncl-fhupper-landing",
                    "node-chncl-ogfarepaid",
                    "node-chncl-ogfareunpaid",
                    "node-chncl-oglower-landing",
                    "node-chncl-oglower-lobby",
                    "node-chncl-ogonlystair-lobby",
                    "node-chncl-ogupper-landing",
                ),
            connectingStopIds = listOf<String>("6567", "6537"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-chswk",
            latitude = 42.340805,
            longitude = -71.150711,
            name = "Chiswick Road",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70114", "70115"),
            connectingStopIds = listOf<String>("9070114"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-clmnl",
            latitude = 42.336142,
            longitude = -71.149326,
            name = "Cleveland Circle",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70237", "70238"),
            connectingStopIds = listOf<String>("9070238", "9070237"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-cntsq",
            latitude = 42.365486,
            longitude = -71.103802,
            name = "Central",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70069",
                    "70070",
                    "door-cntsq-essex",
                    "door-cntsq-ibmass",
                    "door-cntsq-obmass",
                    "door-cntsq-pearl",
                    "door-cntsq-prospect",
                    "door-cntsq-western",
                    "node-cntsq-359-platform",
                    "node-cntsq-360-platform",
                    "node-cntsq-860-platform",
                    "node-cntsq-861-platform",
                    "node-cntsq-essexstair-farepaid",
                    "node-cntsq-essexstair-fareunpaid",
                    "node-cntsq-essexstair-platform",
                    "node-cntsq-ibstair-farepaid",
                    "node-cntsq-ibstair-fareunpaid",
                    "node-cntsq-ibstair-platform",
                    "node-cntsq-obstair-farepaid",
                    "node-cntsq-obstair-fareunpaid",
                    "node-cntsq-obstair-platform",
                    "node-cntsq-pearlstair-farepaid",
                    "node-cntsq-pearlstair-fareunpaid",
                    "node-cntsq-pearlstair-platform",
                    "node-cntsq-prospectstair-farepaid",
                    "node-cntsq-prospectstair-fareunpaid",
                    "node-cntsq-prospectstair-platform",
                    "node-cntsq-westernstair-farepaid",
                    "node-cntsq-westernstair-fareunpaid",
                    "node-cntsq-westernstair-platform",
                ),
            connectingStopIds =
                listOf<String>("102", "1060", "1123", "72", "2443", "1059", "9070070", "2755"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-coecl",
            latitude = 42.349974,
            longitude = -71.077447,
            name = "Copley",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70154",
                    "70155",
                    "door-coecl-bpl",
                    "door-coecl-church",
                    "door-coecl-east",
                    "node-coecl-439-platform",
                    "node-coecl-976-platform",
                    "node-coecl-977-platform",
                    "node-coecl-bplstair-platform",
                    "node-coecl-churchstair-platform",
                    "node-coecl-eaststair-platform",
                    "node-coecl-ebfarepaid",
                    "node-coecl-ebfareunpaid",
                    "node-coecl-wbfarepaid",
                    "node-coecl-wbfareunpaid",
                ),
            connectingStopIds = listOf<String>("175", "9170154", "178", "9070154"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-cool",
            latitude = 42.342116,
            longitude = -71.121263,
            name = "Coolidge Corner",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70219", "70220"),
            connectingStopIds = listOf<String>("1372", "9070219", "9070220", "1308"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-davis",
            latitude = 42.39674,
            longitude = -71.121815,
            name = "Davis",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "5104",
                    "70063",
                    "70064",
                    "door-davis-busway",
                    "door-davis-holland",
                    "node-336-entrance",
                    "node-336-middle",
                    "node-337-entrance",
                    "node-337-middle",
                    "node-338-lobby",
                    "node-338-middle",
                    "node-339-lobby",
                    "node-339-middle",
                    "node-340-lobby",
                    "node-340-platform",
                    "node-341-lobby",
                    "node-341-platform",
                    "node-342-entrance",
                    "node-342-lobby",
                    "node-343-entrance",
                    "node-343-lobby",
                    "node-816-lobby",
                    "node-816-platform",
                    "node-817-entrance",
                    "node-817-lobby",
                    "node-davis-340stair-lobby",
                    "node-davis-340stair-platform",
                    "node-davis-341stair-lobby",
                    "node-davis-341stair-platform",
                    "node-davis-colstairlow-lobby",
                    "node-davis-colstairlow-middle",
                    "node-davis-colstairup-entrance",
                    "node-davis-colstairup-middle",
                    "node-davis-farepaid",
                    "node-davis-fareunpaid",
                    "node-davis-holland-entrance",
                    "node-davis-holland-lobby",
                ),
            connectingStopIds = listOf<String>("5015", "2581"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-denrd",
            latitude = 42.337807,
            longitude = -71.141853,
            name = "Dean Road",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70233", "70234"),
            connectingStopIds = listOf<String>("9070233", "9070234"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-dwnxg",
            latitude = 42.355518,
            longitude = -71.060225,
            name = "Downtown Crossing",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70020",
                    "70021",
                    "70077",
                    "70078",
                    "door-dwnxg-101arch",
                    "door-dwnxg-chauncy",
                    "door-dwnxg-franklin",
                    "door-dwnxg-hawley",
                    "door-dwnxg-macys",
                    "door-dwnxg-rochebros",
                    "door-dwnxg-summer",
                    "door-dwnxg-summereast",
                    "door-dwnxg-temple",
                    "door-dwnxg-winter",
                    "node-112-platform",
                    "node-113-platform",
                    "node-331-ol",
                    "node-331-rl",
                    "node-332-ol",
                    "node-332-rl",
                    "node-364-lobby",
                    "node-365-middle",
                    "node-869-lobby",
                    "node-869-platform",
                    "node-870-lobby",
                    "node-870-platform",
                    "node-891-lobby",
                    "node-892-lobby",
                    "node-998-ol",
                    "node-998-rl",
                    "node-999-ol",
                    "node-999-rl",
                    "node-dwnxg-112fare-paid",
                    "node-dwnxg-112fare-unpaid",
                    "node-dwnxg-113fare-paid",
                    "node-dwnxg-113fare-unpaid",
                    "node-dwnxg-concourse",
                    "node-dwnxg-frnkfare-paid",
                    "node-dwnxg-frnkfare-unpaid",
                    "node-dwnxg-macysramp-lobby",
                    "node-dwnxg-macysstairs-lobby",
                    "node-dwnxg-nbsumexitfare-paid",
                    "node-dwnxg-nbsumexitfare-unpaid",
                    "node-dwnxg-sbsumexitfare-paid",
                    "node-dwnxg-sbsumexitfare-unpaid",
                    "node-dwnxg-stair1-ol",
                    "node-dwnxg-stair1-rl",
                    "node-dwnxg-stair10-lobby",
                    "node-dwnxg-stair10-middle",
                    "node-dwnxg-stair11-lobby",
                    "node-dwnxg-stair11-middle",
                    "node-dwnxg-stair12-middle",
                    "node-dwnxg-stair13-lobby",
                    "node-dwnxg-stair14-lobby",
                    "node-dwnxg-stair15-lobby",
                    "node-dwnxg-stair16-lobby",
                    "node-dwnxg-stair2-ol",
                    "node-dwnxg-stair2-rl",
                    "node-dwnxg-stair3-platform",
                    "node-dwnxg-stair4-lobby",
                    "node-dwnxg-stair4-platform",
                    "node-dwnxg-stair5-lobby",
                    "node-dwnxg-stair5-platform",
                    "node-dwnxg-stair6-platform",
                    "node-dwnxg-stair7-ol",
                    "node-dwnxg-stair7-rl",
                    "node-dwnxg-stair8-ol",
                    "node-dwnxg-stair8-rl",
                    "node-dwnxg-stair9-lobby",
                    "node-dwnxg-sumchyfare-paid",
                    "node-dwnxg-sumchyfare-unpaid",
                    "node-dwnxg-sumwashfare-paid",
                    "node-dwnxg-sumwashfare-unpaid",
                    "node-dwnxg-templefare-paid",
                    "node-dwnxg-templefare-unpaid",
                    "node-dwnxg-wintexitfare-paid",
                    "node-dwnxg-wintexitfare-unpaid",
                    "node-dwnxg-wintwashfare-paid",
                    "node-dwnxg-wintwashfare-unpaid",
                ),
            connectingStopIds = listOf<String>("9070080", "16538", "49001", "16535"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-eliot",
            latitude = 42.319045,
            longitude = -71.216684,
            name = "Eliot",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70166",
                    "70167",
                    "door-eliot-boyle",
                    "door-eliot-boylwr",
                    "door-eliot-boylws",
                    "door-eliot-lincoln",
                    "door-eliot-meredith",
                    "node-eliot-boyle-bridge",
                    "node-eliot-boylwr-platform",
                    "node-eliot-boylws-bridge",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-engav",
            latitude = 42.336971,
            longitude = -71.14566,
            name = "Englewood Avenue",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70235", "70236"),
            connectingStopIds = listOf<String>("9070236", "9070235"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-esomr",
            latitude = 42.379467,
            longitude = -71.086625,
            name = "East Somerville",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70513",
                    "70514",
                    "door-esomr-innerbelt",
                    "door-esomr-washington",
                    "node-esomr-innercross-entrance",
                    "node-esomr-innercross-platform",
                    "node-esomr-washcross-platform",
                ),
            connectingStopIds = listOf<String>("2761"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-fbkst",
            latitude = 42.339725,
            longitude = -71.131073,
            name = "Fairbanks Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70227", "70228"),
            connectingStopIds = listOf<String>("9070228", "9070227"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-fenwd",
            latitude = 42.333706,
            longitude = -71.105728,
            name = "Fenwood Road",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70251", "70252"),
            connectingStopIds = listOf<String>("1363", "1317"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-fenwy",
            latitude = 42.345403,
            longitude = -71.104213,
            name = "Fenway",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70186",
                    "70187",
                    "door-fenwy-longwood",
                    "door-fenwy-parkdr",
                    "node-fenwy-stairs-platform",
                ),
            connectingStopIds = listOf<String>("1807", "9434"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-fldcr",
            latitude = 42.300093,
            longitude = -71.061667,
            name = "Fields Corner",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "323",
                    "70089",
                    "70090",
                    "door-fldcr-busway",
                    "door-fldcr-charles",
                    "node-427-lobby",
                    "node-427-platform",
                    "node-957-lobby",
                    "node-957-platform",
                    "node-958-lobby",
                    "node-958-platform",
                    "node-fldcr-alewstair-lobby",
                    "node-fldcr-alewstair-platform",
                    "node-fldcr-ashstair-lobby",
                    "node-fldcr-ashstair-platform",
                    "node-fldcr-charlesramp-lobby",
                    "node-fldcr-charlesstairs-lobby",
                    "node-fldcr-farepaid",
                    "node-fldcr-fareunpaid",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-forhl",
            latitude = 42.300713,
            longitude = -71.113943,
            name = "Forest Hills",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "10642",
                    "70001",
                    "875",
                    "Forest Hills-01",
                    "Forest Hills-02",
                    "NEC-2237",
                    "NEC-2237-03",
                    "NEC-2237-05",
                    "door-forhl-arborway",
                    "door-forhl-lowern",
                    "door-forhl-lowers",
                    "door-forhl-south",
                    "door-forhl-upper",
                    "node-127-busway",
                    "node-127-lobby",
                    "node-128-busway",
                    "node-128-lobby",
                    "node-129-lobby",
                    "node-129-platform",
                    "node-130-lobby",
                    "node-130-platform",
                    "node-724-lobby",
                    "node-724-platform",
                    "node-841-lobby",
                    "node-841-platform",
                    "node-842-lobby",
                    "node-842-platform",
                    "node-843-busway",
                    "node-843-lobby",
                    "node-forhl-busfarepaid",
                    "node-forhl-busfareunpaid",
                    "node-forhl-crstairs-lobby",
                    "node-forhl-crstairs-platform",
                    "node-forhl-lowerdoor",
                    "node-forhl-nlowbusstairs-busway",
                    "node-forhl-nlowbusstairs-lobby",
                    "node-forhl-nplatstairs-lobby",
                    "node-forhl-nplatstairs-platform",
                    "node-forhl-slowbusstairs-busway",
                    "node-forhl-slowbusstairs-lobby",
                    "node-forhl-southfarepaid",
                    "node-forhl-southfareunpaid",
                    "node-forhl-southstairs-lobby",
                    "node-forhl-southstairs-platform",
                    "node-forhl-splatstairs-lobby",
                    "node-forhl-splatstairs-platform",
                    "node-forhl-upperdoor",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-gilmn",
            latitude = 42.387928,
            longitude = -71.096766,
            name = "Gilman Square",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70505",
                    "70506",
                    "door-gilmn-medford",
                    "door-gilmn-school",
                    "node-765-bottom",
                    "node-765-top",
                    "node-766-bottom",
                    "node-766-top",
                    "node-gilmn-medstairs-bottom",
                    "node-gilmn-medstairs-top",
                    "node-gilmn-schstairs-bottom",
                    "node-gilmn-schstairs-top",
                ),
            connectingStopIds = listOf<String>("9070506", "9070505", "2398", "2388"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-gover",
            latitude = 42.359705,
            longitude = -71.059215,
            name = "Government Center",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70039",
                    "70040",
                    "70201",
                    "70202",
                    "Government Center-Brattle",
                    "door-gover-main",
                    "node-444-lobby",
                    "node-444-platform",
                    "node-445-lobby",
                    "node-445-platform",
                    "node-446-bl",
                    "node-446-gl",
                    "node-720-lobby",
                    "node-720-platform",
                    "node-721-lobby",
                    "node-721-platform",
                    "node-722-bl",
                    "node-722-gl",
                    "node-723-bl",
                    "node-723-gl",
                    "node-gover-ebtransstairs-bl",
                    "node-gover-ebtransstairs-gl",
                    "node-gover-farepaid",
                    "node-gover-fareunpaid",
                    "node-gover-nentrancestairs-lobby",
                    "node-gover-nentrancestairs-platform",
                    "node-gover-sentrancestairs-lobby",
                    "node-gover-sentrancestairs-platform",
                    "node-gover-wbtransstairs-bl",
                    "node-gover-wbtransstairs-gl",
                ),
            connectingStopIds = listOf<String>("4510"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-grigg",
            latitude = 42.348545,
            longitude = -71.134949,
            name = "Griggs Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70128", "70129"),
            connectingStopIds = listOf<String>("9070128", "9070129"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-grnst",
            latitude = 42.310525,
            longitude = -71.107414,
            name = "Green Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70002",
                    "70003",
                    "door-grnst-main",
                    "node-grnst-131-lobby",
                    "node-grnst-131-platform",
                    "node-grnst-844-lobby",
                    "node-grnst-844-platform",
                    "node-grnst-farepaid",
                    "node-grnst-fareunpaid",
                    "node-grnst-stairs-lobby",
                    "node-grnst-stairs-platform",
                ),
            connectingStopIds = listOf<String>("9070002", "52371", "52370", "9070003"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-haecl",
            latitude = 42.363021,
            longitude = -71.05829,
            name = "Haymarket",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70024",
                    "70025",
                    "70203",
                    "70204",
                    "door-haecl-busway",
                    "door-haecl-congress",
                    "node-313-lobby",
                    "node-903-lobby",
                    "node-903-passage",
                    "node-903-platform",
                    "node-904-lobby",
                    "node-904-platform",
                    "node-905-passage",
                    "node-905-platform",
                    "node-906-green",
                    "node-906-passage",
                    "node-907-passage",
                    "node-907-platform",
                    "node-908-lobby",
                    "node-haecl-busfarepaid",
                    "node-haecl-busfareunpaid",
                    "node-haecl-busolstair-green",
                    "node-haecl-busolstair-passage",
                    "node-haecl-busstair-lobby",
                    "node-haecl-congfarepaid",
                    "node-haecl-congfareunpaid",
                    "node-haecl-congfhplatn-passage",
                    "node-haecl-congfhplatn-platform",
                    "node-haecl-congfhplats-passage",
                    "node-haecl-congfhplats-platform",
                    "node-haecl-congfhstair-lobby",
                    "node-haecl-congfhstair-passage",
                    "node-haecl-congogplatn-passage",
                    "node-haecl-congogplatn-platform",
                    "node-haecl-congogplats-passage",
                    "node-haecl-congogplats-platform",
                    "node-haecl-congogstairn-lobby",
                    "node-haecl-congogstairn-passage",
                    "node-haecl-congogstairs-lobby",
                    "node-haecl-congogstairs-passage",
                    "node-haecl-glstair-green",
                    "node-haecl-glstair-passage",
                    "node-haecl-passfhstair-passage",
                    "node-haecl-passfhstair-platform",
                    "node-haecl-passogstair-passage",
                    "node-haecl-passogstair-platform",
                ),
            connectingStopIds =
                listOf<String>("30203", "9170024", "4511", "117", "8309", "9070025", "9070024"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-harsq",
            latitude = 42.373362,
            longitude = -71.118956,
            name = "Harvard",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70067",
                    "70068",
                    "76121",
                    "76122",
                    "76123",
                    "76124",
                    "76125",
                    "76126",
                    "76129",
                    "door-harsq-brattle",
                    "door-harsq-church",
                    "door-harsq-square",
                    "door-harsq-yard",
                    "node-512-landing",
                    "node-513-landing",
                    "node-514-landing",
                    "node-514-lobby",
                    "node-515-landing",
                    "node-515-lobby",
                    "node-516-lobby",
                    "node-516-platform",
                    "node-821-lobby",
                    "node-973-upper",
                    "node-harsq-bratstair-upper",
                    "node-harsq-churstair-lobby",
                    "node-harsq-harfarepaid",
                    "node-harsq-harfareunpaid",
                    "node-harsq-harstair-lobby",
                    "node-harsq-harstair-platform",
                    "node-harsq-lowerramp-lobby",
                    "node-harsq-lowerramp-lower",
                    "node-harsq-lowerstair-lobby",
                    "node-harsq-lowerstair-lower",
                    "node-harsq-lowupstair-lower",
                    "node-harsq-lowupstair-upper",
                    "node-harsq-nbramp-platform",
                    "node-harsq-sbramp-platform",
                    "node-harsq-sbstair-nb",
                    "node-harsq-sbstair-sbn",
                    "node-harsq-sbstair-sbs",
                    "node-harsq-sqfarepaid",
                    "node-harsq-sqfareunpaid",
                    "node-harsq-sqhighstair-landing",
                    "node-harsq-sqlowstair-landing",
                    "node-harsq-sqlowstair-lobby",
                    "node-harsq-upperramp-lobby",
                    "node-harsq-upperramp-upper",
                    "node-harsq-yardstair-lobby",
                ),
            connectingStopIds = listOf<String>("9070072", "110", "22549"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-harvd",
            latitude = 42.350243,
            longitude = -71.131355,
            name = "Harvard Avenue",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70130", "70131"),
            connectingStopIds = listOf<String>("9070130", "1302", "1378"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-hsmnl",
            latitude = 42.328316,
            longitude = -71.110252,
            name = "Heath Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70260"),
            connectingStopIds = listOf<String>("1761", "1747", "65741", "22365"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-hwsst",
            latitude = 42.344906,
            longitude = -71.111145,
            name = "Hawes Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70213", "70214"),
            connectingStopIds = listOf<String>("9070214", "9070213"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-hymnl",
            latitude = 42.347888,
            longitude = -71.087903,
            name = "Hynes Convention Center",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70152",
                    "70153",
                    "door-hymnl-mass",
                    "door-hymnl-newbury",
                    "node-315-platform",
                    "node-315-top",
                    "node-hymnl-ebstairs-lobby",
                    "node-hymnl-ebstairs-platform",
                    "node-hymnl-farepaid",
                    "node-hymnl-fareunpaid",
                    "node-hymnl-massentrance-lobby",
                    "node-hymnl-massentrance-street",
                    "node-hymnl-newbury-farepaid",
                    "node-hymnl-newbury-fareunpaid",
                    "node-hymnl-newburyexit",
                    "node-hymnl-wbstairs-lobby",
                    "node-hymnl-wbstairs-midblobby",
                    "node-hymnl-wbstairs-midtlobby",
                    "node-hymnl-wbstairs-platform",
                ),
            connectingStopIds = listOf<String>("93", "79", "11391", "9070152", "9170152"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-jaksn",
            latitude = 42.323132,
            longitude = -71.099592,
            name = "Jackson Square",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "11531",
                    "70006",
                    "70007",
                    "door-jaksn-main",
                    "node-jaksn-133-lobby",
                    "node-jaksn-133-platform",
                    "node-jaksn-846-lobby",
                    "node-jaksn-846-platform",
                    "node-jaksn-farepaid",
                    "node-jaksn-fareunpaid",
                    "node-jaksn-stairs-lobby",
                    "node-jaksn-stairs-platform",
                ),
            connectingStopIds = listOf<String>("9070007", "41157"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-jfk",
            latitude = 42.320685,
            longitude = -71.052391,
            name = "JFK/UMass",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "121",
                    "70085",
                    "70086",
                    "70095",
                    "70096",
                    "MM-0023-S",
                    "door-jfk-busway",
                    "door-jfk-columbia",
                    "door-jfk-oldcolony",
                    "door-jfk-sydneyn",
                    "door-jfk-sydneys",
                    "node-322-lobby",
                    "node-322-platform",
                    "node-830-lobby",
                    "node-830-platform",
                    "node-831-lobby",
                    "node-831-platform",
                    "node-jfk-ashcolfarepaid",
                    "node-jfk-ashcolfareunpaid",
                    "node-jfk-ashcolstair-lobby",
                    "node-jfk-ashcolstair-platform",
                    "node-jfk-ashganstair-lobby",
                    "node-jfk-ashganstair-platform",
                    "node-jfk-ashgasstair-lobby",
                    "node-jfk-ashgasstair-platform",
                    "node-jfk-ashsydstair-lobby",
                    "node-jfk-ashsydstair-platform",
                    "node-jfk-bracolfarepaid",
                    "node-jfk-bracolfareunpaid",
                    "node-jfk-bracolstair-lobby",
                    "node-jfk-bracolstair-platform",
                    "node-jfk-braganstair-lobby",
                    "node-jfk-braganstair-platform",
                    "node-jfk-bragasstair-lobby",
                    "node-jfk-bragasstair-platform",
                    "node-jfk-brasydstair-lobby",
                    "node-jfk-brasydstair-platform",
                    "node-jfk-busramp-lobby",
                    "node-jfk-crcolstair-columbia",
                    "node-jfk-crcolstair-street",
                    "node-jfk-crramp-cr",
                    "node-jfk-crramp-street",
                    "node-jfk-mainfarepaid",
                    "node-jfk-mainfareunpaid",
                    "node-jfk-oldstaire-lobby",
                    "node-jfk-oldstairw-lobby",
                    "node-jfk-sydfarepaid",
                    "node-jfk-sydfareunpaid",
                    "node-jfk-sydneynstair-lobby",
                    "node-jfk-sydneysstair-lobby",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-kencl",
            latitude = 42.348949,
            longitude = -71.095169,
            name = "Kenmore",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70150",
                    "70151",
                    "71150",
                    "71151",
                    "899",
                    "door-kencl-beacon",
                    "door-kencl-busway",
                    "door-kencl-commonwealth",
                    "node-430-landing",
                    "node-431-lobby",
                    "node-431-platform",
                    "node-432-lobby",
                    "node-432-platform",
                    "node-948-lobby",
                    "node-971-lobby",
                    "node-971-platform",
                    "node-972-paidlobby",
                    "node-972-platform",
                    "node-972-unpaidlobby",
                    "node-kencl-beacstair-lobby",
                    "node-kencl-busstair-landing",
                    "node-kencl-commstair-lobby",
                    "node-kencl-ebstair-lobby",
                    "node-kencl-ebstair-platform",
                    "node-kencl-farepaid",
                    "node-kencl-fareunpaid",
                    "node-kencl-fvmramp-landing",
                    "node-kencl-fvmramp-lobby",
                    "node-kencl-fvmstair-landing",
                    "node-kencl-fvmstair-lobby",
                    "node-kencl-wbstair-lobby",
                    "node-kencl-wbstair-platform",
                ),
            connectingStopIds =
                listOf<String>("34510", "9070185", "9170151", "9070150", "9070151", "34509"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-knncl",
            latitude = 42.362491,
            longitude = -71.086176,
            name = "Kendall/MIT",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70071",
                    "70072",
                    "door-knncl-nbmain",
                    "door-knncl-nbsecond",
                    "door-knncl-sbcarleton",
                    "door-knncl-sbelevator",
                    "door-knncl-sbmain",
                    "door-knncl-sbsecond",
                    "node-450-entrance",
                    "node-450-platform",
                    "node-756-entrance",
                    "node-756-platform",
                    "node-757-entrance",
                    "node-757-platform",
                    "node-866-entrance",
                    "node-866-platform",
                    "node-knncl-backnbstair-entrance",
                    "node-knncl-backnbstair-platform",
                    "node-knncl-frontnbstair-entrance",
                    "node-knncl-frontnbstair-platform",
                    "node-knncl-frontsbstair-entrance",
                    "node-knncl-frontsbstair-platform",
                    "node-knncl-nbmain-farepaid",
                    "node-knncl-nbmain-fareunpaid",
                    "node-knncl-nbsec-farepaid",
                    "node-knncl-nbsec-fareunpaid",
                    "node-knncl-sbcarleton-entrance",
                    "node-knncl-sbcarleton-platform",
                    "node-knncl-sbmain-entrance",
                    "node-knncl-sbmain-farepaid",
                    "node-knncl-sbmain-fareunpaid",
                    "node-knncl-sbmain-platform",
                    "node-knncl-sbsec-farepaid",
                    "node-knncl-sbsec-fareunpaid",
                ),
            connectingStopIds = listOf<String>("2231", "9170071", "9070071"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-kntst",
            latitude = 42.344074,
            longitude = -71.114197,
            name = "Kent Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70215", "70216"),
            connectingStopIds = listOf<String>("9070216", "9070215"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-lake",
            latitude = 42.340081,
            longitude = -71.166769,
            name = "Boston College",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70106", "70107"),
            connectingStopIds = listOf<String>("9070107"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-lech",
            latitude = 42.371572,
            longitude = -71.076584,
            name = "Lechmere",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70500",
                    "70501",
                    "70502",
                    "door-lech-busway",
                    "door-lech-first",
                    "door-lech-firstracks",
                    "door-lech-obrien",
                    "node-762-bottom",
                    "node-762-top",
                    "node-763-bottom",
                    "node-763-top",
                    "node-764-bottom",
                    "node-764-top",
                    "node-lech-firststairs-bottom",
                    "node-lech-firststairs-top",
                    "node-lech-obrienstairs-bottom",
                    "node-lech-obrienstairs-top",
                ),
            connectingStopIds = listOf<String>("9070502"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-lngmd",
            latitude = 42.33596,
            longitude = -71.100052,
            name = "Longwood Medical Area",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70247", "70248"),
            connectingStopIds = listOf<String>("91391", "31317"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-longw",
            latitude = 42.341702,
            longitude = -71.109956,
            name = "Longwood",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70182",
                    "70183",
                    "door-longw-colchester",
                    "door-longw-longwood",
                    "door-longw-neckramp",
                    "door-longw-neckstairs",
                    "node-longw-cstairs-platform",
                    "node-longw-eramp-landing",
                    "node-longw-estairs-landing",
                    "node-longw-pramp-landing",
                    "node-longw-pramp-platform",
                    "node-longw-pstairs-landing",
                    "node-longw-pstairs-platform",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-masta",
            latitude = 42.341512,
            longitude = -71.083423,
            name = "Massachusetts Avenue",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70012",
                    "70013",
                    "door-masta-camden",
                    "door-masta-corridor",
                    "door-masta-main",
                    "door-masta-side",
                    "node-masta-139-lobby",
                    "node-masta-139-platform",
                    "node-masta-852-lobby",
                    "node-masta-852-platform",
                    "node-masta-camdstair-platform",
                    "node-masta-camfarepaid",
                    "node-masta-camfareunpaid",
                    "node-masta-farepaid",
                    "node-masta-fareunpaid",
                    "node-masta-mainstair-lobby",
                    "node-masta-mainstair-platform",
                    "node-masta-undernorth-lobby",
                    "node-masta-undernorth-way",
                    "node-masta-undersouth-lobby",
                    "node-masta-undersouth-way",
                    "node-masta-unpaid-lobby",
                    "node-masta-unpramp-exit",
                    "node-masta-unpramp-lobby",
                    "node-masta-unpstair-exit",
                    "node-masta-unpstair-lobby",
                ),
            connectingStopIds = listOf<String>("187", "188"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-mdftf",
            latitude = 42.407975,
            longitude = -71.117044,
            name = "Medford/Tufts",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70511",
                    "70512",
                    "door-mdftf-boston",
                    "node-770-bottom",
                    "node-770-top",
                    "node-772-bottom",
                    "node-772-top",
                    "node-mdftf-stairs-bottom",
                    "node-mdftf-stairs-top",
                ),
            connectingStopIds = listOf<String>("2407"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-mfa",
            latitude = 42.337711,
            longitude = -71.095512,
            name = "Museum of Fine Arts",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70245", "70246"),
            connectingStopIds = listOf<String>("51317", "1799", "71391", "1784"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-mgngl",
            latitude = 42.393682,
            longitude = -71.106388,
            name = "Magoun Square",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70507",
                    "70508",
                    "door-mgngl-lowell",
                    "node-767-bottom",
                    "node-767-top",
                    "node-768-bottom",
                    "node-768-top",
                    "node-mgngl-ramp-bottom",
                    "node-mgngl-ramp-top",
                    "node-mgngl-stairs-bottom",
                    "node-mgngl-stairs-top",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-mispk",
            latitude = 42.333195,
            longitude = -71.109756,
            name = "Mission Park",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70253", "70254"),
            connectingStopIds = listOf<String>("1315", "1365"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-mlmnl",
            latitude = 42.426632,
            longitude = -71.07411,
            name = "Malden Center",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "5072",
                    "5327",
                    "53270",
                    "70034",
                    "70035",
                    "WR-0045-S",
                    "door-mlmnl-commercial",
                    "door-mlmnl-pleasant",
                    "door-mlmnl-ramp",
                    "node-311-lobby",
                    "node-311-platform",
                    "node-312-lobby",
                    "node-312-platform",
                    "node-944-lobby",
                    "node-944-platform",
                    "node-945-lobby",
                    "node-945-platform",
                    "node-mlmnl-commstair-bus",
                    "node-mlmnl-commstair-lobby",
                    "node-mlmnl-crstairs-lobby",
                    "node-mlmnl-crstairs-platform",
                    "node-mlmnl-exitfarepaid",
                    "node-mlmnl-exitfareunpaid",
                    "node-mlmnl-exitstair-lobby",
                    "node-mlmnl-exitstair-platform",
                    "node-mlmnl-farepaid",
                    "node-mlmnl-fareunpaid",
                    "node-mlmnl-olstairs-lobby",
                    "node-mlmnl-olstairs-platform",
                    "node-mlmnl-ramptop",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-newtn",
            latitude = 42.322381,
            longitude = -71.205509,
            name = "Newton Highlands",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70168",
                    "70169",
                    "8155",
                    "8203",
                    "9070168",
                    "door-newtn-hyde",
                    "door-newtn-lake",
                    "door-newtn-walnut",
                    "node-newtn-hyde-platform",
                    "node-newtn-hyde-stairs",
                    "node-newtn-lake-platform",
                    "node-newtn-walnut-platform",
                ),
            connectingStopIds = listOf<String>("9070169", "9170169", "9170168"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-newto",
            latitude = 42.329443,
            longitude = -71.192414,
            name = "Newton Centre",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70170",
                    "70171",
                    "door-newto-braeland",
                    "door-newto-herrick",
                    "door-newto-langley",
                    "door-newto-uniones",
                    "door-newto-unionr",
                    "door-newto-unionws",
                    "node-newto-braeland-platform",
                    "node-newto-herrick-landing",
                    "node-newto-langley-platform",
                    "node-newto-ramp-landing",
                    "node-newto-ramp-platform",
                    "node-newto-uniones-platform",
                    "node-newto-unionws-platform",
                ),
            connectingStopIds = listOf<String>("9070170", "9070171"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-north",
            latitude = 42.365577,
            longitude = -71.06129,
            name = "North Station",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70026",
                    "70027",
                    "70205",
                    "70206",
                    "BNT-0000",
                    "BNT-0000-01",
                    "BNT-0000-02",
                    "BNT-0000-03",
                    "BNT-0000-04",
                    "BNT-0000-05",
                    "BNT-0000-06",
                    "BNT-0000-07",
                    "BNT-0000-08",
                    "BNT-0000-09",
                    "BNT-0000-10",
                    "BNT-0000-B1",
                    "BNT-0000-B2",
                    "door-north-causewaye",
                    "door-north-causewayexite",
                    "door-north-causewayexitw",
                    "door-north-causeways",
                    "door-north-crcanal",
                    "door-north-crcauseway",
                    "door-north-crcausewayw",
                    "door-north-crnashua",
                    "door-north-tdgarden",
                    "door-north-valenti",
                    "door-north-valentiexite",
                    "door-north-valentiexitw",
                    "node-302-bottom",
                    "node-302-top",
                    "node-303-bottom",
                    "node-303-top",
                    "node-304-bottom",
                    "node-304-top",
                    "node-384-bottom",
                    "node-384-top",
                    "node-385-bottom",
                    "node-385-top",
                    "node-391-bottom",
                    "node-391-top",
                    "node-392-bottom",
                    "node-392-top",
                    "node-393-bottom",
                    "node-393-top",
                    "node-447-bottom",
                    "node-447-top",
                    "node-731-bottom",
                    "node-731-top",
                    "node-732-bottom",
                    "node-732-top",
                    "node-909-bottom",
                    "node-909-top",
                    "node-910-bottom",
                    "node-910-top",
                    "node-911-bottom",
                    "node-911-top",
                    "node-912-bottom",
                    "node-912-top",
                    "node-north-causeway-farepaid",
                    "node-north-causeway-fareunpaid",
                    "node-north-causeway-lobby",
                    "node-north-causewayexite-farepaid",
                    "node-north-causewayexite-fareunpaid",
                    "node-north-causewayexitw-farepaid",
                    "node-north-causewayexitw-fareunpaid",
                    "node-north-causewayoakgrove-stairs-bottom",
                    "node-north-causewayoakgrove-stairs-top",
                    "node-north-causewaystairs-bottom",
                    "node-north-causewaystairs-top",
                    "node-north-centerstairs-bottom",
                    "node-north-centerstairs-top",
                    "node-north-cr-lobby",
                    "node-north-crgates-main-farepaid",
                    "node-north-crgates-main-fareunpaid",
                    "node-north-crgates-west-farepaid",
                    "node-north-crgates-west-fareunpaid",
                    "node-north-eastramp-bottom",
                    "node-north-eastramp-top",
                    "node-north-eaststairs-bottom",
                    "node-north-eaststairs-top",
                    "node-north-ecausewayexitstairs-bottom",
                    "node-north-ecausewayexitstairs-top",
                    "node-north-evalentiexitstairs-bottom",
                    "node-north-evalentiexitstairs-top",
                    "node-north-northstairs-bottom",
                    "node-north-northstairs-top",
                    "node-north-passageway-cr",
                    "node-north-passageway-subway",
                    "node-north-passagewaystairs-bottom",
                    "node-north-passagewaystairs-top",
                    "node-north-southstairs-bottom",
                    "node-north-southstairs-top",
                    "node-north-valenti-farepaid",
                    "node-north-valenti-fareunpaid",
                    "node-north-valenti-lobby",
                    "node-north-valenti-streetstairs-bottom",
                    "node-north-valenti-streetstairs-top",
                    "node-north-valentiexite-farepaid",
                    "node-north-valentiexite-fareunpaid",
                    "node-north-valentiexitw-farepaid",
                    "node-north-valentiexitw-fareunpaid",
                    "node-north-valentioakgrove-stairs-bottom",
                    "node-north-valentioakgrove-stairs-top",
                    "node-north-wcausewayexitstairs-bottom",
                    "node-north-wcausewayexitstairs-top",
                    "node-north-wvalentiexitstairs-bottom",
                    "node-north-wvalentiexitstairs-top",
                ),
            connectingStopIds = listOf<String>("9070026", "114", "113", "9070090"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-nqncy",
            latitude = 42.275275,
            longitude = -71.029583,
            name = "North Quincy",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "3125",
                    "70097",
                    "70098",
                    "door-nqncy-hancock",
                    "door-nqncy-newport",
                    "door-nqncy-squantum",
                    "node-325-lobby",
                    "node-325-platform",
                    "node-326-lobby",
                    "node-326-platform",
                    "node-327-lobby",
                    "node-327-street",
                    "node-383-lobby",
                    "node-383-street",
                    "node-897-lobby",
                    "node-897-street",
                    "node-898-lobby",
                    "node-898-street",
                    "node-899-lobby",
                    "node-899-platform",
                    "node-900-lobby",
                    "node-900-platform",
                    "node-nqncy-hstair-lobby",
                    "node-nqncy-hstair-street",
                    "node-nqncy-nhfarepaid",
                    "node-nqncy-nhfareunpaid",
                    "node-nqncy-nhpstair-lobby",
                    "node-nqncy-nhpstair-platform",
                    "node-nqncy-nstair-lobby",
                    "node-nqncy-nstair-street",
                    "node-nqncy-senterramp-bus",
                    "node-nqncy-senterstair-bus",
                    "node-nqncy-sfarepaid",
                    "node-nqncy-sfareunpaid",
                    "node-nqncy-spstair-lobby",
                    "node-nqncy-spstair-platform",
                ),
            connectingStopIds = listOf<String>("9370024"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-nuniv",
            latitude = 42.340401,
            longitude = -71.088806,
            name = "Northeastern University",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70243", "70244"),
            connectingStopIds = listOf<String>("41391", "81317"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-ogmnl",
            latitude = 42.43668,
            longitude = -71.071097,
            name = "Oak Grove",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70036",
                    "9328",
                    "Oak Grove-01",
                    "Oak Grove-02",
                    "WR-0053-S",
                    "door-ogmnl-banks",
                    "door-ogmnl-wash",
                    "door-ogmnl-washel",
                    "node-105-crplatform",
                    "node-105-lobby",
                    "node-451-lobby",
                    "node-451-street",
                    "node-452-lobby",
                    "node-452-street",
                    "node-453-lobby",
                    "node-453-platform",
                    "node-743-lobby",
                    "node-743-platform",
                    "node-744-lobby",
                    "node-745-lobby",
                    "node-745-street",
                    "node-746-lobby",
                    "node-746-platform",
                    "node-ogmnl-banksstair-lobby",
                    "node-ogmnl-banksstair-street",
                    "node-ogmnl-crstair-crplatform",
                    "node-ogmnl-crstair-lobby",
                    "node-ogmnl-farepaid",
                    "node-ogmnl-fareunpaid",
                    "node-ogmnl-olstair-lobby",
                    "node-ogmnl-olstair-platform",
                    "node-ogmnl-washstair-lobby",
                    "node-ogmnl-washstair-street",
                ),
            connectingStopIds = listOf<String>("9326", "5991"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-pktrm",
            latitude = 42.356395,
            longitude = -71.062424,
            name = "Park Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70075",
                    "70076",
                    "70196",
                    "70197",
                    "70198",
                    "70199",
                    "70200",
                    "71199",
                    "door-pktrm-elevatoreb",
                    "door-pktrm-elevatorwb",
                    "door-pktrm-exitalewife",
                    "door-pktrm-exitashbrain",
                    "door-pktrm-tremonte",
                    "door-pktrm-tremontw",
                    "door-pktrm-tremstair",
                    "door-pktrm-west",
                    "door-pktrm-winter",
                    "node-320-lobby",
                    "node-373-platform",
                    "node-375-platform",
                    "node-804-lobby",
                    "node-808-gl",
                    "node-808-rl",
                    "node-812-platform",
                    "node-812-under",
                    "node-823-platform",
                    "node-823-under",
                    "node-978-lobby",
                    "node-979-gl",
                    "node-979-rl",
                    "node-pktrm-804paid",
                    "node-pktrm-804unpaid",
                    "node-pktrm-concourse",
                    "node-pktrm-cpebstairs-gl",
                    "node-pktrm-cpebstairs-rl",
                    "node-pktrm-cpwbstairs-gl",
                    "node-pktrm-cpwbstairs-rl",
                    "node-pktrm-ebparkstairs-lobby",
                    "node-pktrm-ebupassstairs-platform",
                    "node-pktrm-ebupassstairs-under",
                    "node-pktrm-midupassstairs-platform",
                    "node-pktrm-midupassstairs-under",
                    "node-pktrm-nbexitpaid",
                    "node-pktrm-nbexitunpaid",
                    "node-pktrm-nbwinterstairs-lobby",
                    "node-pktrm-nbwinterstairs-platform",
                    "node-pktrm-sbexitpaid",
                    "node-pktrm-sbexitunpaid",
                    "node-pktrm-sbwbstairs-gl",
                    "node-pktrm-sbwbstairs-rl",
                    "node-pktrm-sbwinterstairs-lobby",
                    "node-pktrm-sbwinterstairs-platform",
                    "node-pktrm-stair14-gl",
                    "node-pktrm-stair14-rl",
                    "node-pktrm-stair17-lobby",
                    "node-pktrm-stair4-lobby",
                    "node-pktrm-stair4-platform",
                    "node-pktrm-stair5-gl",
                    "node-pktrm-stair5-rl",
                    "node-pktrm-stair7-gl",
                    "node-pktrm-stair7-rl",
                    "node-pktrm-stair8-gl",
                    "node-pktrm-stair8-rl",
                    "node-pktrm-tremefare-paid",
                    "node-pktrm-tremefare-unpaid",
                    "node-pktrm-tremontstairs-lobby",
                    "node-pktrm-tremstairfare-paid",
                    "node-pktrm-tremstairfare-unpaid",
                    "node-pktrm-tremwfare-paid",
                    "node-pktrm-tremwfare-unpaid",
                    "node-pktrm-wbparkstairs-lobby",
                    "node-pktrm-wbupassstairs-platform",
                    "node-pktrm-wbupassstairs-under",
                    "node-pktrm-westfare-paid",
                    "node-pktrm-westfare-unpaid",
                    "node-pktrm-weststairs-lobby",
                    "node-pktrm-wintfare-paid",
                    "node-pktrm-wintfare-unpaid",
                ),
            connectingStopIds = listOf<String>("9070076", "9170076", "10000", "49001"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-portr",
            latitude = 42.3884,
            longitude = -71.119149,
            name = "Porter",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70065",
                    "70066",
                    "FR-0034",
                    "FR-0034-01",
                    "FR-0034-02",
                    "door-portr-cracross",
                    "door-portr-crtransfer",
                    "door-portr-elevator982",
                    "door-portr-elevator985",
                    "door-portr-escal",
                    "node-504-lobby",
                    "node-504-street",
                    "node-505-lobby",
                    "node-505-street",
                    "node-506-nbplatform",
                    "node-506-sbplatform",
                    "node-507-nbplatform",
                    "node-507-sbplatform",
                    "node-509-lobby",
                    "node-509-sbplatform",
                    "node-510-lobby",
                    "node-510-sbplatform",
                    "node-511-lobby",
                    "node-511-sbplatform",
                    "node-982-lobby",
                    "node-983-lobby",
                    "node-983-nbplatform",
                    "node-983-sbplatform",
                    "node-985-lobby",
                    "node-986-crplatform",
                    "node-986-lobby",
                    "node-987-lobby",
                    "node-987-nbplatform",
                    "node-987-sbplatform",
                    "node-portr-506stairs-nbplatform",
                    "node-portr-506stairs-sbplatform",
                    "node-portr-507stairs-nbplatform",
                    "node-portr-507stairs-sbplatform",
                    "node-portr-crramp-elevator",
                    "node-portr-crramp-minihigh",
                    "node-portr-crstairs-crplatform",
                    "node-portr-crstairs-lobby",
                    "node-portr-eaststairs-crplatform",
                    "node-portr-farepaid",
                    "node-portr-fareunpaid",
                    "node-portr-lobbyrlstairs-lobby",
                    "node-portr-lobbyrlstairs-sbplatform",
                    "node-portr-massavestairs-crplatform",
                    "node-portr-massavestairs-lobby",
                    "node-portr-somervilleavestairs-lobby",
                    "node-portr-somervilleavestairs-street",
                    "node-portr-weststairs-crplatform",
                    "node-portr-weststairs-street",
                ),
            connectingStopIds = listOf<String>("2430", "2460", "12301", "23151", "9070065"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-prmnl",
            latitude = 42.34557,
            longitude = -71.081696,
            name = "Prudential",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70239",
                    "70240",
                    "door-prmnl-huntington",
                    "door-prmnl-prudential",
                    "node-150-lobby",
                    "node-150-street",
                    "node-151-lobby",
                    "node-151-street",
                    "node-152-lobby",
                    "node-152-platform",
                    "node-503-lobby",
                    "node-917-lobby",
                    "node-917-street",
                    "node-920-lobby",
                    "node-920-platform",
                    "node-921-lobby",
                    "node-921-platform",
                    "node-prmnl-152stair-platform",
                    "node-prmnl-920ramp-platform",
                    "node-prmnl-920stair-platform",
                    "node-prmnl-921ramp-platform",
                    "node-prmnl-921stair-platform",
                    "node-prmnl-ebstair-lobby",
                    "node-prmnl-ebstair-platform",
                    "node-prmnl-farepaid",
                    "node-prmnl-fareunpaid",
                    "node-prmnl-huntstair-lobby",
                    "node-prmnl-prustair-lobby",
                    "node-prmnl-prustair-street",
                    "node-prmnl-wbstair-lobby",
                    "node-prmnl-wbstair-platform",
                ),
            connectingStopIds = listOf<String>("11389", "11388"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-qamnl",
            latitude = 42.233391,
            longitude = -71.007153,
            name = "Quincy Adams",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "41031",
                    "70103",
                    "70104",
                    "door-qamnl-burgin",
                    "door-qamnl-centre",
                    "door-qamnl-indep",
                    "door-qamnl-pickup",
                    "node-344-bottom",
                    "node-344-top",
                    "node-345-bottom",
                    "node-345-top",
                    "node-346-bottom",
                    "node-346-top",
                    "node-347-bottom",
                    "node-347-top",
                    "node-348-bottom",
                    "node-348-top",
                    "node-736-bottom",
                    "node-736-top",
                    "node-805-bottom",
                    "node-805-top",
                    "node-806-fifth",
                    "node-806-fourth",
                    "node-806-ground",
                    "node-806-second",
                    "node-806-sixth",
                    "node-806-third",
                    "node-807-fifth",
                    "node-807-fourth",
                    "node-807-ground",
                    "node-807-second",
                    "node-807-sixth",
                    "node-807-third",
                    "node-qamnl-busway-centerdoor",
                    "node-qamnl-busway-northdoor",
                    "node-qamnl-busway-southdoor",
                    "node-qamnl-farepaid",
                    "node-qamnl-fareunpaid",
                    "node-qamnl-fifth-lobby",
                    "node-qamnl-fourth-lobby",
                    "node-qamnl-ngaragestairs-fifth",
                    "node-qamnl-ngaragestairs-fourth",
                    "node-qamnl-ngaragestairs-ground",
                    "node-qamnl-ngaragestairs-second",
                    "node-qamnl-ngaragestairs-sixth",
                    "node-qamnl-ngaragestairs-third",
                    "node-qamnl-northstairs-bottom",
                    "node-qamnl-northstairs-top",
                    "node-qamnl-second-lobby",
                    "node-qamnl-sgaragestairs-fifth",
                    "node-qamnl-sgaragestairs-fourth",
                    "node-qamnl-sgaragestairs-ground",
                    "node-qamnl-sgaragestairs-second",
                    "node-qamnl-sgaragestairs-sixth",
                    "node-qamnl-sgaragestairs-third",
                    "node-qamnl-sixth-lobby",
                    "node-qamnl-southstairs-bottom",
                    "node-qamnl-southstairs-top",
                    "node-qamnl-third-lobby",
                ),
            connectingStopIds = listOf<String>("3852", "3946"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-qnctr",
            latitude = 42.251809,
            longitude = -71.005409,
            name = "Quincy Center",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "32001",
                    "32002",
                    "32003",
                    "32004",
                    "32005",
                    "70101",
                    "70102",
                    "MM-0079-S",
                    "door-qnctr-burgin",
                    "door-qnctr-burginramp",
                    "door-qnctr-busway",
                    "node-330-entrance",
                    "node-330-platform",
                    "node-810-entrance",
                    "node-810-platform",
                    "node-896-entrance",
                    "node-896-platform",
                    "node-qnctr-burgin-door",
                    "node-qnctr-burginramp-cr",
                    "node-qnctr-center-lobby",
                    "node-qnctr-crstair-entrance",
                    "node-qnctr-crstair-platform",
                    "node-qnctr-elevfarepaid",
                    "node-qnctr-elevfareunpaid",
                    "node-qnctr-farepaid",
                    "node-qnctr-fareunpaid",
                    "node-qnctr-rdstair-entrance",
                    "node-qnctr-rdstair-platform",
                ),
            connectingStopIds = listOf<String>("32000", "9070101"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-rcmnl",
            latitude = 42.331397,
            longitude = -71.095451,
            name = "Roxbury Crossing",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70008",
                    "70009",
                    "door-rcmnl-tremont",
                    "node-rcmnl-134-lobby",
                    "node-rcmnl-134-platform",
                    "node-rcmnl-847-lobby",
                    "node-rcmnl-847-platform",
                    "node-rcmnl-farepaid",
                    "node-rcmnl-fareunpaid",
                    "node-rcmnl-stairs-lobby",
                    "node-rcmnl-stairs-platform",
                ),
            connectingStopIds = listOf<String>("1323", "1357", "1258", "1222"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-river",
            latitude = 42.337352,
            longitude = -71.252685,
            name = "Riverside",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "38155",
                    "70160",
                    "70161",
                    "door-river-estairs",
                    "door-river-ramp",
                    "door-river-wstairs",
                    "node-river-estairs-lobby",
                    "node-river-farepaid",
                    "node-river-fareunpaid",
                    "node-river-ramp-lobby",
                    "node-river-wstairs-lobby",
                    "river-B",
                ),
            connectingStopIds = listOf<String>("9070160"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-rsmnl",
            latitude = 42.335088,
            longitude = -71.148758,
            name = "Reservoir",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "21917",
                    "21918",
                    "70174",
                    "70175",
                    "door-rsmnl-eramp",
                    "door-rsmnl-estairs",
                    "door-rsmnl-wramp",
                    "door-rsmnl-wstairs",
                    "node-rsmnl-eramp-platform",
                    "node-rsmnl-estairs-platform",
                    "node-rsmnl-wramp-platform",
                    "node-rsmnl-wstairs-platform",
                ),
            connectingStopIds = listOf<String>("9070238"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-rugg",
            latitude = 42.336377,
            longitude = -71.088961,
            name = "Ruggles",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "17861",
                    "17862",
                    "17863",
                    "70010",
                    "70011",
                    "NEC-2265",
                    "NEC-2265-01",
                    "NEC-2265-02",
                    "NEC-2265-03",
                    "door-rugg-buses",
                    "door-rugg-columbus",
                    "door-rugg-forsyth",
                    "door-rugg-ruggles",
                    "door-rugg-track2",
                    "door-rugg-upper",
                    "node-135-busway",
                    "node-135-lobby",
                    "node-136-lobby",
                    "node-136-platform",
                    "node-137-lobby",
                    "node-137-platform",
                    "node-138-bottom",
                    "node-138-top",
                    "node-728-lobby",
                    "node-728-street",
                    "node-848-busway",
                    "node-848-lobby",
                    "node-849-lobby",
                    "node-849-platform",
                    "node-850-lobby",
                    "node-850-platform",
                    "node-851-bottom",
                    "node-851-top",
                    "node-rugg-bus1stairs-busway",
                    "node-rugg-bus1stairs-lobby",
                    "node-rugg-bus2stairs-busway",
                    "node-rugg-bus2stairs-lobby",
                    "node-rugg-columbusstairs-top",
                    "node-rugg-crstairs-lobby",
                    "node-rugg-crstairs-platform",
                    "node-rugg-farepaid",
                    "node-rugg-fareunpaid",
                    "node-rugg-forsythstairs-bottom",
                    "node-rugg-forsythstairs-top",
                    "node-rugg-olexit-farepaid",
                    "node-rugg-olexit-fareunpaid",
                    "node-rugg-olexit-platform",
                    "node-rugg-olnstairs-lobby",
                    "node-rugg-olnstairs-platform",
                    "node-rugg-olsstairs-lobby",
                    "node-rugg-olsstairs-platform",
                    "node-rugg-track2ramp-bottom",
                    "node-rugg-track2ramp-top",
                ),
            connectingStopIds = listOf<String>("1225", "9070010"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-rvrwy",
            latitude = 42.331684,
            longitude = -71.111931,
            name = "Riverway",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70255", "70256"),
            connectingStopIds = listOf<String>("21365", "6575", "1314"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-sbmnl",
            latitude = 42.317062,
            longitude = -71.104248,
            name = "Stony Brook",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70004",
                    "70005",
                    "door-sbmnl-main",
                    "node-sbmnl-132-lobby",
                    "node-sbmnl-132-platform",
                    "node-sbmnl-845-lobby",
                    "node-sbmnl-845-platform",
                    "node-sbmnl-farepaid",
                    "node-sbmnl-fareunpaid",
                    "node-sbmnl-stairs-lobby",
                    "node-sbmnl-stairs-platform",
                ),
            connectingStopIds = listOf<String>("45237", "9070005"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-shmnl",
            latitude = 42.31129,
            longitude = -71.053331,
            name = "Savin Hill",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70087",
                    "70088",
                    "door-shmnl-savin",
                    "door-shmnl-sydney",
                    "node-423-lobby",
                    "node-423-platform",
                    "node-946-lobby",
                    "node-946-platform",
                    "node-947-lobby",
                    "node-shmnl-endstair-lobby",
                    "node-shmnl-endstair-platform",
                    "node-shmnl-platstair-lobby",
                    "node-shmnl-platstair-platform",
                    "node-shmnl-sh-farepaid",
                    "node-shmnl-sh-fareunpaid",
                    "node-shmnl-syd-farepaid",
                    "node-shmnl-syd-fareunpaid",
                    "node-shmnl-sydstair-lobby",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-smary",
            latitude = 42.345974,
            longitude = -71.107353,
            name = "Saint Mary's Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70211", "70212"),
            connectingStopIds = listOf<String>("9070211", "9070212"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-smmnl",
            latitude = 42.293126,
            longitude = -71.065738,
            name = "Shawmut",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70091",
                    "70092",
                    "door-smmnl-clemen",
                    "node-smmnl-953-lobby",
                    "node-smmnl-953-platform",
                    "node-smmnl-954-lobby",
                    "node-smmnl-954-platform",
                    "node-smmnl-farepaid",
                    "node-smmnl-fareunpaid",
                    "node-smmnl-nbstairs-lobby",
                    "node-smmnl-nbstairs-platform",
                    "node-smmnl-sbstairs-lobby",
                    "node-smmnl-sbstairs-platform",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-sougr",
            latitude = 42.3396,
            longitude = -71.157661,
            name = "South Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70110", "70111"),
            connectingStopIds = listOf<String>("9070111", "9070110"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-spmnl",
            latitude = 42.366664,
            longitude = -71.067666,
            name = "Science Park/West End",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70207",
                    "70208",
                    "door-spmnl-nashua",
                    "door-spmnl-nashuael",
                    "door-spmnl-obrien",
                    "door-spmnl-obrienel",
                    "node-980-lobby",
                    "node-980-platform",
                    "node-980-street",
                    "node-981-lobby",
                    "node-981-platform",
                    "node-981-street",
                    "node-spmnl-ibstairs-lobby",
                    "node-spmnl-ibstairs-platform",
                    "node-spmnl-mfarepaid",
                    "node-spmnl-mfareunpaid",
                    "node-spmnl-nashuastairs-top",
                    "node-spmnl-nfarepaid",
                    "node-spmnl-nfareunpaid",
                    "node-spmnl-obrienstairs-top",
                    "node-spmnl-obstairs-lobby",
                    "node-spmnl-obstairs-platform",
                    "node-spmnl-ofarepaid",
                    "node-spmnl-ofareunpaid",
                    "node-spmnl-sfarepaid",
                    "node-spmnl-sfareunpaid",
                ),
            connectingStopIds = listOf<String>("9070091"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-sstat",
            latitude = 42.352271,
            longitude = -71.055242,
            name = "South Station",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70079",
                    "70080",
                    "74611",
                    "74617",
                    "NEC-2287",
                    "NEC-2287-01",
                    "NEC-2287-02",
                    "NEC-2287-03",
                    "NEC-2287-04",
                    "NEC-2287-05",
                    "NEC-2287-06",
                    "NEC-2287-07",
                    "NEC-2287-08",
                    "NEC-2287-09",
                    "NEC-2287-10",
                    "NEC-2287-11",
                    "NEC-2287-12",
                    "NEC-2287-13",
                    "NEC-2287-B",
                    "door-sstat-atlantic",
                    "door-sstat-bus",
                    "door-sstat-dewey",
                    "door-sstat-finctr",
                    "door-sstat-main",
                    "door-sstat-outmain",
                    "door-sstat-reserve",
                    "door-sstat-summer",
                    "door-sstat-usps",
                    "node-382-lobby",
                    "node-382-rl",
                    "node-386-lobby",
                    "node-387-lobby",
                    "node-388-lobby",
                    "node-388-sl",
                    "node-389-lobby",
                    "node-389-rl",
                    "node-390-rl",
                    "node-390-sl",
                    "node-398-rl",
                    "node-398-sl",
                    "node-399-lobby",
                    "node-399-middle",
                    "node-400-middle",
                    "node-400-rl",
                    "node-411-lobby",
                    "node-411-sl",
                    "node-419-lobby",
                    "node-419-middle",
                    "node-420-middle",
                    "node-420-rl",
                    "node-424-lobby",
                    "node-6476-ground",
                    "node-6476-lobby",
                    "node-901-lobby",
                    "node-901-rl",
                    "node-901-sl",
                    "node-918-lobby",
                    "node-918-rl",
                    "node-918-sl",
                    "node-919-rl",
                    "node-919-sl",
                    "node-926-lobby",
                    "node-927-rl",
                    "node-927-sl",
                    "node-949-lobby",
                    "node-sstat-382stair-lobby",
                    "node-sstat-382stair-rl",
                    "node-sstat-388stair-lobby",
                    "node-sstat-388stair-sl",
                    "node-sstat-389stair-lobby",
                    "node-sstat-389stair-rl",
                    "node-sstat-390stair-rl",
                    "node-sstat-390stair-sl",
                    "node-sstat-398stair-rl",
                    "node-sstat-398stair-sl",
                    "node-sstat-399stair-lobby",
                    "node-sstat-399stair-middle",
                    "node-sstat-400sl-middle",
                    "node-sstat-400sl-sl",
                    "node-sstat-400stair-middle",
                    "node-sstat-400stair-rl",
                    "node-sstat-411stair-lobby",
                    "node-sstat-411stair-sl",
                    "node-sstat-419stair-lobby",
                    "node-sstat-419stair-middle",
                    "node-sstat-420sl-middle",
                    "node-sstat-420sl-sl",
                    "node-sstat-420stair-middle",
                    "node-sstat-420stair-rl",
                    "node-sstat-918stair-lobby",
                    "node-sstat-918stair-sl",
                    "node-sstat-919stair-rl",
                    "node-sstat-919stair-sl",
                    "node-sstat-alewife-farepaid",
                    "node-sstat-alewife-fareunpaid",
                    "node-sstat-ashbrain-farepaid",
                    "node-sstat-ashbrain-fareunpaid",
                    "node-sstat-bldgescdown-ground",
                    "node-sstat-bldgescdown-lobby",
                    "node-sstat-bldgescup-ground",
                    "node-sstat-bldgescup-lobby",
                    "node-sstat-bldgstair-ground",
                    "node-sstat-bldgstair-lobby",
                    "node-sstat-cr-lobby",
                    "node-sstat-deweystair-lobby",
                    "node-sstat-finctrstair-lobby",
                    "node-sstat-north-farepaid",
                    "node-sstat-north-fareunpaid",
                    "node-sstat-outmainstair-lobby",
                    "node-sstat-reservestair-lobby",
                    "node-sstat-south-farepaid",
                    "node-sstat-south-fareunpaid",
                ),
            connectingStopIds = listOf<String>("6564", "892", "6538", "9070079"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-state",
            latitude = 42.358978,
            longitude = -71.057598,
            name = "State",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70022",
                    "70023",
                    "70041",
                    "70042",
                    "door-state-cityhall",
                    "door-state-congress",
                    "door-state-exchange",
                    "door-state-meeting",
                    "door-state-state",
                    "door-state-water",
                    "node-374-lobby",
                    "node-374-nbplatform",
                    "node-501-nbplatform",
                    "node-501-sbplatform",
                    "node-502-landing",
                    "node-502-sbplatform",
                    "node-523-sbplatform",
                    "node-523-street",
                    "node-6-landing",
                    "node-6-nbplatform",
                    "node-802-nbplatform",
                    "node-802-sbplatform",
                    "node-803-sbplatform",
                    "node-803-street",
                    "node-967-nbplatform",
                    "node-967-sbplatform",
                    "node-974-street",
                    "node-974-wbplatform",
                    "node-975-ebplatform",
                    "node-ogcityhallstairs-lobby",
                    "node-ogcityhallstairs-nbplatform",
                    "node-state-bowdoinoldstatestairs-landing",
                    "node-state-bowdoinoldstatestairs-wbplatform",
                    "node-state-bowdoinramp-nbplatform",
                    "node-state-bowdoinramp-wbplatform",
                    "node-state-cityhall-farepaid",
                    "node-state-cityhall-fareunpaid",
                    "node-state-congress-farepaid",
                    "node-state-congress-fareunpaid",
                    "node-state-congressstairs-street",
                    "node-state-congressstairs-wbplatform",
                    "node-state-exchange-farepaid",
                    "node-state-exchange-fareunpaid",
                    "node-state-exchangestairs-ebplatform",
                    "node-state-milk-farepaid",
                    "node-state-milk-fareunpaid",
                    "node-state-milkmidstairs-landing",
                    "node-state-milkmidstairs-street",
                    "node-state-milkststairs-lobby",
                    "node-state-ogoldstatehousestairs-landing",
                    "node-state-ogoldstatehousestairs-nbplatform",
                    "node-state-oldstate-farepaid",
                    "node-state-oldstate-fareunpaid",
                    "node-state-oldstateogministairs-landing",
                    "node-state-oldstateogministairs-street",
                    "node-state-oldstatepassageministairs-landing",
                    "node-state-oldstatepassageministairs-street",
                    "node-state-olnbplatformstairsmain-nbplatform",
                    "node-state-olnbplatformstairsmain-sbplatform",
                    "node-state-olnbplatformstairssec-nbplatform",
                    "node-state-olnbplatformstairssec-sbplatform",
                    "node-state-olpassagestairs-nbplatform",
                    "node-state-olpassagestairs-sbplatform",
                    "node-state-passageoldstatehousestairs-landing",
                    "node-state-passageoldstatehousestairs-sbplatform",
                    "node-state-statestreetstairs-lobby",
                    "node-state-water-farepaid",
                    "node-state-water-fareunpaid",
                    "node-state-waterststairs-lobby",
                    "node-state-wonderlandoldstatestairs-ebplatform",
                    "node-state-wonderlandoldstatestairs-landing",
                ),
            connectingStopIds =
                listOf<String>("65", "9370022", "9070022", "204", "9270022", "190", "191"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-sthld",
            latitude = 42.341614,
            longitude = -71.146202,
            name = "Sutherland Road",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70116", "70117"),
            connectingStopIds = listOf<String>("9070116"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-stpul",
            latitude = 42.343327,
            longitude = -71.116997,
            name = "Saint Paul Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70217", "70218"),
            connectingStopIds = listOf<String>("9070217", "9070218"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-sull",
            latitude = 42.383975,
            longitude = -71.076994,
            name = "Sullivan Square",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "29001",
                    "29002",
                    "29003",
                    "29004",
                    "29005",
                    "29006",
                    "29007",
                    "29008",
                    "29009",
                    "29010",
                    "29011",
                    "29012",
                    "29013",
                    "70030",
                    "70031",
                    "door-sull-main",
                    "node-307-lobby",
                    "node-307-platform",
                    "node-308-lobby",
                    "node-308-platform",
                    "node-840-lobby",
                    "node-840-platform",
                    "node-881-lobby",
                    "node-881-platform",
                    "node-lbusramp-lower",
                    "node-lbusramp-upper",
                    "node-lbusstairs-lower",
                    "node-lbusstairs-upper",
                    "node-sull-farepaid",
                    "node-sull-fareunpaid",
                    "node-sull-nbstairs-lobby",
                    "node-sull-nbstairs-platform",
                    "node-sull-sbstairs-lobby",
                    "node-sull-sbstairs-platform",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-sumav",
            latitude = 42.34111,
            longitude = -71.12561,
            name = "Summit Avenue",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70223", "70224"),
            connectingStopIds = listOf<String>("9070224", "9070223"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-symcl",
            latitude = 42.342687,
            longitude = -71.085056,
            name = "Symphony",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70241",
                    "70242",
                    "door-symcl-ebchurch",
                    "door-symcl-ebsymphony",
                    "door-symcl-falmouth",
                    "door-symcl-hunte",
                    "door-symcl-huntw",
                    "door-symcl-westland",
                    "node-symcl-churstair-platform",
                    "node-symcl-eb-farepaid",
                    "node-symcl-eb-fareunpaid",
                    "node-symcl-eb-midbottom",
                    "node-symcl-eb-midtop",
                    "node-symcl-falmstair-platform",
                    "node-symcl-hunestair-platform",
                    "node-symcl-hunwstair-platform",
                    "node-symcl-sympstair-platform",
                    "node-symcl-wb-ebottom",
                    "node-symcl-wb-etop",
                    "node-symcl-wb-farepaid",
                    "node-symcl-wb-fareunpaid",
                    "node-symcl-wb-midbottom",
                    "node-symcl-wb-midtop",
                    "node-symcl-wb-wbottom",
                    "node-symcl-wb-wtop",
                    "node-symcl-weststair-platform",
                ),
            connectingStopIds = listOf<String>("82", "89"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-tapst",
            latitude = 42.338459,
            longitude = -71.138702,
            name = "Tappan Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70231", "70232"),
            connectingStopIds = listOf<String>("9070232", "9070231"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-tumnl",
            latitude = 42.349662,
            longitude = -71.063917,
            name = "Tufts Medical Center",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70016",
                    "70017",
                    "door-tumnl-tremont",
                    "door-tumnl-washington",
                    "node-145-lobby",
                    "node-146-lobby",
                    "node-146-platform",
                    "node-147-lobby",
                    "node-147-platform",
                    "node-148-lobby",
                    "node-148-platform",
                    "node-149-lobby",
                    "node-149-platform",
                    "node-857-lobby",
                    "node-858-lobby",
                    "node-858-platform",
                    "node-859-lobby",
                    "node-859-platform",
                    "node-tumnl-tre-farepaid",
                    "node-tumnl-tre-fareunpaid",
                    "node-tumnl-trenbstairs-lobby",
                    "node-tumnl-trenbstairs-platform",
                    "node-tumnl-tresbstairs-lobby",
                    "node-tumnl-tresbstairs-platform",
                    "node-tumnl-trestairs-lobby",
                    "node-tumnl-wash-farepaid",
                    "node-tumnl-wash-fareunpaid",
                    "node-tumnl-washnbstairs-lobby",
                    "node-tumnl-washnbstairs-platform",
                    "node-tumnl-washsbstairs-lobby",
                    "node-tumnl-washsbstairs-platform",
                    "node-tumnl-washstairs-lobby",
                ),
            connectingStopIds = listOf<String>("49002", "6565", "8281"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-unsqu",
            latitude = 42.377359,
            longitude = -71.094761,
            name = "Union Square",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70503",
                    "70504",
                    "Union Square-01",
                    "Union Square-02",
                    "door-unsqu-allen",
                    "door-unsqu-elevator",
                    "door-unsqu-somerville",
                    "node-771-bottom",
                    "node-771-top",
                    "node-unsqu-rollupgate",
                    "node-unsqu-stairs-bottom",
                    "node-unsqu-stairs-top",
                ),
            connectingStopIds = listOf<String>("9070503", "12530", "2512", "2531"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-waban",
            latitude = 42.325845,
            longitude = -71.230609,
            name = "Waban",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70164",
                    "70165",
                    "door-waban-beacon",
                    "door-waban-parkr",
                    "door-waban-parks",
                    "door-waban-wyman",
                    "node-waban-beacon-ramp",
                    "node-waban-beacon-stairs",
                    "node-waban-parkstairs-platform",
                ),
            connectingStopIds = listOf<String>("9070164", "9070165"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-wascm",
            latitude = 42.343864,
            longitude = -71.142853,
            name = "Washington Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70120", "70121"),
            connectingStopIds = listOf<String>("9070120", "9070121", "1273", "1295", "9170120"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-welln",
            latitude = 42.40237,
            longitude = -71.077082,
            name = "Wellington",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "52710",
                    "52711",
                    "52712",
                    "52713",
                    "52714",
                    "52715",
                    "52716",
                    "52720",
                    "70032",
                    "70033",
                    "door-welln-busway",
                    "door-welln-garage",
                    "node-309-lobby",
                    "node-309-platform",
                    "node-310-lobby",
                    "node-310-platform",
                    "node-864-lobby",
                    "node-864-platform",
                    "node-865-lobby",
                    "node-865-platform",
                    "node-welln-busramp-lobby",
                    "node-welln-busstair-lobby",
                    "node-welln-farepaid",
                    "node-welln-fareunpaid",
                    "node-welln-fhstair-lobby",
                    "node-welln-fhstair-platform",
                    "node-welln-ogstair-lobby",
                    "node-welln-ogstair-platform",
                    "node-welln-skyelev-bridge",
                    "node-welln-skyentry",
                    "node-welln-skystairs-bridge",
                ),
            connectingStopIds = listOf<String>(),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-wlsta",
            latitude = 42.266514,
            longitude = -71.020337,
            name = "Wollaston",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds =
                listOf<String>(
                    "70099",
                    "70100",
                    "door-wlsta-beale",
                    "door-wlsta-newpnorth",
                    "door-wlsta-newpsouth",
                    "door-wlsta-parking",
                    "node-448-bridge",
                    "node-448-lobby",
                    "node-449-bridge",
                    "node-449-platform",
                    "node-733-bridge",
                    "node-733-lobby",
                    "node-734-lobby",
                    "node-734-platform",
                    "node-735-bridge",
                    "node-735-platform",
                    "node-wlsta-lobbyramp-lower",
                    "node-wlsta-lobbyramp-upper",
                    "node-wlsta-lobbysteps-lower",
                    "node-wlsta-lobbysteps-upper",
                    "node-wlsta-mainnewp-farepaid",
                    "node-wlsta-mainnewp-fareunpaid",
                    "node-wlsta-newpramp-lobby",
                    "node-wlsta-newpramp-middle",
                    "node-wlsta-newprampup-middle",
                    "node-wlsta-newpstairup-middle",
                    "node-wlsta-park-farepaid",
                    "node-wlsta-park-fareunpaid",
                    "node-wlsta-stair1-bridge",
                    "node-wlsta-stair1-lobby",
                    "node-wlsta-stair4-lobby",
                    "node-wlsta-stair4-platform",
                    "node-wlsta-stair5-bridge",
                    "node-wlsta-stair5-platform",
                    "node-wlsta-stair6-lobby",
                    "node-wlsta-stair6-platform",
                    "node-wlsta-stair6gate-farepaid",
                    "node-wlsta-stair6gate-fareunpaid",
                    "node-wlsta-stair7-lobby",
                    "node-wlsta-stair7-street",
                ),
            connectingStopIds = listOf<String>("9170099", "9170100"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-woodl",
            latitude = 42.332902,
            longitude = -71.243362,
            name = "Woodland",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70162", "70163", "door-woodl-east", "door-woodl-main"),
            connectingStopIds = listOf<String>("9070162"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "place-wrnst",
            latitude = 42.348343,
            longitude = -71.140457,
            name = "Warren Street",
            locationType = LocationType.STATION,
            description = null,
            platformCode = null,
            platformName = null,
            vehicleType = null,
            childStopIds = listOf<String>("70124", "70125"),
            connectingStopIds = listOf<String>("9070125", "9070124"),
            parentStationId = null,
            wheelchairBoarding = WheelchairBoardingStatus.INACCESSIBLE,
        )
    )
    objects.put(
        Stop(
            id = "river-B",
            latitude = 42.336666,
            longitude = -71.2533,
            name = "Riverside",
            locationType = LocationType.STOP,
            description = "Riverside - Commuter Rail Shuttle - Busway",
            platformCode = null,
            platformName = "Busway",
            vehicleType = RouteType.BUS,
            childStopIds = listOf<String>(),
            connectingStopIds = listOf<String>(),
            parentStationId = "place-river",
            wheelchairBoarding = WheelchairBoardingStatus.ACCESSIBLE,
        )
    )
}

private fun putTrips(objects: ObjectCollectionBuilder) {
    objects.put(
        Trip(
            id = "71373380",
            directionId = 1,
            headsign = "Ruggles",
            routeId = Route.Id("15"),
            routePatternId = "15-1-1",
            shapeId = "150308",
            stopIds =
                listOf<String>(
                    "323",
                    "322",
                    "557",
                    "558",
                    "559",
                    "560",
                    "561",
                    "1468",
                    "1469",
                    "1470",
                    "1471",
                    "1472",
                    "1473",
                    "14731",
                    "1474",
                    "1475",
                    "1478",
                    "1479",
                    "1480",
                    "1481",
                    "11482",
                    "14831",
                    "1484",
                    "1485",
                    "1486",
                    "1487",
                    "1488",
                    "1489",
                    "1491",
                    "64000",
                    "1148",
                    "11149",
                    "11148",
                    "21148",
                    "1224",
                    "17861",
                ),
        )
    )
    objects.put(
        Trip(
            id = "71373591",
            directionId = 0,
            headsign = "Fields Corner",
            routeId = Route.Id("15"),
            routePatternId = "15-1-0",
            shapeId = "150307",
            stopIds =
                listOf<String>(
                    "17863",
                    "11257",
                    "1259",
                    "11323",
                    "11259",
                    "64000",
                    "1493",
                    "1495",
                    "1496",
                    "1497",
                    "1498",
                    "1499",
                    "1500",
                    "1501",
                    "11501",
                    "1504",
                    "1506",
                    "1508",
                    "1509",
                    "1510",
                    "1511",
                    "11512",
                    "1512",
                    "1514",
                    "1515",
                    "553",
                    "554",
                    "555",
                    "556",
                    "323",
                ),
        )
    )
    objects.put(
        Trip(
            id = "71694434",
            directionId = 0,
            headsign = "Arlington Center",
            routeId = Route.Id("87"),
            routePatternId = "87-2-0",
            shapeId = "870206",
            stopIds =
                listOf<String>(
                    "70500",
                    "2605",
                    "2607",
                    "12610",
                    "2612",
                    "26131",
                    "12615",
                    "12616",
                    "2617",
                    "2618",
                    "2620",
                    "2621",
                    "2622",
                    "2623",
                    "2625",
                    "2626",
                    "2627",
                    "2628",
                    "5104",
                    "2631",
                    "2632",
                    "2634",
                    "2635",
                    "2715",
                    "2636",
                    "12636",
                    "12637",
                    "12638",
                    "12639",
                    "12640",
                    "12641",
                    "12642",
                    "12643",
                    "12644",
                ),
        )
    )
    objects.put(
        Trip(
            id = "71694436",
            directionId = 1,
            headsign = "Lechmere",
            routeId = Route.Id("87"),
            routePatternId = "87-2-1",
            shapeId = "870202",
            stopIds =
                listOf<String>(
                    "12644",
                    "12645",
                    "12648",
                    "12649",
                    "12650",
                    "12651",
                    "12652",
                    "12653",
                    "12654",
                    "2637",
                    "2575",
                    "2576",
                    "2577",
                    "2579",
                    "2580",
                    "2581",
                    "2582",
                    "2583",
                    "2584",
                    "2586",
                    "2587",
                    "2588",
                    "2589",
                    "2590",
                    "2591",
                    "2593",
                    "2594",
                    "2595",
                    "2510",
                    "2597",
                    "2598",
                    "2599",
                    "2600",
                    "2601",
                    "70500",
                ),
        )
    )
    objects.put(
        Trip(
            id = "71694754",
            directionId = 1,
            headsign = "Alewife via Arlington Center",
            routeId = Route.Id("67"),
            routePatternId = "67-4-1",
            shapeId = "670068",
            stopIds =
                listOf<String>(
                    "7961",
                    "7963",
                    "79642",
                    "7965",
                    "7966",
                    "7967",
                    "7968",
                    "7969",
                    "2258",
                    "2259",
                    "23535",
                    "23536",
                    "23537",
                    "23538",
                    "2475",
                    "2477",
                    "2478",
                    "24790",
                    "24791",
                    "14118",
                ),
        )
    )
    objects.put(
        Trip(
            id = "71694757",
            directionId = 0,
            headsign = "Turkey Hill via Arlington Center",
            routeId = Route.Id("67"),
            routePatternId = "67-4-0",
            shapeId = "670067",
            stopIds =
                listOf<String>(
                    "14121",
                    "2482",
                    "23530",
                    "23531",
                    "23532",
                    "23533",
                    "2283",
                    "2284",
                    "7976",
                    "7977",
                    "7978",
                    "7979",
                    "7981",
                    "7953",
                    "7954",
                    "7955",
                    "7956",
                    "7959",
                    "7960",
                    "7962",
                    "7961",
                ),
        )
    )
    objects.put(
        Trip(
            id = "72039742",
            directionId = 0,
            headsign = "Logan Airport",
            routeId = Route.Id("741"),
            routePatternId = "741-_-0",
            shapeId = "7410055",
            stopIds = listOf<String>("74611", "74612", "74613", "74624", "17091"),
        )
    )
    objects.put(
        Trip(
            id = "72039743",
            directionId = 0,
            headsign = "Silver Line Way",
            routeId = Route.Id("746"),
            routePatternId = "746-_-0",
            shapeId = "7460032",
            stopIds = listOf<String>("74611", "74612", "74613", "74614"),
        )
    )
    objects.put(
        Trip(
            id = "72039744",
            directionId = 1,
            headsign = "South Station",
            routeId = Route.Id("741"),
            routePatternId = "741-_-1",
            shapeId = "7410056",
            stopIds =
                listOf<String>(
                    "17091",
                    "27092",
                    "17093",
                    "17094",
                    "17095",
                    "17096",
                    "74614",
                    "74615",
                    "74616",
                    "74617",
                ),
        )
    )
    objects.put(
        Trip(
            id = "72039750",
            directionId = 1,
            headsign = "South Station",
            routeId = Route.Id("746"),
            routePatternId = "746-_-1",
            shapeId = "7460033",
            stopIds = listOf<String>("74614", "74615", "74616", "74617"),
        )
    )
    objects.put(
        Trip(
            id = "72039768",
            directionId = 1,
            headsign = "South Station",
            routeId = Route.Id("742"),
            routePatternId = "742-3-1",
            shapeId = "7420071",
            stopIds =
                listOf<String>(
                    "30250",
                    "30251",
                    "31259",
                    "31255",
                    "31257",
                    "31256",
                    "74614",
                    "74615",
                    "74616",
                    "74617",
                ),
        )
    )
    objects.put(
        Trip(
            id = "72039786",
            directionId = 0,
            headsign = "Design Center",
            routeId = Route.Id("742"),
            routePatternId = "742-3-0",
            shapeId = "7420072",
            stopIds = listOf<String>("74611", "74612", "74613", "74624", "247", "30249", "30250"),
        )
    )
    objects.put(
        Trip(
            id = "72040977",
            directionId = 0,
            headsign = "Chelsea",
            routeId = Route.Id("743"),
            routePatternId = "743-_-0",
            shapeId = "7430048",
            stopIds =
                listOf<String>(
                    "74611",
                    "74612",
                    "74613",
                    "74624",
                    "7096",
                    "74637",
                    "74635",
                    "74633",
                    "74631",
                ),
        )
    )
    objects.put(
        Trip(
            id = "72040978",
            directionId = 1,
            headsign = "South Station",
            routeId = Route.Id("743"),
            routePatternId = "743-_-1",
            shapeId = "7430047",
            stopIds =
                listOf<String>(
                    "74630",
                    "74632",
                    "74634",
                    "74636",
                    "7097",
                    "17096",
                    "74614",
                    "74615",
                    "74616",
                    "74617",
                ),
        )
    )
    objects.put(
        Trip(
            id = "HaverhillRestoredWKDY-744235-400",
            directionId = 1,
            headsign = "North Station",
            routeId = Route.Id("CR-Fitchburg"),
            routePatternId = "CR-Fitchburg-d82ea33a-1",
            shapeId = "9840003",
            stopIds =
                listOf<String>(
                    "FR-3338-CS",
                    "FR-0494-CS",
                    "FR-0451-02",
                    "FR-0394-02",
                    "FR-0361-02",
                    "FR-0301-02",
                    "FR-0253-02",
                    "FR-0219-02",
                    "FR-0201-02",
                    "FR-0167-02",
                    "FR-0132-02",
                    "FR-0115-02",
                    "FR-0098-S",
                    "FR-0074-02",
                    "FR-0064-02",
                    "FR-0034-02",
                    "BNT-0000",
                ),
        )
    )
    objects.put(
        Trip(
            id = "HaverhillRestoredWKDY-744237-405",
            directionId = 0,
            headsign = "Wachusett",
            routeId = Route.Id("CR-Fitchburg"),
            routePatternId = "CR-Fitchburg-2a5f6366-0",
            shapeId = "9840004",
            stopIds =
                listOf<String>(
                    "BNT-0000",
                    "FR-0034-01",
                    "FR-0064-01",
                    "FR-0074-01",
                    "FR-0098-01",
                    "FR-0115-01",
                    "FR-0132-01",
                    "FR-0167-01",
                    "FR-0201-01",
                    "FR-0219-01",
                    "FR-0253-01",
                    "FR-0301-01",
                    "FR-0361-01",
                    "FR-0394-01",
                    "FR-0451-01",
                    "FR-0494-CS",
                    "FR-3338-CS",
                ),
        )
    )
    objects.put(
        Trip(
            id = "HaverhillRestoredWKDY-744311-24",
            directionId = 1,
            headsign = "North Station",
            routeId = Route.Id("CR-Newburyport"),
            routePatternId = "CR-Newburyport-d47c5647-1",
            shapeId = "9810006",
            stopIds =
                listOf<String>(
                    "GB-0353-S",
                    "GB-0316-S",
                    "GB-0296-02",
                    "GB-0254-02",
                    "GB-0229-02",
                    "GB-0198-02",
                    "ER-0183-02",
                    "ER-0168-S",
                    "ER-0128-02",
                    "ER-0117-02",
                    "ER-0042-02",
                    "BNT-0000",
                ),
        )
    )
    objects.put(
        Trip(
            id = "HaverhillRestoredWKDY-744312-27",
            directionId = 0,
            headsign = "Rockport",
            routeId = Route.Id("CR-Newburyport"),
            routePatternId = "CR-Newburyport-e54dc640-0",
            shapeId = "9810007",
            stopIds =
                listOf<String>(
                    "BNT-0000",
                    "ER-0042-01",
                    "ER-0117-01",
                    "ER-0128-01",
                    "ER-0168-S",
                    "ER-0183-01",
                    "GB-0198-01",
                    "GB-0229-01",
                    "GB-0254-01",
                    "GB-0296-01",
                    "GB-0316-S",
                    "GB-0353-S",
                ),
        )
    )
    objects.put(
        Trip(
            id = "HaverhillRestoredWKDY-744338-114",
            directionId = 1,
            headsign = "North Station",
            routeId = Route.Id("CR-Newburyport"),
            routePatternId = "CR-Newburyport-7e4857df-1",
            shapeId = "9810001",
            stopIds =
                listOf<String>(
                    "ER-0362-01",
                    "ER-0312-S",
                    "ER-0276-S",
                    "ER-0227-S",
                    "ER-0208-02",
                    "ER-0183-02",
                    "ER-0168-S",
                    "ER-0128-02",
                    "ER-0117-02",
                    "ER-0042-02",
                    "BNT-0000",
                ),
        )
    )
    objects.put(
        Trip(
            id = "HaverhillRestoredWKDY-744341-125",
            directionId = 0,
            headsign = "Newburyport",
            routeId = Route.Id("CR-Newburyport"),
            routePatternId = "CR-Newburyport-79533330-0",
            shapeId = "9810002",
            stopIds =
                listOf<String>(
                    "BNT-0000",
                    "ER-0042-01",
                    "ER-0117-01",
                    "ER-0128-01",
                    "ER-0168-S",
                    "ER-0183-01",
                    "ER-0208-01",
                    "ER-0227-S",
                    "ER-0276-S",
                    "ER-0312-S",
                    "ER-0362-01",
                ),
        )
    )
    objects.put(
        Trip(
            id = "Sept8Read-767840-852",
            directionId = 1,
            headsign = "South Station",
            routeId = Route.Id("CR-Providence"),
            routePatternId = "CR-Providence-e9395acc-1",
            shapeId = "9890008",
            stopIds =
                listOf<String>(
                    "NEC-1659-03",
                    "NEC-1768-03",
                    "NEC-1851-03",
                    "NEC-1891-02",
                    "NEC-1969-04",
                    "NEC-2040-02",
                    "NEC-2108-02",
                    "NEC-2139-02",
                    "NEC-2173-02",
                    "NEC-2265-02",
                    "NEC-2276-02",
                    "NEC-2287",
                ),
        )
    )
    objects.put(
        Trip(
            id = "Sept8Read-767850-839",
            directionId = 0,
            headsign = "Wickford Junction",
            routeId = Route.Id("CR-Providence"),
            routePatternId = "CR-Providence-9cf54fb3-0",
            shapeId = "9890009",
            stopIds =
                listOf<String>(
                    "NEC-2287",
                    "NEC-2276-01",
                    "NEC-2265-01",
                    "NEC-2173-01",
                    "NEC-2139-01",
                    "NEC-2108-01",
                    "NEC-2040-01",
                    "NEC-1969-03",
                    "NEC-1891-01",
                    "NEC-1851-03",
                    "NEC-1768-03",
                    "NEC-1659-03",
                ),
        )
    )
    objects.put(
        Trip(
            id = "Sept8Read-768056-925",
            directionId = 0,
            headsign = "Stoughton",
            routeId = Route.Id("CR-Providence"),
            routePatternId = "CR-Providence-9515a09b-0",
            shapeId = "9890004",
            stopIds =
                listOf<String>(
                    "NEC-2287",
                    "NEC-2276-01",
                    "NEC-2265-01",
                    "NEC-2173-01",
                    "SB-0150-04",
                    "SB-0156-S",
                    "SB-0189-S",
                ),
        )
    )
    objects.put(
        Trip(
            id = "Sept8Read-768057-930",
            directionId = 1,
            headsign = "South Station",
            routeId = Route.Id("CR-Providence"),
            routePatternId = "CR-Providence-6cae46be-1",
            shapeId = "9890003",
            stopIds =
                listOf<String>(
                    "SB-0189-S",
                    "SB-0156-S",
                    "SB-0150-06",
                    "NEC-2173-02",
                    "NEC-2203-02",
                    "NEC-2265-02",
                    "NEC-2276-02",
                    "NEC-2287",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-CR-Haverhill-C1-0",
            directionId = 0,
            headsign = "Haverhill",
            routeId = Route.Id("CR-Haverhill"),
            routePatternId = "CR-Haverhill-779dede9-0",
            shapeId = "canonical-9820002",
            stopIds =
                listOf<String>(
                    "BNT-0000",
                    "WR-0045-S",
                    "WR-0053-S",
                    "WR-0062-01",
                    "WR-0067-01",
                    "WR-0075-01",
                    "WR-0085-01",
                    "WR-0099-01",
                    "WR-0120-S",
                    "WR-0163-S",
                    "WR-0205-02",
                    "WR-0228-02",
                    "WR-0264-02",
                    "WR-0325-01",
                    "WR-0329-01",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-CR-Haverhill-C1-1",
            directionId = 1,
            headsign = "North Station",
            routeId = Route.Id("CR-Haverhill"),
            routePatternId = "CR-Haverhill-ebc58735-1",
            shapeId = "canonical-9820001",
            stopIds =
                listOf<String>(
                    "WR-0329-02",
                    "WR-0325-02",
                    "WR-0264-02",
                    "WR-0228-02",
                    "WR-0205-02",
                    "WR-0163-S",
                    "WR-0120-S",
                    "WR-0099-02",
                    "WR-0085-02",
                    "WR-0075-02",
                    "WR-0067-02",
                    "WR-0062-02",
                    "WR-0053-S",
                    "WR-0045-S",
                    "BNT-0000",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-CR-Lowell-C1-0",
            directionId = 0,
            headsign = "Lowell",
            routeId = Route.Id("CR-Lowell"),
            routePatternId = "CR-Lowell-edb39c7b-0",
            shapeId = "canonical-9830006",
            stopIds =
                listOf<String>(
                    "BNT-0000",
                    "NHRML-0055-01",
                    "NHRML-0073-01",
                    "NHRML-0078-01",
                    "NHRML-0127-01",
                    "NHRML-0152-01",
                    "NHRML-0218-01",
                    "NHRML-0254-04",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-CR-Lowell-C1-1",
            directionId = 1,
            headsign = "North Station",
            routeId = Route.Id("CR-Lowell"),
            routePatternId = "CR-Lowell-305fef81-1",
            shapeId = "canonical-9830005",
            stopIds =
                listOf<String>(
                    "NHRML-0254-04",
                    "NHRML-0218-02",
                    "NHRML-0152-02",
                    "NHRML-0127-02",
                    "NHRML-0078-02",
                    "NHRML-0073-02",
                    "NHRML-0055-02",
                    "BNT-0000",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-Green-B-C1-0",
            directionId = 0,
            headsign = "Boston College",
            routeId = Route.Id("Green-B"),
            routePatternId = "Green-B-812-0",
            shapeId = "canonical-8000013",
            stopIds =
                listOf<String>(
                    "70202",
                    "70196",
                    "70159",
                    "70157",
                    "70155",
                    "70153",
                    "71151",
                    "70149",
                    "70147",
                    "70145",
                    "170141",
                    "170137",
                    "70135",
                    "70131",
                    "70129",
                    "70127",
                    "70125",
                    "70121",
                    "70117",
                    "70115",
                    "70113",
                    "70111",
                    "70107",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-Green-B-C1-1",
            directionId = 1,
            headsign = "Government Center",
            routeId = Route.Id("Green-B"),
            routePatternId = "Green-B-812-1",
            shapeId = "canonical-8000012",
            stopIds =
                listOf<String>(
                    "70106",
                    "70110",
                    "70112",
                    "70114",
                    "70116",
                    "70120",
                    "70124",
                    "70126",
                    "70128",
                    "70130",
                    "70134",
                    "170136",
                    "170140",
                    "70144",
                    "70146",
                    "70148",
                    "71150",
                    "70152",
                    "70154",
                    "70156",
                    "70158",
                    "70200",
                    "70201",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-Green-C-C1-0",
            directionId = 0,
            headsign = "Cleveland Circle",
            routeId = Route.Id("Green-C"),
            routePatternId = "Green-C-832-0",
            shapeId = "canonical-8000006",
            stopIds =
                listOf<String>(
                    "70202",
                    "70197",
                    "70159",
                    "70157",
                    "70155",
                    "70153",
                    "70151",
                    "70211",
                    "70213",
                    "70215",
                    "70217",
                    "70219",
                    "70223",
                    "70225",
                    "70227",
                    "70229",
                    "70231",
                    "70233",
                    "70235",
                    "70237",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-Green-C-C1-1",
            directionId = 1,
            headsign = "Government Center",
            routeId = Route.Id("Green-C"),
            routePatternId = "Green-C-832-1",
            shapeId = "canonical-8000005",
            stopIds =
                listOf<String>(
                    "70238",
                    "70236",
                    "70234",
                    "70232",
                    "70230",
                    "70228",
                    "70226",
                    "70224",
                    "70220",
                    "70218",
                    "70216",
                    "70214",
                    "70212",
                    "70150",
                    "70152",
                    "70154",
                    "70156",
                    "70158",
                    "70200",
                    "70201",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-Green-D-C1-0",
            directionId = 0,
            headsign = "Riverside",
            routeId = Route.Id("Green-D"),
            routePatternId = "Green-D-855-0",
            shapeId = "canonical-8000008",
            stopIds =
                listOf<String>(
                    "70504",
                    "70502",
                    "70208",
                    "70206",
                    "70204",
                    "70202",
                    "70198",
                    "70159",
                    "70157",
                    "70155",
                    "70153",
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
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-Green-D-C1-1",
            directionId = 1,
            headsign = "Union Square",
            routeId = Route.Id("Green-D"),
            routePatternId = "Green-D-855-1",
            shapeId = "canonical-8000009",
            stopIds =
                listOf<String>(
                    "70160",
                    "70162",
                    "70164",
                    "70166",
                    "70168",
                    "70170",
                    "70172",
                    "70174",
                    "70176",
                    "70178",
                    "70180",
                    "70182",
                    "70186",
                    "70150",
                    "70152",
                    "70154",
                    "70156",
                    "70158",
                    "70200",
                    "70201",
                    "70203",
                    "70205",
                    "70207",
                    "70501",
                    "70503",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-Green-E-C1-0",
            directionId = 0,
            headsign = "Heath Street",
            routeId = Route.Id("Green-E"),
            routePatternId = "Green-E-886-0",
            shapeId = "canonical-8000018",
            stopIds =
                listOf<String>(
                    "70512",
                    "70510",
                    "70508",
                    "70506",
                    "70514",
                    "70502",
                    "70208",
                    "70206",
                    "70204",
                    "70202",
                    "70199",
                    "70159",
                    "70157",
                    "70155",
                    "70239",
                    "70241",
                    "70243",
                    "70245",
                    "70247",
                    "70249",
                    "70251",
                    "70253",
                    "70255",
                    "70257",
                    "70260",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-Green-E-C1-1",
            directionId = 1,
            headsign = "Medford/Tufts",
            routeId = Route.Id("Green-E"),
            routePatternId = "Green-E-886-1",
            shapeId = "canonical-8000015",
            stopIds =
                listOf<String>(
                    "70260",
                    "70258",
                    "70256",
                    "70254",
                    "70252",
                    "70250",
                    "70248",
                    "70246",
                    "70244",
                    "70242",
                    "70240",
                    "70154",
                    "70156",
                    "70158",
                    "70200",
                    "70201",
                    "70203",
                    "70205",
                    "70207",
                    "70501",
                    "70513",
                    "70505",
                    "70507",
                    "70509",
                    "70511",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-Orange-C1-0",
            directionId = 0,
            headsign = "Forest Hills",
            routeId = Route.Id("Orange"),
            routePatternId = "Orange-3-0",
            shapeId = "canonical-903_0018",
            stopIds =
                listOf<String>(
                    "70036",
                    "70034",
                    "70032",
                    "70278",
                    "70030",
                    "70028",
                    "70026",
                    "70024",
                    "70022",
                    "70020",
                    "70018",
                    "70016",
                    "70014",
                    "70012",
                    "70010",
                    "70008",
                    "70006",
                    "70004",
                    "70002",
                    "70001",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-Orange-C1-1",
            directionId = 1,
            headsign = "Oak Grove",
            routeId = Route.Id("Orange"),
            routePatternId = "Orange-3-1",
            shapeId = "canonical-903_0017",
            stopIds =
                listOf<String>(
                    "70001",
                    "70003",
                    "70005",
                    "70007",
                    "70009",
                    "70011",
                    "70013",
                    "70015",
                    "70017",
                    "70019",
                    "70021",
                    "70023",
                    "70025",
                    "70027",
                    "70029",
                    "70031",
                    "70279",
                    "70033",
                    "70035",
                    "70036",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-Red-C1-0",
            directionId = 0,
            headsign = "Braintree",
            routeId = Route.Id("Red"),
            routePatternId = "Red-3-0",
            shapeId = "canonical-933_0009",
            stopIds =
                listOf<String>(
                    "70061",
                    "70063",
                    "70065",
                    "70067",
                    "70069",
                    "70071",
                    "70073",
                    "70075",
                    "70077",
                    "70079",
                    "70081",
                    "70083",
                    "70095",
                    "70097",
                    "70099",
                    "70101",
                    "70103",
                    "70105",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-Red-C1-1",
            directionId = 1,
            headsign = "Alewife",
            routeId = Route.Id("Red"),
            routePatternId = "Red-3-1",
            shapeId = "canonical-933_0010",
            stopIds =
                listOf<String>(
                    "70105",
                    "70104",
                    "70102",
                    "70100",
                    "70098",
                    "70096",
                    "70084",
                    "70082",
                    "70080",
                    "70078",
                    "70076",
                    "70074",
                    "70072",
                    "70070",
                    "70068",
                    "70066",
                    "70064",
                    "70061",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-Red-C2-0",
            directionId = 0,
            headsign = "Ashmont",
            routeId = Route.Id("Red"),
            routePatternId = "Red-1-0",
            shapeId = "canonical-931_0009",
            stopIds =
                listOf<String>(
                    "70061",
                    "70063",
                    "70065",
                    "70067",
                    "70069",
                    "70071",
                    "70073",
                    "70075",
                    "70077",
                    "70079",
                    "70081",
                    "70083",
                    "70085",
                    "70087",
                    "70089",
                    "70091",
                    "70093",
                ),
        )
    )
    objects.put(
        Trip(
            id = "canonical-Red-C2-1",
            directionId = 1,
            headsign = "Alewife",
            routeId = Route.Id("Red"),
            routePatternId = "Red-1-1",
            shapeId = "canonical-931_0010",
            stopIds =
                listOf<String>(
                    "70094",
                    "70092",
                    "70090",
                    "70088",
                    "70086",
                    "70084",
                    "70082",
                    "70080",
                    "70078",
                    "70076",
                    "70074",
                    "70072",
                    "70070",
                    "70068",
                    "70066",
                    "70064",
                    "70061",
                ),
        )
    )
}
