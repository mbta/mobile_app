package com.mbta.tid.mbta_app.model

data class RoutePillSpec(
    val textColor: String,
    val routeColor: String,
    val content: Content,
    val size: Size,
    val shape: Shape
) {
    enum class Type {
        Fixed,
        Flex
    }

    sealed interface Content {
        data object Empty : Content

        data class Text(val text: String) : Content

        data class ModeImage(val mode: RouteType) : Content
    }

    enum class Size {
        FixedPill,
        Circle,
        CircleSmall,
        FlexPill,
        FlexPillSmall
    }

    enum class Shape {
        Capsule,
        Rectangle
    }

    constructor(
        route: Route?,
        line: Line?,
        type: Type
    ) : this(
        route?.takeUnless { it.id.startsWith("Shuttle") }?.textColor ?: line?.textColor ?: "",
        route?.takeUnless { it.id.startsWith("Shuttle") }?.color ?: line?.color ?: "",
        when (route?.type) {
            null -> if (line == null) Content.Empty else linePillContent(line)
            RouteType.LIGHT_RAIL -> lightRailPillContent(route, type)
            RouteType.HEAVY_RAIL -> heavyRailPillContent(route)
            RouteType.COMMUTER_RAIL -> commuterRailPillContent(route, type)
            RouteType.BUS -> busPillContent(route, type)
            RouteType.FERRY -> ferryPillContent(route, type)
        },
        when {
            type == Type.Fixed -> Size.FixedPill
            route?.longName?.startsWith("Green Line ") ?: false -> Size.Circle
            else -> Size.FlexPill
        },
        when {
            route?.type == RouteType.BUS && !route.id.startsWith("Shuttle") -> Shape.Rectangle
            else -> Shape.Capsule
        }
    )

    constructor(
        stopResultRoute: StopResultRoute,
    ) : this(
        if (stopResultRoute.type == RouteType.BUS && stopResultRoute.icon != "silver_line") {
            "192026"
        } else {
            "FFFFFF"
        },
        when (stopResultRoute.icon) {
            "orange_line" -> "ED8B00"
            "red_line",
            "mattapan_line" -> "DA291C"
            "blue_line" -> "003DA5"
            "commuter_rail" -> "80276C"
            "bus" -> "FFC72C"
            "silver_line" -> "7C878E"
            "ferry" -> "008EAA"
            else -> if (stopResultRoute.icon.contains("green_line_")) "00843D" else ""
        },
        when (stopResultRoute.type) {
            RouteType.LIGHT_RAIL -> lightRailPillContent(stopResultRoute)
            RouteType.HEAVY_RAIL -> heavyRailPillContent(stopResultRoute)
            RouteType.COMMUTER_RAIL -> Content.Text("CR")
            RouteType.BUS -> Content.ModeImage(RouteType.BUS)
            RouteType.FERRY -> Content.ModeImage(RouteType.FERRY)
        },
        when {
            stopResultRoute.icon.startsWith("green_line_") -> Size.CircleSmall
            else -> Size.FlexPillSmall
        },
        when {
            stopResultRoute.type == RouteType.BUS -> Shape.Rectangle
            else -> Shape.Capsule
        }
    )

    companion object {

        private fun lightRailPillContent(stopResultRoute: StopResultRoute): Content =
            if (stopResultRoute.icon.startsWith("green_line_")) {
                Content.Text(stopResultRoute.icon.replace("green_line_", "").uppercase())
            } else if (stopResultRoute.icon == "mattapan_line") {
                Content.Text("M")
            } else {
                val text =
                    when (stopResultRoute.icon) {
                        "orange_line" -> "Orange Line"
                        "red_line" -> "Red Line"
                        "blue_line" -> "Blue Line"
                        else -> ""
                    }
                Content.Text(text)
            }

        private fun heavyRailPillContent(stopResultRoute: StopResultRoute): Content =
            Content.Text(
                stopResultRoute.icon.split("_").joinToString("") { it.first().uppercase() }
            )

        private fun linePillContent(line: Line): Content =
            if (line.longName == "Green Line") {
                Content.Text("GL")
            } else {
                Content.Text(line.longName)
            }

        private fun lightRailPillContent(route: Route, type: Type): Content =
            if (route.longName.startsWith("Green Line ")) {
                when (type) {
                    Type.Fixed -> Content.Text(route.longName.replace("Green Line ", "GL "))
                    Type.Flex -> Content.Text(route.shortName)
                }
            } else if (route.longName == "Mattapan Trolley") {
                Content.Text("M")
            } else {
                Content.Text(route.longName)
            }

        private fun heavyRailPillContent(route: Route): Content =
            Content.Text(route.longName.split(" ").map { it.first() }.joinToString(""))

        private fun commuterRailPillContent(route: Route, type: Type): Content =
            when (type) {
                Type.Fixed -> Content.Text("CR")
                Type.Flex -> Content.Text(route.longName.removeSuffix(" Line"))
            }

        private fun busPillContent(route: Route, type: Type): Content =
            if (route.id.startsWith("Shuttle") && type == Type.Fixed) {
                Content.ModeImage(RouteType.BUS)
            } else {
                Content.Text(route.shortName)
            }

        private fun ferryPillContent(route: Route, type: Type): Content =
            when (type) {
                Type.Fixed -> Content.ModeImage(RouteType.FERRY)
                Type.Flex -> Content.Text(route.longName)
            }
    }
}
