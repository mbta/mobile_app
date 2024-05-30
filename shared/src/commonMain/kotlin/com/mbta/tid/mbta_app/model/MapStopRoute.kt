package com.mbta.tid.mbta_app.model

enum class MapStopRoute {
    RED {
        override fun matches(route: Route): Boolean {
            return route.id == "Red" || route.id == "Mattapan"
        }
    },
    ORANGE {
        override fun matches(route: Route): Boolean {
            return route.id == "Orange"
        }
    },
    GREEN {
        override fun matches(route: Route): Boolean {
            return route.id.startsWith("Green")
        }
    },
    BLUE {
        override fun matches(route: Route): Boolean {
            return route.id == "Blue"
        }
    },
    SILVER {
        override fun matches(route: Route): Boolean {
            return MapStopRoute.slRoutes.contains(route.id)
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
        val slRoutes = setOf("741", "742", "743", "751", "749", "746")

        fun matching(route: Route): MapStopRoute? {
            return MapStopRoute.entries.firstOrNull() { it.matches(route) }
        }
    }
}
