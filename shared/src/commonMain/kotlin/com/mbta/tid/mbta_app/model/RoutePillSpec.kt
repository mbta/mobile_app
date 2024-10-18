package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop

data class RoutePillSpec(
    val textColor: String,
    val routeColor: String,
    val content: Content,
    val size: Size,
    val shape: Shape
) {
    enum class Type {
        Fixed,
        Flex,
        FlexCompact
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

    enum class Context {
        SearchStation,
        Default
    }

    @DefaultArgumentInterop.Enabled
    constructor(
        route: Route?,
        line: Line?,
        type: Type,
        context: Context = Context.Default
    ) : this(
        route?.takeUnless { it.id.startsWith("Shuttle") }?.textColor ?: line?.textColor ?: "",
        route?.takeUnless { it.id.startsWith("Shuttle") }?.color ?: line?.color ?: "",
        when (route?.type) {
            null -> if (line == null) Content.Empty else linePillContent(line)
            RouteType.LIGHT_RAIL -> lightRailPillContent(route, type)
            RouteType.HEAVY_RAIL -> heavyRailPillContent(route)
            RouteType.COMMUTER_RAIL -> commuterRailPillContent(route, type)
            RouteType.BUS -> busPillContent(route, type, context)
            RouteType.FERRY -> ferryPillContent(route, type)
        },
        when {
            type == Type.Fixed -> Size.FixedPill
            route?.longName?.startsWith("Green Line ") ?: false ->
                if (type == Type.FlexCompact) {
                    Size.CircleSmall
                } else {
                    Size.Circle
                }
            type == Type.FlexCompact -> Size.FlexPillSmall
            else -> Size.FlexPill
        },
        when {
            route?.type == RouteType.BUS && !route.id.startsWith("Shuttle") -> Shape.Rectangle
            else -> Shape.Capsule
        }
    )

    companion object {

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
                    Type.Flex,
                    Type.FlexCompact -> Content.Text(route.shortName)
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
                Type.Fixed,
                Type.FlexCompact -> Content.Text("CR")
                Type.Flex -> Content.Text(route.longName.removeSuffix(" Line"))
            }

        private fun busPillContent(route: Route, type: Type, context: Context): Content =
            if (
                (route.id.startsWith("Shuttle") && type != Type.Flex) ||
                    context == Context.SearchStation
            ) {
                Content.ModeImage(RouteType.BUS)
            } else {
                Content.Text(route.shortName)
            }

        private fun ferryPillContent(route: Route, type: Type): Content =
            when (type) {
                Type.Fixed,
                Type.FlexCompact -> Content.ModeImage(RouteType.FERRY)
                Type.Flex -> Content.Text(route.longName)
            }
    }
}
