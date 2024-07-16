package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.map.style.Exp
import com.mbta.tid.mbta_app.model.MapStopRoute
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

object MapExp {
    // Get the array of MapStopRoute string values from a stop feature
    val routesExp = Exp("get", StopSourceGenerator.propMapRoutesKey)

    // Returns true if there is only a single type of route served at this stop
    val singleRouteTypeExp = Exp("==", 1, Exp("length", routesExp))

    // Get the route type string ordered in the top position of the route type array,
    // or an empty string if there's nothing in the array
    val topRouteExp =
        Exp(
            "string",
            Exp("case", Exp("==", Exp("length", routesExp), 0) to "", Exp("at", 0, routesExp))
        )

    // Returns true if there's both a single type of route, and only a single route ID
    val singleRouteExp =
        Exp(
            "all",
            singleRouteTypeExp,
            Exp(
                "==",
                1,
                Exp(
                    "length",
                    Exp("get", topRouteExp, Exp("get", StopSourceGenerator.propRouteIdsKey))
                )
            )
        )

    // Return true if the route type is a branching route, and there is only
    // a single route ID served at this stop, that means we're on a branch
    val branchedRouteExp =
        Exp(
            "all",
            Exp(
                "any",
                Exp("==", topRouteExp, MapStopRoute.GREEN.name),
                Exp("==", topRouteExp, MapStopRoute.SILVER.name)
            ),
            singleRouteExp
        )

    // Get the route string in the stop feature's route array at the provided index
    fun routeAt(index: Int): Exp {
        return Exp("string", Exp("at", index, routesExp))
    }

    // Returns true if you're currently on the stop page for this stop
    val selectedExp = Exp("boolean", Exp("get", StopSourceGenerator.propIsSelectedKey))

    // Returns the iconSize of the stop icon, interpolated by zoom level, and sized differently
    // depending on the route served by the stop.
    val selectedSizeExp =
        Exp(
            "interpolate",
            Exp("exponential", 1.5),
            Exp("zoom"),
            MapDefaults.midZoomThreshold to
                withMultipliers(0.25, modeResize = listOf(0.5, 2, 1.75)),
            13 to withMultipliers(0.625, modeResize = listOf(1, 1.5, 1.5)),
            14 to withMultipliers(1)
        )

    // Returns a different double value from a provided array depending on route type.
    // The resizeWith array is ordered by [bus, commuter rail, everything else].
    fun modeSizeMultiplierExp(resizeWith: List<Number>) =
        Exp(
            "case",
            Exp("==", topRouteExp, MapStopRoute.BUS.name) to resizeWith[0],
            Exp("==", topRouteExp, MapStopRoute.COMMUTER.name) to resizeWith[1],
            resizeWith[2]
        )

    // MapBox requires the value at the base of every case in this expression to be a literal
    // length 2 array of Doubles, since that's the type that iconOffset expects. This means
    // that we can't use expressions for each Double, which is why this function is so convoluted.
    fun offsetAlertExp(closeZoom: Boolean, index: Int): Exp {
        val doubleRouteHeight = if (closeZoom) 13 else 8
        val tripleRouteHeight = if (closeZoom) 26 else 16
        return Exp(
            "step",
            Exp("length", routesExp),
            // At stops only serving one type of route, the height doesn't need to be offset at all
            offsetAlertPairExp(closeZoom = closeZoom, height = 0),
            // At stops serving two different types of route, position the first upwards and the
            // second downwards. The third value is unused, so is set to 0.
            2 to
                listOf(
                    offsetAlertPairExp(closeZoom = closeZoom, height = -doubleRouteHeight),
                    offsetAlertPairExp(closeZoom = closeZoom, height = doubleRouteHeight),
                    xyExp(0, 0),
                )[index],
            // At stops serving 3 routes, the first and third are moved up and down, and the center
            // one
            // remains at the same height.
            3 to
                listOf(
                    offsetAlertPairExp(closeZoom = closeZoom, height = -tripleRouteHeight),
                    offsetAlertPairExp(closeZoom = closeZoom, height = 0),
                    offsetAlertPairExp(closeZoom = closeZoom, height = tripleRouteHeight),
                )[index]
        )
    }

    // The provided height is determined by the position in the route type array,
    // this function determines the width to offset the alert icon depending on
    // the width of the type of icon that the stop is being displayed with.
    fun offsetAlertPairExp(closeZoom: Boolean, height: Int): Exp {
        val pillWidth = 26
        val railStopWidth = if (closeZoom) pillWidth else 12
        val busStopWidth = if (closeZoom) 18 else 12
        val terminalRailStopWidth = if (closeZoom) pillWidth else 20
        val terminalFerryWidth = if (closeZoom) pillWidth else 15
        val branchPillWidth = 35
        val branchTerminalWidth = if (closeZoom) branchPillWidth else 26
        val branchStopWidth = if (closeZoom) branchPillWidth else 13

        return Exp(
            "step",
            Exp("length", routesExp),
            Exp(
                "case",
                // Branching routes need additional width because their pills are extra wide
                // from the additional branch indicator
                branchedRouteExp to
                    Exp(
                        "case",
                        Exp("get", StopSourceGenerator.propIsTerminalKey) to
                            xyExp(branchTerminalWidth, height),
                        xyExp(branchStopWidth, height)
                    ),
                // Ferry terminals have a special larger icon at the wide zoom level
                Exp(
                    "all",
                    Exp("==", topRouteExp, MapStopRoute.FERRY.name),
                    Exp("get", StopSourceGenerator.propIsTerminalKey)
                ) to xyExp(terminalFerryWidth, height),
                // Buses have an extra small dot at wide zoom, and at close zoom, the height
                // is slightly repositioned to center the alert on the tombstone, ignoring the
                // small pole rectangle at the bottom
                Exp("==", topRouteExp, MapStopRoute.BUS.name) to
                    xyExp(busStopWidth, height - (if (closeZoom) 2 else 0)),
                // Rail terminals at wide zoom have a special pill icon rather than the usual dot
                Exp("get", StopSourceGenerator.propIsTerminalKey) to
                    xyExp(terminalRailStopWidth, height),
                // Regular rail stops, have a basic pill at close zoom and a dot at wide
                xyExp(railStopWidth, height)
            ),
            // If the stop is a transfer stop, all routes are displayed with a basic pill and dot
            2 to xyExp(railStopWidth, height)
        )
    }

    fun offsetTransferExp(closeZoom: Boolean, index: Int): Exp {
        val doubleRouteOffset = if (closeZoom) 13 else 8
        val tripleRouteOffset = if (closeZoom) 26 else 16
        return Exp(
            "step",
            Exp("length", routesExp),
            xyExp(0, 0),
            2 to xyExp(0, listOf(-doubleRouteOffset, doubleRouteOffset, 0)[index]),
            3 to xyExp(0, listOf(-tripleRouteOffset, 0, tripleRouteOffset)[index])
        )
    }

    fun offsetPinExp(closeZoom: Boolean): Exp {
        val singleRouteOffset = if (closeZoom) 38 else 33
        val doubleRouteOffset = if (closeZoom) 52 else 42
        val tripleRouteOffset = if (closeZoom) 65 else 50
        return Exp(
            "step",
            Exp("length", routesExp),
            Exp(
                "case",
                // Ferry terminals have a special larger icon at the wide zoom level
                Exp(
                    "all",
                    Exp("==", topRouteExp, MapStopRoute.FERRY.name),
                    Exp("get", StopSourceGenerator.propIsTerminalKey)
                ) to xyExp(0, -singleRouteOffset - (if (closeZoom) 0 else 2)),
                // Buses have an extra small dot at wide zoom, and at close zoom, the height
                // is slightly repositioned to center the alert on the tombstone, ignoring the
                // small pole rectangle at the bottom
                Exp("==", topRouteExp, MapStopRoute.BUS.name) to
                    xyExp(0, -singleRouteOffset - (if (closeZoom) 2 else 0)),
                // Rail terminals at wide zoom have a special pill icon rather than the usual dot
                Exp("get", StopSourceGenerator.propIsTerminalKey) to
                    xyExp(0, -singleRouteOffset - (if (closeZoom) 0 else 2)),
                // Regular rail stops, have a basic pill at close zoom and a dot at wide
                xyExp(0, -singleRouteOffset)
            ),
            2 to xyExp(0, -doubleRouteOffset),
            3 to xyExp(0, -tripleRouteOffset)
        )
    }

    // Similar to offsetAlertExp, the labels are set to different height and width offsets
    // based on the type of icon that they're next to. I'm not certain what units these values
    // are in though, they definitely aren't pixels, and the docs don't specify.
    val labelOffsetExp: Exp =
        Exp(
            "interpolate",
            Exp("exponential", 1.5),
            Exp("zoom"),
            MapDefaults.midZoomThreshold to
                Exp(
                    "step",
                    Exp("length", Exp("get", StopSourceGenerator.propMapRoutesKey)),
                    Exp(
                        "case",
                        branchedRouteExp to xyExp(1.15, 0.75),
                        Exp("get", StopSourceGenerator.propIsTerminalKey) to xyExp(1, 0.75),
                        xyExp(0.75, 0.5)
                    ),
                    2 to xyExp(0.5, 1.25),
                    3 to xyExp(0.5, 1.5)
                ),
            MapDefaults.closeZoomThreshold to
                Exp(
                    "step",
                    Exp("length", Exp("get", StopSourceGenerator.propMapRoutesKey)),
                    Exp("case", branchedRouteExp to xyExp(2.5, 1.5), xyExp(2, 1.5)),
                    2 to xyExp(2, 2),
                    3 to xyExp(2, 2.5)
                )
        )

    // The modeResize array must contain 3 entries for [BUS, COMMUTER, fallback]
    fun withMultipliers(base: Number, modeResize: List<Number> = listOf(1, 1, 1)): Exp {
        return Exp(
            "*",
            base,
            modeSizeMultiplierExp(resizeWith = modeResize),
            // TODO: We actually want to give the icon a halo rather than resize,
            // but that is only supported for SDFs, which can only be one color.
            // Alternates of stop icon SVGs with halo applied?
            Exp("case", selectedExp to 1.25, 1)
        )
    }

    // Mapbox only accepts iconOffset values if they're wrapped in this array literal expression
    fun xyExp(x: Number, y: Number): Exp {
        return Exp(
            "array",
            "number",
            2,
            Exp.bareValue(
                buildJsonArray {
                    add(x)
                    add(y)
                }
            )
        )
    }
}
