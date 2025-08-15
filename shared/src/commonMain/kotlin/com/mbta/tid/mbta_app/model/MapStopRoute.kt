package com.mbta.tid.mbta_app.model

import kotlinx.serialization.Serializable

public val silverRoutes: Set<String> = setOf("741", "742", "743", "751", "749", "746")
internal val greenRoutes = setOf("Green-B", "Green-C", "Green-D", "Green-E")

@Serializable
public enum class MapStopRoute(
    internal val hasBranchingTerminals: Boolean = false,
    internal val branchingRoutes: Set<String> = setOf(),
) {
    RED {
        override fun matches(route: Route): Boolean {
            return route.id == "Red"
        }
    },
    MATTAPAN {
        override fun matches(route: Route): Boolean {
            return route.id == "Mattapan"
        }
    },
    ORANGE {
        override fun matches(route: Route): Boolean {
            return route.id == "Orange"
        }
    },
    GREEN(hasBranchingTerminals = true, branchingRoutes = greenRoutes) {
        override fun matches(route: Route): Boolean {
            return route.id.startsWith("Green")
        }
    },
    BLUE {
        override fun matches(route: Route): Boolean {
            return route.id == "Blue"
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
