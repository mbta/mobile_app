package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop

public data class RoutePillSpec(
    val textColor: String,
    val routeColor: String,
    val content: Content,
    val size: Size,
    val shape: Shape,
    val contentDescription: ContentDescription? = null,
) {
    public enum class Type {
        Fixed,
        Flex,
        FlexCompact,
    }

    public sealed interface Content {
        public data object Empty : Content

        public data class Text(val text: String) : Content

        public data class ModeImage internal constructor(val mode: RouteType) : Content
    }

    public enum class Size {
        FixedPill,
        Circle,
        CircleSmall,
        FlexPill,
        FlexPillSmall,
    }

    public enum class Shape {
        Capsule,
        Rectangle,
    }

    public enum class Context {
        SearchStation,
        Default,
    }

    public sealed class ContentDescription {
        public data class StopSearchResultRoute(
            val routeName: String?,
            val routeType: RouteType,
            val isOnly: Boolean,
        ) : ContentDescription()
    }

    @DefaultArgumentInterop.Enabled
    public constructor(
        route: Route?,
        line: Line?,
        type: Type,
        context: Context = Context.Default,
        contentDescription: ContentDescription? = null,
    ) : this(
        route?.textColor ?: line?.textColor ?: "FFFFFF",
        route?.color ?: line?.color ?: "000000",
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
            route?.type == RouteType.BUS && !route.isShuttle -> Shape.Rectangle
            else -> Shape.Capsule
        },
        contentDescription = contentDescription,
    )

    internal companion object {

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
            if ((route.isShuttle && type != Type.Flex) || context == Context.SearchStation) {
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
