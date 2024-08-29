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
        FlexPill
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
            null -> if (line == null) Content.Empty else linePillContent(line, type)
            RouteType.LIGHT_RAIL -> lightRailPillContent(route, type)
            RouteType.HEAVY_RAIL -> heavyRailPillContent(route, type)
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

    companion object {
        private fun linePillContent(line: Line, type: Type): Content =
            if (line.longName == "Green Line" && type == Type.Fixed) {
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
            } else if (route.longName == "Mattapan Trolley" && type == Type.Fixed) {
                Content.Text("M")
            } else {
                Content.Text(route.longName)
            }

        private fun heavyRailPillContent(route: Route, type: Type): Content =
            when (type) {
                Type.Fixed ->
                    Content.Text(route.longName.split(" ").map { it.first() }.joinToString(""))
                Type.Flex -> Content.Text(route.longName)
            }

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
