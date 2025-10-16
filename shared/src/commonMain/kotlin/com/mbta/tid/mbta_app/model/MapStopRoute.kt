package com.mbta.tid.mbta_app.model

import kotlinx.serialization.Serializable

public val silverRoutes: Set<Route.Id> =
    setOf(
        Route.Id("741"),
        Route.Id("742"),
        Route.Id("743"),
        Route.Id("751"),
        Route.Id("749"),
        Route.Id("746"),
    )
internal val greenRoutes =
    setOf(Route.Id("Green-B"), Route.Id("Green-C"), Route.Id("Green-D"), Route.Id("Green-E"))

@Serializable
public enum class MapStopRoute(
    internal val hasBranchingTerminals: Boolean = false,
    internal val branchingRoutes: Set<Route.Id> = setOf(),
) {
    RED {
        override fun matches(route: Route): Boolean {
            return route.id == Route.Id("Red")
        }
    },
    MATTAPAN {
        override fun matches(route: Route): Boolean {
            return route.id == Route.Id("Mattapan")
        }
    },
    ORANGE {
        override fun matches(route: Route): Boolean {
            return route.id == Route.Id("Orange")
        }
    },
    GREEN(hasBranchingTerminals = true, branchingRoutes = greenRoutes) {
        override fun matches(route: Route): Boolean {
            return route.id.idText.startsWith("Green")
        }
    },
    BLUE {
        override fun matches(route: Route): Boolean {
            return route.id == Route.Id("Blue")
        }
    },
    SILVER(hasBranchingTerminals = true, branchingRoutes = silverRoutes) {
        override fun matches(route: Route): Boolean {
            return branchingRoutes.contains(route.id)
        }
    },
    COMMUTER {
        override fun matches(route: Route): Boolean {
            return route.type == RouteType.COMMUTER_RAIL
        }
    },
    FERRY {
        override fun matches(route: Route): Boolean {
            return route.type == RouteType.FERRY
        }
    },
    BUS {
        override fun matches(route: Route): Boolean {
            return route.type == RouteType.BUS
        }
    };

    internal abstract fun matches(route: Route): Boolean

    internal companion object {
        fun matching(route: Route): MapStopRoute? {
            return entries.firstOrNull { it.matches(route) }
        }
    }
}
