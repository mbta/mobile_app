package com.mbta.tid.mbta_app.model

import kotlinx.serialization.Serializable

val silverRoutes = setOf("741", "742", "743", "751", "749", "746")
val greenRoutes = setOf("Green-B", "Green-C", "Green-D", "Green-E")

@Serializable
enum class MapStopRoute(
    val hasBranchingTerminals: Boolean = false,
    val branchingRoutes: Set<String> = setOf(),
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

    abstract fun matches(route: Route): Boolean

    companion object {
        fun matching(route: Route): MapStopRoute? {
            return MapStopRoute.entries.firstOrNull { it.matches(route) }
        }
    }
}
